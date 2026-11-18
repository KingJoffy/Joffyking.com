package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.VideoCall
import com.example.rtc.WebRtcEngine
import com.example.ui.components.GamerAvatar
import com.example.ui.components.WebRtcVideoSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import kotlinx.coroutines.delay

@Composable
fun VideoCallScreen(
    call: VideoCall?,
    currentUserId: String,
    webRtcEngine: WebRtcEngine,
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    speakerEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    BackHandler(onBack = onEndCall)

    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results[Manifest.permission.CAMERA] == true &&
            results[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(permissionsGranted) {
        if (!permissionsGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else {
            webRtcEngine.startLocalMedia(enableVideo = true, enableAudio = true)
        }
    }

    val cameraReady by webRtcEngine.cameraReady.collectAsStateWithLifecycle()
    val remoteVideoActive by webRtcEngine.remoteVideoActive.collectAsStateWithLifecycle()
    val isFrontCamera by webRtcEngine.isFrontCamera.collectAsStateWithLifecycle()

    var activeSeconds by remember(call?.status) { mutableLongStateOf(0L) }
    LaunchedEffect(call?.status) {
        if (call?.status == "active") {
            while (true) {
                delay(1000L)
                activeSeconds++
            }
        }
    }

    val isCaller = call?.callerId == currentUserId
    val peerName = if (call == null) "Gamer" else if (isCaller) call.calleeName else call.callerName
    val peerAvatar = if (call == null) "" else if (isCaller) call.calleeAvatarData else call.callerAvatarData

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Remote full-screen video or connecting stage
        if (remoteVideoActive) {
            WebRtcVideoSurface(
                webRtcEngine = webRtcEngine,
                isLocal = false,
                mirror = false,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(ElectricViolet.copy(alpha = 0.35f), ObsidianBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GamerAvatar(
                        avatarData = peerAvatar,
                        username = peerName,
                        size = 112.dp,
                        onlineStatus = "in_call"
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    val statusText = when (call?.status) {
                        "active" -> "Connected HD P2P"
                        "declined" -> "Call declined"
                        "ended" -> "Call ended"
                        else -> "Ringing..."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan
                    )
                    if (!permissionsGranted) {
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) {
                            Text(stringResource(R.string.grant_permissions))
                        }
                    }
                }
            }
        }

        // Top Call Status + Local PiP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    val timeLabel = if (call?.status == "active") {
                        "%02d:%02d • P2P HD".format(activeSeconds / 60, activeSeconds % 60)
                    } else {
                        call?.status?.uppercase() ?: "RINGING"
                    }
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (call?.status == "active") EmeraldOnline else NeonCyan
                    )
                }
            }

            if (permissionsGranted && cameraReady && cameraEnabled) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, ElectricViolet),
                    modifier = Modifier.size(width = 104.dp, height = 148.dp)
                ) {
                    WebRtcVideoSurface(
                        webRtcEngine = webRtcEngine,
                        isLocal = true,
                        mirror = isFrontCamera,
                        isOverlay = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Bottom Controls Dock
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(if (micEnabled) MaterialTheme.colorScheme.surfaceVariant else LiveCrimson)
                ) {
                    Icon(
                        imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle microphone",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onToggleCamera,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(if (cameraEnabled) MaterialTheme.colorScheme.surfaceVariant else LiveCrimson)
                ) {
                    Icon(
                        imageVector = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle camera",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(if (speakerEnabled) ElectricViolet else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle speakerphone",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(LiveCrimson)
                        .testTag("end_video_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End video call",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
