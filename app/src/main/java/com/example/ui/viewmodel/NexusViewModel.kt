package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.NexusRepository
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.GamerProfile
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LiveStream
import com.example.data.model.MatchRecord
import com.example.data.model.SportHighlight
import com.example.data.model.StreamMessage
import com.example.data.model.UiState
import com.example.data.model.VideoCall
import com.example.data.model.VoiceParticipant
import com.example.rtc.WebRtcEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchFilterTab { ALL, GAMES, STREAMS, GAMERS }

class NexusViewModel(
    private val repository: NexusRepository,
    val webRtcEngine: WebRtcEngine,
    val currentUserId: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedGameId = MutableStateFlow<String?>(null)
    val selectedGameId: StateFlow<String?> = _selectedGameId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTab = MutableStateFlow(SearchFilterTab.ALL)
    val searchTab: StateFlow<SearchFilterTab> = _searchTab.asStateFlow()

    // Active broadcast state
    private val _activeBroadcastId = MutableStateFlow<String?>(null)
    val activeBroadcastId: StateFlow<String?> = _activeBroadcastId.asStateFlow()

    private val _micEnabled = MutableStateFlow(true)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(true)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    private val _speakerEnabled = MutableStateFlow(true)
    val speakerEnabled: StateFlow<Boolean> = _speakerEnabled.asStateFlow()

    // Active watch stream state
    private val _watchingStreamId = MutableStateFlow<String?>(null)
    val watchingStreamId: StateFlow<String?> = _watchingStreamId.asStateFlow()

    private val _watchingMessages = MutableStateFlow<List<StreamMessage>>(emptyList())
    val watchingMessages: StateFlow<List<StreamMessage>> = _watchingMessages.asStateFlow()

    // Active video call state
    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

    // Real-time audio-only voice commentary state
    private val _voiceParticipants = MutableStateFlow<List<VoiceParticipant>>(emptyList())
    val voiceParticipants: StateFlow<List<VoiceParticipant>> = _voiceParticipants.asStateFlow()

    private val _inVoiceChannel = MutableStateFlow(false)
    val inVoiceChannel: StateFlow<Boolean> = _inVoiceChannel.asStateFlow()

    private val _voiceMicMuted = MutableStateFlow(false)
    val voiceMicMuted: StateFlow<Boolean> = _voiceMicMuted.asStateFlow()

    private val _voiceFocusTag = MutableStateFlow("Tactics")
    val voiceFocusTag: StateFlow<String> = _voiceFocusTag.asStateFlow()

    private val _voiceHostMutedMe = MutableStateFlow(false)
    val voiceHostMutedMe: StateFlow<Boolean> = _voiceHostMutedMe.asStateFlow()

    private var broadcastSignalsJob: Job? = null
    private var broadcastMessagesJob: Job? = null
    private var watchMessagesJob: Job? = null
    private var watchSignalJob: Job? = null
    private var activeCallJob: Job? = null
    private var voiceParticipantsJob: Job? = null
    private var voiceSignalsJob: Job? = null
    private var currentVoiceStreamId: String? = null

    private val answeredViewers = mutableSetOf<String>()
    private val appliedHostCandidates = mutableSetOf<String>()
    private val appliedViewerCandidates = mutableMapOf<String, MutableSet<String>>()
    private val appliedCallCandidates = mutableSetOf<String>()
    private var appliedCallAnswer = false

    private val initiatedVoicePairs = mutableSetOf<String>()
    private val answeredVoicePairs = mutableSetOf<String>()
    private val appliedVoiceAnswers = mutableSetOf<String>()
    private val appliedVoiceCandidates = mutableMapOf<String, MutableSet<String>>()

    val myProfileState: StateFlow<UiState<GamerProfile?>> =
        repository.observeMyProfile(currentUserId)
            .toUiState("Failed to load profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val liveStreamsState: StateFlow<UiState<List<LiveStream>>> =
        repository.observeLiveStreams()
            .toUiState("Failed to load live streams")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val discoverableGamersState: StateFlow<UiState<List<GamerProfile>>> =
        repository.observeDiscoverableProfiles()
            .toUiState("Failed to load gamers")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val friendshipsState: StateFlow<UiState<List<Friendship>>> =
        repository.observeFriendships(currentUserId)
            .toUiState("Failed to load friends")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard()
            .toUiState("Failed to load leaderboard")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val matchHistoryState: StateFlow<UiState<List<MatchRecord>>> =
        repository.observeMyMatchHistory(currentUserId)
            .toUiState("Failed to load match history")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val sportHighlightsState: StateFlow<UiState<List<SportHighlight>>> =
        repository.observeSportHighlights()
            .toUiState("Failed to load sports highlights")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val myCallsState: StateFlow<List<VideoCall>> =
        repository.observeMyCalls(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing calls", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val incomingCall: StateFlow<VideoCall?> = myCallsState
        .map { calls ->
            calls.firstOrNull { it.calleeId == currentUserId && it.status == "ringing" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val activeCall: StateFlow<VideoCall?> = combine(myCallsState, _activeCallId) { calls, callId ->
        if (callId == null) null else calls.firstOrNull { it.id == callId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureDefaultProfile(user)
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun selectGameCategory(gameId: String?) {
        _selectedGameId.value = gameId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSearchTab(tab: SearchFilterTab) {
        _searchTab.value = tab
    }

    fun updateProfile(
        username: String,
        bio: String,
        avatarData: String,
        favoriteGame: String,
        discoverable: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.updateProfile(username, bio, avatarData, favoriteGame, discoverable)
            _statusBanner.value = if (result.isSuccess) {
                "Profile saved!"
            } else {
                result.exceptionOrNull()?.message ?: "Could not save profile."
            }
        }
    }

    fun recordMatchResult(
        category: GameCategory,
        outcome: String,
        opponentName: String,
        scoreSummary: String,
        myScore: Long? = null,
        opponentScore: Long? = null,
        matchMode: String = "",
        wagerPoints: Long = 0L,
        payoutPoints: Long = 0L,
        notes: String = ""
    ) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: GamerProfile(
            userId = currentUserId,
            username = "Gamer_${currentUserId.take(5)}"
        )
        viewModelScope.launch {
            val result = repository.recordMatchResult(
                profile = myProfile,
                category = category,
                outcome = outcome,
                opponentName = opponentName,
                scoreSummary = scoreSummary,
                myScore = myScore,
                opponentScore = opponentScore,
                matchMode = matchMode,
                wagerPoints = wagerPoints,
                payoutPoints = payoutPoints,
                notes = notes
            )
            val netPts = payoutPoints - wagerPoints
            val ptsSuffix = if (wagerPoints > 0L || payoutPoints > 0L) {
                val sign = if (netPts >= 0L) "+" else ""
                " ($sign$netPts PTS)"
            } else {
                ""
            }
            _statusBanner.value = if (result.isSuccess) {
                when (outcome.lowercase()) {
                    "win" -> "Victory logged! +25 MMR$ptsSuffix"
                    "loss" -> "Match logged (-15 MMR)$ptsSuffix"
                    else -> "Draw logged (0 MMR)$ptsSuffix"
                }
            } else {
                result.exceptionOrNull()?.message ?: "Could not record match."
            }
        }
    }

    fun deleteMatchRecord(matchId: String) {
        viewModelScope.launch {
            val result = repository.deleteMatchRecord(matchId)
            _statusBanner.value = if (result.isSuccess) {
                "Match entry removed from history."
            } else {
                result.exceptionOrNull()?.message ?: "Could not remove match entry."
            }
        }
    }

    // --- Sports Highlights ---

    fun publishSportHighlight(
        category: GameCategory,
        sportLeague: String,
        title: String,
        playType: String,
        statLine: String,
        quarterOrTime: String,
        description: String
    ) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: GamerProfile(
            userId = currentUserId,
            username = "Gamer_${currentUserId.take(5)}"
        )
        viewModelScope.launch {
            val result = repository.publishSportHighlight(
                profile = myProfile,
                category = category,
                sportLeague = sportLeague,
                title = title,
                playType = playType,
                statLine = statLine,
                quarterOrTime = quarterOrTime,
                description = description
            )
            _statusBanner.value = if (result.isSuccess) {
                "Highlight posted to the Sports Arena!"
            } else {
                result.exceptionOrNull()?.message ?: "Could not post highlight."
            }
        }
    }

    fun toggleHighlightHype(highlight: SportHighlight) {
        viewModelScope.launch {
            val result = repository.toggleHighlightHype(highlight)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.message ?: "Could not update hype."
            }
        }
    }

    fun deleteSportHighlight(highlightId: String) {
        viewModelScope.launch {
            val result = repository.deleteSportHighlight(highlightId)
            _statusBanner.value = if (result.isSuccess) {
                "Highlight removed."
            } else {
                result.exceptionOrNull()?.message ?: "Could not delete highlight."
            }
        }
    }

    // --- Friend System ---

    fun sendFriendRequest(targetProfile: GamerProfile) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.sendFriendRequest(myProfile, targetProfile)
            _statusBanner.value = if (result.isSuccess) {
                "Friend request sent to ${targetProfile.username}!"
            } else {
                result.exceptionOrNull()?.message ?: "Could not send friend request."
            }
        }
    }

    fun respondToFriendRequest(friendshipId: String, accept: Boolean) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.respondToFriendRequest(friendshipId, accept, myProfile)
            _statusBanner.value = if (result.isSuccess) {
                if (accept) "Friend request accepted!" else "Friend request declined."
            } else {
                result.exceptionOrNull()?.message ?: "Action failed."
            }
        }
    }

    fun removeFriendship(friendshipId: String) {
        viewModelScope.launch {
            val result = repository.removeFriendship(friendshipId)
            _statusBanner.value = if (result.isSuccess) {
                "Updated friends list."
            } else {
                result.exceptionOrNull()?.message ?: "Could not remove friend."
            }
        }
    }

    // --- Live Broadcasting ---

    fun startBroadcastPreview() {
        webRtcEngine.startLocalMedia(enableVideo = true, enableAudio = true)
        webRtcEngine.setMicEnabled(_micEnabled.value)
        webRtcEngine.setCameraEnabled(_cameraEnabled.value)
    }

    fun goLive(
        title: String,
        category: GameCategory,
        region: String,
        voiceChannelEnabled: Boolean = true,
        voiceTopic: String = "Tactics & Feedback"
    ) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: GamerProfile(
            userId = currentUserId,
            username = "Streamer"
        )
        viewModelScope.launch {
            webRtcEngine.startLocalMedia(enableVideo = true, enableAudio = true)
            val result = repository.startLiveStream(
                hostProfile = myProfile,
                title = title,
                category = category,
                region = region,
                micEnabled = _micEnabled.value,
                cameraEnabled = _cameraEnabled.value,
                voiceChannelEnabled = voiceChannelEnabled,
                voiceTopic = voiceTopic
            )
            result.onSuccess { streamId ->
                _activeBroadcastId.value = streamId
                _statusBanner.value = if (voiceChannelEnabled) {
                    "You are LIVE with Voice Commentary open!"
                } else {
                    "You are LIVE in ${category.title}!"
                }
                listenToBroadcastSignalsAndChat(streamId)
                startObservingVoiceParticipants(streamId)
                if (voiceChannelEnabled) {
                    joinVoiceCommentaryInternal(
                        streamId = streamId,
                        hostId = currentUserId,
                        withMic = _micEnabled.value,
                        focusTag = "Co-Cast",
                        announce = false
                    )
                }
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Failed to go live."
            }
        }
    }

    private fun listenToBroadcastSignalsAndChat(streamId: String) {
        answeredViewers.clear()
        appliedViewerCandidates.clear()

        broadcastMessagesJob?.cancel()
        broadcastMessagesJob = viewModelScope.launch {
            repository.observeStreamMessages(streamId)
                .catch { e -> Log.w(TAG, "Broadcast chat error", e) }
                .collect { _watchingMessages.value = it }
        }

        broadcastSignalsJob?.cancel()
        broadcastSignalsJob = viewModelScope.launch {
            repository.observeHostViewerSignals(streamId, currentUserId)
                .catch { e -> Log.w(TAG, "Broadcast signaling error", e) }
                .collect { signals ->
                    signals.forEach { signal ->
                        if (signal.offerSdp.isNotBlank() && !answeredViewers.contains(signal.viewerId)) {
                            answeredViewers.add(signal.viewerId)
                            launch {
                                val answer = webRtcEngine.hostAnswerViewerOffer(
                                    viewerId = signal.viewerId,
                                    offerSdp = signal.offerSdp,
                                    onIceCandidate = { candidate ->
                                        launch {
                                            repository.appendViewerSignalCandidate(
                                                streamId = streamId,
                                                viewerId = signal.viewerId,
                                                isHost = true,
                                                candidate = candidate
                                            )
                                        }
                                    }
                                )
                                if (answer != null) {
                                    repository.answerViewerOffer(streamId, signal.viewerId, answer)
                                }
                            }
                        }
                        val seen = appliedViewerCandidates.getOrPut(signal.viewerId) { mutableSetOf() }
                        signal.viewerCandidates.forEach { cand ->
                            if (seen.add(cand)) {
                                webRtcEngine.hostAddViewerIceCandidate(signal.viewerId, cand)
                            }
                        }
                    }
                }
        }
    }

    fun endLiveBroadcast() {
        val streamId = _activeBroadcastId.value ?: return
        leaveVoiceCommentary(releaseMicIfViewer = false)
        voiceParticipantsJob?.cancel()
        _voiceParticipants.value = emptyList()
        broadcastSignalsJob?.cancel()
        broadcastMessagesJob?.cancel()
        _activeBroadcastId.value = null
        _watchingMessages.value = emptyList()
        webRtcEngine.closeAllHostViewerConnections()
        viewModelScope.launch {
            repository.endLiveStream(streamId)
            _statusBanner.value = "Broadcast ended."
        }
    }

    fun toggleMic() {
        val next = !_micEnabled.value
        _micEnabled.value = next
        webRtcEngine.setMicEnabled(next)
        _activeBroadcastId.value?.let { streamId ->
            viewModelScope.launch {
                repository.updateStreamControls(streamId, next, _cameraEnabled.value)
                if (_inVoiceChannel.value && currentVoiceStreamId == streamId) {
                    _voiceMicMuted.value = !next
                    repository.updateMyVoiceParticipant(streamId, _voiceFocusTag.value, !next)
                }
            }
        }
    }

    fun toggleCamera() {
        val next = !_cameraEnabled.value
        _cameraEnabled.value = next
        webRtcEngine.setCameraEnabled(next)
        _activeBroadcastId.value?.let { streamId ->
            viewModelScope.launch {
                repository.updateStreamControls(streamId, _micEnabled.value, next)
            }
        }
    }

    fun switchCamera() {
        webRtcEngine.switchCamera()
    }

    fun toggleSpeakerphone() {
        val next = !_speakerEnabled.value
        _speakerEnabled.value = next
        webRtcEngine.setSpeakerphoneOn(next)
    }

    // --- Real-Time Audio-Only Voice Commentary Channel ---

    private fun startObservingVoiceParticipants(streamId: String) {
        voiceParticipantsJob?.cancel()
        voiceParticipantsJob = viewModelScope.launch {
            repository.observeVoiceParticipants(streamId)
                .catch { e -> Log.w(TAG, "Voice participants error", e) }
                .collect { participants ->
                    _voiceParticipants.value = participants
                    val myEntry = participants.firstOrNull { it.userId == currentUserId }
                    if (_inVoiceChannel.value && currentVoiceStreamId == streamId) {
                        if (myEntry == null) {
                            // Removed by host or left externally
                            stopLocalVoiceMesh(releaseMicIfViewer = _activeBroadcastId.value == null)
                            _statusBanner.value = "Voice commentary session disconnected."
                        } else if (myEntry.hostMuted && !_voiceHostMutedMe.value) {
                            _voiceHostMutedMe.value = true
                            _voiceMicMuted.value = true
                            webRtcEngine.setVoiceMicMuted(true)
                            _statusBanner.value = "Host muted your commentary mic."
                        } else if (!myEntry.hostMuted && _voiceHostMutedMe.value) {
                            _voiceHostMutedMe.value = false
                            _voiceMicMuted.value = myEntry.muted
                            webRtcEngine.setVoiceMicMuted(myEntry.muted)
                            _statusBanner.value = "Host unmuted your commentary mic."
                        }
                    }
                }
        }
    }

    fun updateVoiceRoomSettings(streamId: String, enabled: Boolean, topic: String) {
        viewModelScope.launch {
            val res = repository.updateStreamVoiceSettings(streamId, enabled, topic)
            if (res.isSuccess) {
                if (enabled && !_inVoiceChannel.value) {
                    joinVoiceCommentaryInternal(
                        streamId = streamId,
                        hostId = currentUserId,
                        withMic = _micEnabled.value,
                        focusTag = "Co-Cast",
                        announce = false
                    )
                } else if (!enabled && _inVoiceChannel.value) {
                    leaveVoiceCommentary(releaseMicIfViewer = false)
                }
                _statusBanner.value = if (enabled) {
                    "Voice commentary opened: $topic"
                } else {
                    "Voice commentary paused for viewers."
                }
            } else {
                _statusBanner.value = res.exceptionOrNull()?.message ?: "Could not update voice channel."
            }
        }
    }

    fun joinVoiceCommentary(
        stream: LiveStream,
        withMicPermission: Boolean,
        focusTag: String = "Tactics"
    ) {
        if (!stream.voiceChannelEnabled && stream.hostId != currentUserId) {
            _statusBanner.value = "The streamer has paused voice commentary right now."
            return
        }
        viewModelScope.launch {
            joinVoiceCommentaryInternal(
                streamId = stream.id,
                hostId = stream.hostId,
                withMic = withMicPermission,
                focusTag = focusTag,
                announce = true
            )
        }
    }

    private suspend fun joinVoiceCommentaryInternal(
        streamId: String,
        hostId: String,
        withMic: Boolean,
        focusTag: String,
        announce: Boolean
    ) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: GamerProfile(
            userId = currentUserId,
            username = "Gamer_${currentUserId.take(5)}"
        )
        initiatedVoicePairs.clear()
        answeredVoicePairs.clear()
        appliedVoiceAnswers.clear()
        appliedVoiceCandidates.clear()

        webRtcEngine.startAudioOnlyVoice(enableMic = withMic, speakerphone = _speakerEnabled.value)
        val initialMuted = !withMic
        webRtcEngine.setVoiceMicMuted(initialMuted)

        _voiceFocusTag.value = focusTag
        _voiceMicMuted.value = initialMuted
        _voiceHostMutedMe.value = false
        currentVoiceStreamId = streamId
        _inVoiceChannel.value = true

        val res = repository.joinVoiceChannel(
            streamId = streamId,
            hostId = hostId,
            profile = myProfile,
            focusTag = focusTag,
            muted = initialMuted
        )
        if (res.isSuccess) {
            observeVoiceMeshSignaling(streamId)
            if (announce) {
                _statusBanner.value = if (withMic) {
                    "Joined audio commentary ($focusTag)"
                } else {
                    "Joined voice room in listen-only mode"
                }
            }
        } else {
            stopLocalVoiceMesh(releaseMicIfViewer = _activeBroadcastId.value == null)
            _statusBanner.value = res.exceptionOrNull()?.message ?: "Could not join voice commentary."
        }
    }

    private fun observeVoiceMeshSignaling(streamId: String) {
        voiceSignalsJob?.cancel()
        voiceSignalsJob = viewModelScope.launch {
            combine(
                _voiceParticipants,
                repository.observeMyVoiceSignals(streamId, currentUserId)
                    .catch { e ->
                        Log.w(TAG, "Voice signaling error", e)
                        emit(emptyList())
                    }
            ) { participants, signals ->
                participants to signals
            }.collect { (participants, signals) ->
                if (!_inVoiceChannel.value || currentVoiceStreamId != streamId) return@collect

                val activeRemoteIds = participants
                    .map { it.userId }
                    .filter { it.isNotBlank() && it != currentUserId }
                    .toSet()

                // Close WebRTC audio links for peers who left the commentary channel
                val disconnectedUids = initiatedVoicePairs.filter { it !in activeRemoteIds }
                disconnectedUids.forEach { goneUid ->
                    webRtcEngine.closeVoicePeer(goneUid)
                    initiatedVoicePairs.remove(goneUid)
                    val sigId = com.example.data.model.VoiceCommentaryCatalog
                        .deterministicPairSignalId(currentUserId, goneUid)
                    answeredVoicePairs.remove(sigId)
                    appliedVoiceAnswers.remove(sigId)
                    appliedVoiceCandidates.remove(sigId)
                }
                val activeSignalIds = activeRemoteIds.map { remoteUid ->
                    com.example.data.model.VoiceCommentaryCatalog.deterministicPairSignalId(currentUserId, remoteUid)
                }.toSet()
                val staleSignalIds = answeredVoicePairs.filter { it !in activeSignalIds }
                staleSignalIds.forEach { sigId ->
                    answeredVoicePairs.remove(sigId)
                    appliedVoiceAnswers.remove(sigId)
                    appliedVoiceCandidates.remove(sigId)
                }

                // Initiate audio-only offers deterministically for any active peer where currentUserId < remoteUid
                activeRemoteIds.forEach { remoteUid ->
                    if (currentUserId < remoteUid && initiatedVoicePairs.add(remoteUid)) {
                        val signalId = com.example.data.model.VoiceCommentaryCatalog
                            .deterministicPairSignalId(currentUserId, remoteUid)
                        launch {
                            val offerSdp = webRtcEngine.createVoiceOffer(remoteUid) { cand ->
                                launch {
                                    repository.appendVoiceIceCandidate(
                                        streamId = streamId,
                                        signalId = signalId,
                                        isInitiator = true,
                                        candidate = cand
                                    )
                                }
                            }
                            if (offerSdp != null) {
                                repository.upsertVoiceOffer(streamId, remoteUid, offerSdp)
                            }
                        }
                    }
                }

                // Process incoming SDP offers, answers, and ICE candidates
                signals.forEach { signal ->
                    val remoteUid = if (signal.initiatorId == currentUserId) signal.responderId else signal.initiatorId
                    if (remoteUid !in activeRemoteIds) {
                        webRtcEngine.closeVoicePeer(remoteUid)
                        return@forEach
                    }

                    if (signal.responderId == currentUserId &&
                        signal.offerSdp.isNotBlank() &&
                        answeredVoicePairs.add(signal.id)
                    ) {
                        launch {
                            val answerSdp = webRtcEngine.acceptVoiceOfferAndCreateAnswer(
                                peerId = remoteUid,
                                offerSdp = signal.offerSdp,
                                onIceCandidate = { cand ->
                                    launch {
                                        repository.appendVoiceIceCandidate(
                                            streamId = streamId,
                                            signalId = signal.id,
                                            isInitiator = false,
                                            candidate = cand
                                        )
                                    }
                                }
                            )
                            if (answerSdp != null) {
                                repository.answerVoiceOffer(streamId, signal.id, answerSdp)
                            }
                        }
                    }

                    if (signal.initiatorId == currentUserId &&
                        signal.answerSdp.isNotBlank() &&
                        appliedVoiceAnswers.add(signal.id)
                    ) {
                        launch {
                            webRtcEngine.applyVoiceAnswer(remoteUid, signal.answerSdp)
                        }
                    }

                    val remoteCands = if (signal.initiatorId == currentUserId) {
                        signal.responderCandidates
                    } else {
                        signal.initiatorCandidates
                    }
                    val seen = appliedVoiceCandidates.getOrPut(signal.id) { mutableSetOf() }
                    remoteCands.forEach { cand ->
                        if (seen.add(cand)) {
                            webRtcEngine.addVoiceIceCandidate(remoteUid, cand)
                        }
                    }
                }
            }
        }
    }

    fun toggleVoiceMic(hasAudioPermission: Boolean = true) {
        val streamId = currentVoiceStreamId ?: return
        if (_voiceHostMutedMe.value) {
            _statusBanner.value = "Stream host muted your microphone."
            return
        }
        val nextMuted = !_voiceMicMuted.value
        if (!nextMuted && !hasAudioPermission) {
            _statusBanner.value = "Grant microphone access to unmute in voice commentary."
            return
        }
        if (!nextMuted) {
            webRtcEngine.startAudioOnlyVoice(enableMic = true, speakerphone = _speakerEnabled.value)
        }
        _voiceMicMuted.value = nextMuted
        webRtcEngine.setVoiceMicMuted(nextMuted)
        viewModelScope.launch {
            repository.updateMyVoiceParticipant(streamId, _voiceFocusTag.value, nextMuted)
        }
    }

    fun setVoiceFocusTag(tag: String) {
        _voiceFocusTag.value = tag
        val streamId = currentVoiceStreamId
        if (_inVoiceChannel.value && streamId != null) {
            viewModelScope.launch {
                repository.updateMyVoiceParticipant(streamId, tag, _voiceMicMuted.value)
            }
        }
    }

    fun hostMuteVoiceParticipant(streamId: String, targetUserId: String, mute: Boolean) {
        viewModelScope.launch {
            val res = repository.hostModerateVoiceParticipant(streamId, targetUserId, mute)
            if (res.isFailure) {
                _statusBanner.value = res.exceptionOrNull()?.message ?: "Could not update speaker mic."
            }
        }
    }

    fun hostRemoveVoiceParticipant(streamId: String, targetUserId: String) {
        viewModelScope.launch {
            webRtcEngine.closeVoicePeer(targetUserId)
            val res = repository.leaveVoiceChannel(streamId, targetUserId)
            if (res.isSuccess) {
                val signalId = com.example.data.model.VoiceCommentaryCatalog
                    .deterministicPairSignalId(currentUserId, targetUserId)
                repository.deleteVoiceSignal(streamId, signalId)
                _statusBanner.value = "Removed speaker from voice commentary."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.message ?: "Could not remove speaker."
            }
        }
    }

    fun leaveVoiceCommentary(releaseMicIfViewer: Boolean = true) {
        val streamId = currentVoiceStreamId
        val remotePeerIds = _voiceParticipants.value
            .map { it.userId }
            .filter { it.isNotBlank() && it != currentUserId }
        stopLocalVoiceMesh(releaseMicIfViewer)
        if (streamId != null) {
            viewModelScope.launch {
                repository.leaveVoiceChannel(streamId, currentUserId)
                remotePeerIds.forEach { remoteId ->
                    val sigId = com.example.data.model.VoiceCommentaryCatalog
                        .deterministicPairSignalId(currentUserId, remoteId)
                    repository.deleteVoiceSignal(streamId, sigId)
                }
            }
        }
    }

    private fun stopLocalVoiceMesh(releaseMicIfViewer: Boolean) {
        voiceSignalsJob?.cancel()
        currentVoiceStreamId = null
        _inVoiceChannel.value = false
        _voiceMicMuted.value = false
        _voiceHostMutedMe.value = false
        initiatedVoicePairs.clear()
        answeredVoicePairs.clear()
        appliedVoiceAnswers.clear()
        appliedVoiceCandidates.clear()
        webRtcEngine.closeAllVoicePeers(releaseMicIfViewerOnly = releaseMicIfViewer)
    }

    // --- Watching a Live Stream ---

    fun joinStream(stream: LiveStream) {
        _watchingStreamId.value = stream.id
        appliedHostCandidates.clear()

        watchMessagesJob?.cancel()
        watchMessagesJob = viewModelScope.launch {
            repository.observeStreamMessages(stream.id)
                .catch { e -> Log.w(TAG, "Watch chat error", e) }
                .collect { _watchingMessages.value = it }
        }

        startObservingVoiceParticipants(stream.id)

        if (stream.hostId != currentUserId) {
            viewModelScope.launch {
                repository.adjustStreamViewerCount(stream.id, stream.viewerCount + 1L)
                val offer = webRtcEngine.viewerCreateStreamOffer { candidate ->
                    launch {
                        repository.appendViewerSignalCandidate(
                            streamId = stream.id,
                            viewerId = currentUserId,
                            isHost = false,
                            candidate = candidate
                        )
                    }
                }
                if (offer != null) {
                    repository.createOrUpdateViewerOffer(stream.id, stream.hostId, offer)
                }
            }
            watchSignalJob?.cancel()
            watchSignalJob = viewModelScope.launch {
                repository.observeViewerSignal(stream.id, currentUserId)
                    .catch { e -> Log.w(TAG, "Viewer signal error", e) }
                    .collect { signal ->
                        if (signal != null) {
                            if (signal.answerSdp.isNotBlank()) {
                                webRtcEngine.viewerApplyStreamAnswer(signal.answerSdp)
                            }
                            signal.hostCandidates.forEach { cand ->
                                if (appliedHostCandidates.add(cand)) {
                                    webRtcEngine.viewerAddHostIceCandidate(cand)
                                }
                            }
                        }
                    }
            }
        }
    }

    fun leaveStream(stream: LiveStream?) {
        val currentId = _watchingStreamId.value
        leaveVoiceCommentary(releaseMicIfViewer = _activeBroadcastId.value == null)
        voiceParticipantsJob?.cancel()
        _voiceParticipants.value = emptyList()
        _watchingStreamId.value = null
        watchMessagesJob?.cancel()
        watchSignalJob?.cancel()
        _watchingMessages.value = emptyList()
        webRtcEngine.closeViewerWatchSession()
        if (currentId != null && stream != null && stream.hostId != currentUserId && stream.status == "live") {
            viewModelScope.launch {
                repository.adjustStreamViewerCount(currentId, (stream.viewerCount - 1L).coerceAtLeast(1L))
            }
        }
    }

    fun sendChatMessage(streamId: String, text: String, messageType: String = "chat") {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.sendStreamMessage(streamId, myProfile, text, messageType)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.message ?: "Failed to send message."
            }
        }
    }

    // --- 1-on-1 Gamer Video Calls ---

    fun startVideoCall(
        targetUserId: String,
        targetUsername: String,
        targetAvatarData: String,
        gameContext: String = "",
        onCallCreated: (String) -> Unit
    ) {
        val myProfile = (myProfileState.value as? UiState.Success)?.data ?: GamerProfile(
            userId = currentUserId,
            username = "Gamer"
        )
        viewModelScope.launch {
            appliedCallCandidates.clear()
            appliedCallAnswer = false
            _micEnabled.value = true
            _cameraEnabled.value = true
            _speakerEnabled.value = true
            webRtcEngine.setSpeakerphoneOn(true)

            val pendingCandidates = mutableListOf<String>()
            val offerSdp = webRtcEngine.createCallOffer { candidate ->
                val callId = _activeCallId.value
                if (callId != null) {
                    launch { repository.appendCallCandidate(callId, isCaller = true, candidate = candidate) }
                } else {
                    pendingCandidates.add(candidate)
                }
            }

            val result = repository.startVideoCall(
                myProfile = myProfile,
                targetUserId = targetUserId,
                targetUsername = targetUsername,
                targetAvatarData = targetAvatarData,
                gameContext = gameContext,
                offerSdp = offerSdp ?: ""
            )
            result.onSuccess { callId ->
                _activeCallId.value = callId
                pendingCandidates.forEach { cand ->
                    repository.appendCallCandidate(callId, isCaller = true, candidate = cand)
                }
                observeActiveCallSignaling(callId, isCaller = true)
                onCallCreated(callId)
            }.onFailure { err ->
                webRtcEngine.closeCallSession()
                _statusBanner.value = err.message ?: "Failed to start video call."
            }
        }
    }

    fun acceptIncomingCall(call: VideoCall, onAccepted: (String) -> Unit) {
        viewModelScope.launch {
            appliedCallCandidates.clear()
            appliedCallAnswer = true
            _activeCallId.value = call.id
            _micEnabled.value = true
            _cameraEnabled.value = true
            _speakerEnabled.value = true
            webRtcEngine.setSpeakerphoneOn(true)

            val answerSdp = if (call.offerSdp.isNotBlank()) {
                webRtcEngine.acceptCallOfferAndCreateAnswer(call.offerSdp) { cand ->
                    launch { repository.appendCallCandidate(call.id, isCaller = false, candidate = cand) }
                }
            } else {
                webRtcEngine.startLocalMedia(enableVideo = true, enableAudio = true)
                ""
            }

            val res = repository.answerVideoCall(call.id, answerSdp ?: "")
            if (res.isSuccess) {
                call.callerCandidates.forEach { cand ->
                    if (appliedCallCandidates.add(cand)) {
                        webRtcEngine.addCallIceCandidate(cand)
                    }
                }
                observeActiveCallSignaling(call.id, isCaller = false)
                onAccepted(call.id)
            } else {
                webRtcEngine.closeCallSession()
                _activeCallId.value = null
                _statusBanner.value = "Could not accept call."
            }
        }
    }

    fun declineIncomingCall(callId: String) {
        viewModelScope.launch {
            repository.endOrDeclineVideoCall(callId, "declined")
        }
    }

    fun endActiveCall() {
        val callId = _activeCallId.value
        activeCallJob?.cancel()
        _activeCallId.value = null
        webRtcEngine.closeCallSession()
        if (callId != null) {
            viewModelScope.launch {
                repository.endOrDeclineVideoCall(callId, "ended")
            }
        }
    }

    private fun observeActiveCallSignaling(callId: String, isCaller: Boolean) {
        activeCallJob?.cancel()
        activeCallJob = viewModelScope.launch {
            repository.observeCall(callId)
                .catch { e -> Log.w(TAG, "Call observe error", e) }
                .collect { call ->
                    if (call == null) return@collect
                    if (isCaller && !appliedCallAnswer && call.answerSdp.isNotBlank()) {
                        appliedCallAnswer = true
                        webRtcEngine.applyCallAnswer(call.answerSdp)
                    }
                    val remoteCandidates = if (isCaller) call.calleeCandidates else call.callerCandidates
                    remoteCandidates.forEach { cand ->
                        if (appliedCallCandidates.add(cand)) {
                            webRtcEngine.addCallIceCandidate(cand)
                        }
                    }
                    if (call.status == "ended" || call.status == "declined") {
                        webRtcEngine.closeCallSession()
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        broadcastSignalsJob?.cancel()
        broadcastMessagesJob?.cancel()
        watchMessagesJob?.cancel()
        watchSignalJob?.cancel()
        activeCallJob?.cancel()
        voiceParticipantsJob?.cancel()
        voiceSignalsJob?.cancel()
        webRtcEngine.release()
    }

    private fun <T> Flow<T>.toUiState(fallbackMessage: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
            }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "NexusVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
