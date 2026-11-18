package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rtc.WebRtcEngine
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun WebRtcVideoSurface(
    webRtcEngine: WebRtcEngine,
    isLocal: Boolean,
    mirror: Boolean = isLocal,
    isOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember(context) {
        SurfaceViewRenderer(context).apply {
            init(webRtcEngine.eglBase.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
            setMirror(mirror)
            setZOrderMediaOverlay(isOverlay)
        }
    }

    DisposableEffect(renderer, isLocal) {
        if (isLocal) {
            webRtcEngine.attachLocalRenderer(renderer)
        } else {
            webRtcEngine.attachRemoteRenderer(renderer)
        }
        onDispose {
            if (isLocal) {
                webRtcEngine.detachLocalRenderer(renderer)
            } else {
                webRtcEngine.detachRemoteRenderer(renderer)
            }
            renderer.release()
        }
    }

    AndroidView(
        factory = { renderer },
        update = { view -> view.setMirror(mirror) },
        modifier = modifier
    )
}
