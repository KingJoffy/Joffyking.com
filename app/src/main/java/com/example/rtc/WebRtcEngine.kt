package com.example.rtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.coroutines.resume

class WebRtcEngine(context: Context) {
    private val appContext = context.applicationContext
    val eglBase: EglBase = EglBase.create()

    private val peerConnectionFactory: PeerConnectionFactory
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var activeCallPeerConnection: PeerConnection? = null
    private val viewerPeerConnections = mutableMapOf<String, PeerConnection>()
    private var viewerWatchPeerConnection: PeerConnection? = null
    private val voicePeerConnections = mutableMapOf<String, PeerConnection>()

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private val _cameraReady = MutableStateFlow(false)
    val cameraReady: StateFlow<Boolean> = _cameraReady.asStateFlow()

    private val _remoteVideoActive = MutableStateFlow(false)
    val remoteVideoActive: StateFlow<Boolean> = _remoteVideoActive.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _voiceActivePeerCount = MutableStateFlow(0)
    val voiceActivePeerCount: StateFlow<Int> = _voiceActivePeerCount.asStateFlow()

    private val _voiceSessionActive = MutableStateFlow(false)
    val voiceSessionActive: StateFlow<Boolean> = _voiceSessionActive.asStateFlow()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val audioDeviceModule = JavaAudioDeviceModule.builder(appContext).createAudioDeviceModule()
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun startLocalMedia(enableVideo: Boolean = true, enableAudio: Boolean = true) {
        if (localAudioTrack == null && enableAudio) {
            try {
                val audioConstraints = MediaConstraints()
                localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
                localAudioTrack = peerConnectionFactory.createAudioTrack("NEXUS_AUDIO_0", localAudioSource)
                localAudioTrack?.setEnabled(true)
            } catch (e: Exception) {
                Log.w(TAG, "Audio capture unavailable: ${e.message}")
            }
        }

        if (localVideoTrack == null && enableVideo) {
            try {
                val enumerator = Camera2Enumerator(appContext)
                val deviceNames = enumerator.deviceNames
                val selectedDevice = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                    ?: deviceNames.firstOrNull()

                if (selectedDevice != null) {
                    _isFrontCamera.value = enumerator.isFrontFacing(selectedDevice)
                    val capturer = enumerator.createCapturer(selectedDevice, null)
                    if (capturer != null) {
                        val texHelper = SurfaceTextureHelper.create("NexusCapture", eglBase.eglBaseContext)
                        val source = peerConnectionFactory.createVideoSource(capturer.isScreencast)
                        capturer.initialize(texHelper, appContext, source.capturerObserver)
                        capturer.startCapture(720, 1280, 30)

                        videoCapturer = capturer
                        surfaceTextureHelper = texHelper
                        localVideoSource = source
                        localVideoTrack = peerConnectionFactory.createVideoTrack("NEXUS_VIDEO_0", source)
                        localVideoTrack?.setEnabled(true)
                        localRenderer?.let { localVideoTrack?.addSink(it) }
                        _cameraReady.value = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Camera capture unavailable on this device: ${e.message}")
                _cameraReady.value = false
            }
        }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        if (localRenderer !== renderer) {
            localRenderer?.let { old -> localVideoTrack?.removeSink(old) }
            localRenderer = renderer
            localVideoTrack?.addSink(renderer)
        }
    }

    fun detachLocalRenderer(renderer: SurfaceViewRenderer) {
        localVideoTrack?.removeSink(renderer)
        if (localRenderer === renderer) {
            localRenderer = null
        }
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
    }

    fun detachRemoteRenderer(renderer: SurfaceViewRenderer) {
        if (remoteRenderer === renderer) {
            remoteRenderer = null
        }
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                _isFrontCamera.value = isFront
                localRenderer?.setMirror(isFront)
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.w(TAG, "Camera switch failed: $errorDescription")
            }
        })
    }

    fun setSpeakerphoneOn(enabled: Boolean) {
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Speakerphone toggle failed: ${e.message}")
        }
    }

    // --- 1-on-1 Call Session ---

    suspend fun createCallOffer(
        onIceCandidate: (String) -> Unit
    ): String? {
        closeCallSession()
        startLocalMedia(enableVideo = true, enableAudio = true)

        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = { track ->
                remoteRenderer?.let { track.addSink(it) }
                _remoteVideoActive.value = true
            }
        ) ?: return null

        activeCallPeerConnection = pc
        attachLocalTracksToPeerConnection(pc)
        return createOfferSdp(pc)
    }

    suspend fun acceptCallOfferAndCreateAnswer(
        offerSdp: String,
        onIceCandidate: (String) -> Unit
    ): String? {
        closeCallSession()
        startLocalMedia(enableVideo = true, enableAudio = true)

        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = { track ->
                remoteRenderer?.let { track.addSink(it) }
                _remoteVideoActive.value = true
            }
        ) ?: return null

        activeCallPeerConnection = pc
        attachLocalTracksToPeerConnection(pc)
        val setRemoteOk = setRemoteSdp(pc, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        if (!setRemoteOk) return null
        return createAnswerSdp(pc)
    }

    suspend fun applyCallAnswer(answerSdp: String) {
        val pc = activeCallPeerConnection ?: return
        if (pc.remoteDescription == null && answerSdp.isNotBlank()) {
            setRemoteSdp(pc, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
        }
    }

    fun addCallIceCandidate(serializedCandidate: String) {
        val pc = activeCallPeerConnection ?: return
        parseIceCandidate(serializedCandidate)?.let { pc.addIceCandidate(it) }
    }

    fun closeCallSession() {
        _remoteVideoActive.value = false
        activeCallPeerConnection?.close()
        activeCallPeerConnection?.dispose()
        activeCallPeerConnection = null
    }

    // --- Live Broadcast Multi-Viewer Signaling ---

    suspend fun hostAnswerViewerOffer(
        viewerId: String,
        offerSdp: String,
        onIceCandidate: (String) -> Unit
    ): String? {
        viewerPeerConnections[viewerId]?.let { existing ->
            existing.close()
            existing.dispose()
        }
        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = {}
        ) ?: return null
        viewerPeerConnections[viewerId] = pc
        attachLocalTracksToPeerConnection(pc)
        val ok = setRemoteSdp(pc, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        if (!ok) return null
        return createAnswerSdp(pc)
    }

    fun hostAddViewerIceCandidate(viewerId: String, serializedCandidate: String) {
        val pc = viewerPeerConnections[viewerId] ?: return
        parseIceCandidate(serializedCandidate)?.let { pc.addIceCandidate(it) }
    }

    suspend fun viewerCreateStreamOffer(onIceCandidate: (String) -> Unit): String? {
        closeViewerWatchSession()
        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = { track ->
                remoteRenderer?.let { track.addSink(it) }
                _remoteVideoActive.value = true
            }
        ) ?: return null
        viewerWatchPeerConnection = pc
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        return createOfferSdp(pc, constraints)
    }

    suspend fun viewerApplyStreamAnswer(answerSdp: String) {
        val pc = viewerWatchPeerConnection ?: return
        if (pc.remoteDescription == null && answerSdp.isNotBlank()) {
            setRemoteSdp(pc, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
        }
    }

    fun viewerAddHostIceCandidate(serializedCandidate: String) {
        val pc = viewerWatchPeerConnection ?: return
        parseIceCandidate(serializedCandidate)?.let { pc.addIceCandidate(it) }
    }

    fun closeViewerWatchSession() {
        _remoteVideoActive.value = false
        viewerWatchPeerConnection?.close()
        viewerWatchPeerConnection?.dispose()
        viewerWatchPeerConnection = null
    }

    fun closeAllHostViewerConnections() {
        viewerPeerConnections.values.forEach { pc ->
            pc.close()
            pc.dispose()
        }
        viewerPeerConnections.clear()
    }

    // --- Audio-Only Voice Commentary Mesh ---

    fun startAudioOnlyVoice(enableMic: Boolean = true, speakerphone: Boolean = true): Boolean {
        if (enableMic) {
            startLocalMedia(enableVideo = false, enableAudio = true)
            localAudioTrack?.setEnabled(true)
        }
        setSpeakerphoneOn(speakerphone)
        _voiceSessionActive.value = true
        return !enableMic || localAudioTrack != null
    }

    fun setVoiceMicMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    suspend fun createVoiceOffer(
        peerId: String,
        onIceCandidate: (String) -> Unit
    ): String? {
        closeVoicePeer(peerId)
        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = {}
        ) ?: return null
        voicePeerConnections[peerId] = pc
        _voiceActivePeerCount.value = voicePeerConnections.size
        attachLocalAudioOnlyTrack(pc)
        val audioOnlyConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        return createOfferSdp(pc, audioOnlyConstraints)
    }

    suspend fun acceptVoiceOfferAndCreateAnswer(
        peerId: String,
        offerSdp: String,
        onIceCandidate: (String) -> Unit
    ): String? {
        closeVoicePeer(peerId)
        val pc = createPeerConnection(
            onIceCandidate = onIceCandidate,
            onRemoteVideoTrack = {}
        ) ?: return null
        voicePeerConnections[peerId] = pc
        _voiceActivePeerCount.value = voicePeerConnections.size
        attachLocalAudioOnlyTrack(pc)
        val ok = setRemoteSdp(pc, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        if (!ok) return null
        return createAnswerSdp(pc)
    }

    suspend fun applyVoiceAnswer(peerId: String, answerSdp: String) {
        val pc = voicePeerConnections[peerId] ?: return
        if (pc.remoteDescription == null && answerSdp.isNotBlank()) {
            setRemoteSdp(pc, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
        }
    }

    fun addVoiceIceCandidate(peerId: String, serializedCandidate: String) {
        val pc = voicePeerConnections[peerId] ?: return
        parseIceCandidate(serializedCandidate)?.let { pc.addIceCandidate(it) }
    }

    fun hasVoicePeer(peerId: String): Boolean = voicePeerConnections.containsKey(peerId)

    fun closeVoicePeer(peerId: String) {
        voicePeerConnections.remove(peerId)?.let { pc ->
            pc.close()
            pc.dispose()
        }
        _voiceActivePeerCount.value = voicePeerConnections.size
    }

    fun closeAllVoicePeers(releaseMicIfViewerOnly: Boolean = false) {
        voicePeerConnections.values.forEach { pc ->
            pc.close()
            pc.dispose()
        }
        voicePeerConnections.clear()
        _voiceActivePeerCount.value = 0
        _voiceSessionActive.value = false
        if (releaseMicIfViewerOnly &&
            localVideoTrack == null &&
            activeCallPeerConnection == null &&
            viewerPeerConnections.isEmpty()
        ) {
            localAudioTrack?.dispose()
            localAudioTrack = null
            localAudioSource?.dispose()
            localAudioSource = null
        }
    }

    fun stopLocalMedia() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        videoCapturer?.dispose()
        videoCapturer = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        localVideoSource?.dispose()
        localVideoSource = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        localAudioSource?.dispose()
        localAudioSource = null
        _cameraReady.value = false
    }

    fun release() {
        closeCallSession()
        closeViewerWatchSession()
        closeAllHostViewerConnections()
        closeAllVoicePeers(releaseMicIfViewerOnly = false)
        stopLocalMedia()
        peerConnectionFactory.dispose()
        eglBase.release()
    }

    private fun attachLocalTracksToPeerConnection(pc: PeerConnection) {
        val streamIds = listOf("NEXUS_STREAM")
        localAudioTrack?.let { pc.addTrack(it, streamIds) }
        localVideoTrack?.let { pc.addTrack(it, streamIds) }
    }

    private fun attachLocalAudioOnlyTrack(pc: PeerConnection) {
        val streamIds = listOf("NEXUS_VOICE")
        localAudioTrack?.let { pc.addTrack(it, streamIds) }
    }

    private fun createPeerConnection(
        onIceCandidate: (String) -> Unit,
        onRemoteVideoTrack: (VideoTrack) -> Unit
    ): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        return peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        val serialized = "${it.sdpMid}|${it.sdpMLineIndex}|${it.sdp}"
                        onIceCandidate(serialized)
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {
                    stream?.videoTracks?.firstOrNull()?.let(onRemoteVideoTrack)
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        onRemoteVideoTrack(track)
                    }
                }
            }
        )
    }

    private suspend fun createOfferSdp(
        pc: PeerConnection,
        constraints: MediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    ): String? = suspendCancellableCoroutine { cont ->
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) {
                    if (cont.isActive) cont.resume(null)
                    return
                }
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        if (cont.isActive) cont.resume(sdp.description)
                    }
                    override fun onCreateFailure(err: String?) {}
                    override fun onSetFailure(err: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                if (cont.isActive) cont.resume(null)
            }
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    private suspend fun createAnswerSdp(pc: PeerConnection): String? =
        suspendCancellableCoroutine { cont ->
            val constraints = MediaConstraints()
            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) {
                        if (cont.isActive) cont.resume(null)
                        return
                    }
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            if (cont.isActive) cont.resume(sdp.description)
                        }
                        override fun onCreateFailure(err: String?) {}
                        override fun onSetFailure(err: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }, sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(err: String?) {
                    if (cont.isActive) cont.resume(null)
                }
                override fun onSetFailure(err: String?) {}
            }, constraints)
        }

    private suspend fun setRemoteSdp(pc: PeerConnection, sdp: SessionDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            pc.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCreateFailure(err: String?) {}
                override fun onSetFailure(err: String?) {
                    if (cont.isActive) cont.resume(false)
                }
            }, sdp)
        }

    private fun parseIceCandidate(serialized: String): IceCandidate? {
        val parts = serialized.split("|", limit = 3)
        if (parts.size != 3) return null
        val index = parts[1].toIntOrNull() ?: return null
        return IceCandidate(parts[0], index, parts[2])
    }

    companion object {
        private const val TAG = "WebRtcEngine"
    }
}
