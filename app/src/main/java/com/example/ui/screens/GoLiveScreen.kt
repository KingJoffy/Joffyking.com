package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.LiveStream
import com.example.data.model.StreamMessage
import com.example.data.model.VoiceCommentaryCatalog
import com.example.data.model.VoiceParticipant
import com.example.rtc.WebRtcEngine
import com.example.ui.components.GamerAvatar
import com.example.ui.components.LiveBadge
import com.example.ui.components.WebRtcVideoSurface
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import kotlinx.coroutines.delay

@Composable
fun GoLiveScreen(
    webRtcEngine: WebRtcEngine,
    activeBroadcastId: String?,
    activeStream: LiveStream?,
    liveMessages: List<StreamMessage>,
    voiceParticipants: List<VoiceParticipant>,
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    initialCategory: GameCategory?,
    onStartPreview: () -> Unit,
    onGoLive: (title: String, category: GameCategory, region: String, voiceChannelEnabled: Boolean, voiceTopic: String) -> Unit,
    onEndBroadcast: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onUpdateVoiceSettings: (streamId: String, enabled: Boolean, topic: String) -> Unit,
    onHostMuteParticipant: (streamId: String, targetUserId: String, mute: Boolean) -> Unit,
    onHostRemoveParticipant: (streamId: String, targetUserId: String) -> Unit
) {
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

    val cameraReady by webRtcEngine.cameraReady.collectAsStateWithLifecycle()
    val isFrontCamera by webRtcEngine.isFrontCamera.collectAsStateWithLifecycle()
    val voicePeerCount by webRtcEngine.voiceActivePeerCount.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory ?: GameCatalog.categories.first())
    }
    var selectedRegion by remember { mutableStateOf(GameCatalog.regions.first()) }
    var voiceEnabledPreStream by remember { mutableStateOf(true) }
    var selectedVoiceTopic by remember { mutableStateOf(VoiceCommentaryCatalog.topics.first()) }
    var voiceDrawerExpanded by remember { mutableStateOf(false) }
    var elapsedSeconds by remember(activeBroadcastId) { mutableLongStateOf(0L) }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            onStartPreview()
        }
    }

    LaunchedEffect(activeBroadcastId) {
        if (activeBroadcastId != null) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!permissionsGranted) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(24.dp)
                    .align(Alignment.Center),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.permission_camera_mic_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.permission_camera_mic_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("grant_broadcast_permissions")
                    ) {
                        Text(stringResource(R.string.grant_permissions))
                    }
                }
            }
        } else if (activeBroadcastId != null) {
            val streamVoiceEnabled = activeStream?.voiceChannelEnabled ?: voiceEnabledPreStream
            val streamVoiceTopic = activeStream?.voiceTopic ?: selectedVoiceTopic
            val remoteSpeakersCount = voiceParticipants.count { it.userId != activeStream?.hostId }

            // Active Live Studio View
            Box(modifier = Modifier.fillMaxSize()) {
                if (cameraReady && cameraEnabled) {
                    WebRtcVideoSurface(
                        webRtcEngine = webRtcEngine,
                        isLocal = true,
                        mirror = isFrontCamera,
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
                            Icon(
                                imageVector = if (cameraEnabled) Icons.Default.LiveTv else Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (cameraEnabled) "Broadcasting Live Feed" else "Camera Paused (Audio Live)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top Live Telemetry + Voice Room Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiveBadge(viewerCount = activeStream?.viewerCount ?: 1L)
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(50)
                            ) {
                                val mins = elapsedSeconds / 60
                                val secs = elapsedSeconds % 60
                                Text(
                                    text = "%02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            color = ElectricViolet.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = activeStream?.gameTitle ?: selectedCategory.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Audio-Only Voice Commentary Status Pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (streamVoiceEnabled) NeonCyan.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { voiceDrawerExpanded = !voiceDrawerExpanded }
                            .testTag("host_voice_drawer_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Voice commentary status",
                                    tint = if (streamVoiceEnabled) EmeraldOnline else Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (streamVoiceEnabled) {
                                            "Voice Comms Open · $streamVoiceTopic"
                                        } else {
                                            "Voice Comms Paused"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${voiceParticipants.size} in audio ($remoteSpeakersCount viewers) · $voicePeerCount P2P audio links",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (voiceDrawerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (voiceDrawerExpanded) "Collapse voice panel" else "Manage voice commentary",
                                tint = Color.White
                            )
                        }
                    }

                    // Expandable Host Voice Commentary Moderation Console
                    AnimatedVisibility(visible = voiceDrawerExpanded) {
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Audio-Only Commentary Channel",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Moderate speakers, mute feedback, or switch tactics topic.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = streamVoiceEnabled,
                                        onCheckedChange = { enabled ->
                                            onUpdateVoiceSettings(activeBroadcastId, enabled, streamVoiceTopic)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianBg,
                                            checkedTrackColor = EmeraldOnline
                                        ),
                                        modifier = Modifier.testTag("live_voice_room_switch")
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(VoiceCommentaryCatalog.topics) { topic ->
                                        FilterChip(
                                            selected = streamVoiceTopic == topic,
                                            onClick = {
                                                selectedVoiceTopic = topic
                                                onUpdateVoiceSettings(activeBroadcastId, streamVoiceEnabled, topic)
                                            },
                                            label = { Text(topic) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan,
                                                selectedLabelColor = ObsidianBg
                                            )
                                        )
                                    }
                                }

                                if (voiceParticipants.isEmpty()) {
                                    Text(
                                        text = if (streamVoiceEnabled) {
                                            "Audio channel open — waiting for viewers to join for tactics or feedback."
                                        } else {
                                            "Turn on the switch above to invite viewers into audio-only comms."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(voiceParticipants, key = { it.userId }) { participant ->
                                            val isHostRow = participant.role == "host"
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    GamerAvatar(
                                                        avatarData = participant.avatarData,
                                                        username = participant.username,
                                                        size = 34.dp,
                                                        onlineStatus = if (participant.muted) "offline" else "online"
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = participant.username,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Surface(
                                                                color = if (isHostRow) ElectricViolet else NeonCyan.copy(alpha = 0.2f),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isHostRow) "HOST" else participant.focusTag,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = if (isHostRow) Color.White else NeonCyan,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = when {
                                                                participant.hostMuted -> "Muted by host"
                                                                participant.muted -> "Mic muted"
                                                                else -> "Live audio active"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = when {
                                                                participant.hostMuted -> AmberBusy
                                                                participant.muted -> LiveCrimson
                                                                else -> EmeraldOnline
                                                            }
                                                        )
                                                    }
                                                    if (!isHostRow) {
                                                        IconButton(
                                                            onClick = {
                                                                onHostMuteParticipant(
                                                                    activeBroadcastId,
                                                                    participant.userId,
                                                                    !participant.hostMuted
                                                                )
                                                            },
                                                            modifier = Modifier.minimumInteractiveComponentSize()
                                                        ) {
                                                            Icon(
                                                                imageVector = if (participant.hostMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                                                contentDescription = if (participant.hostMuted) {
                                                                    "Unmute ${participant.username}"
                                                                } else {
                                                                    "Mute ${participant.username}"
                                                                },
                                                                tint = if (participant.hostMuted) AmberBusy else EmeraldOnline
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                onHostRemoveParticipant(activeBroadcastId, participant.userId)
                                                            },
                                                            modifier = Modifier.minimumInteractiveComponentSize()
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PersonRemove,
                                                                contentDescription = "Remove ${participant.username} from voice channel",
                                                                tint = LiveCrimson
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Live Chat + Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    if (liveMessages.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(liveMessages.reversed(), key = { it.id }) { msg ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${msg.senderUsername}: ${msg.text}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BroadcastControlButton(
                            icon = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            label = if (micEnabled) "Mute" else "Unmute",
                            active = micEnabled,
                            onClick = onToggleMic
                        )
                        BroadcastControlButton(
                            icon = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = if (cameraEnabled) "Cam On" else "Cam Off",
                            active = cameraEnabled,
                            onClick = onToggleCamera
                        )
                        BroadcastControlButton(
                            icon = Icons.Default.Cameraswitch,
                            label = "Flip",
                            active = true,
                            onClick = onSwitchCamera
                        )
                        Button(
                            onClick = onEndBroadcast,
                            colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("end_stream_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = "End live stream",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("End Live", color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Pre-Broadcast Setup Studio
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Broadcast Studio",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Live Camera Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (cameraReady && cameraEnabled) {
                        WebRtcVideoSurface(
                            webRtcEngine = webRtcEngine,
                            isLocal = true,
                            mirror = isFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (cameraEnabled) "WebRTC Camera Ready" else "Camera Off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onToggleMic,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(if (micEnabled) Color.Black.copy(alpha = 0.6f) else LiveCrimson)
                        ) {
                            Icon(
                                imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle mic",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onToggleCamera,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(if (cameraEnabled) Color.Black.copy(alpha = 0.6f) else LiveCrimson)
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
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = Color.White
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Stream Title") },
                    placeholder = { Text("e.g., NBA 2K27 Pro-AM Tactics & Viewer Comms!") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_title_input")
                )

                Column {
                    Text(
                        text = "Select Game Category",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(GameCatalog.categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategory.id == cat.id,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Broadcast Region",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GameCatalog.regions) { reg ->
                            FilterChip(
                                selected = selectedRegion == reg,
                                onClick = { selectedRegion = reg },
                                label = { Text(reg) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                // Real-Time Audio-Only Voice Commentary Setup Card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (voiceEnabledPreStream) NeonCyan.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = if (voiceEnabledPreStream) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Live Voice Commentary Room",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Viewers join audio-only comms for tactics & live feedback.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = voiceEnabledPreStream,
                                onCheckedChange = { voiceEnabledPreStream = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBg,
                                    checkedTrackColor = NeonCyan
                                ),
                                modifier = Modifier.testTag("pre_stream_voice_toggle")
                            )
                        }

                        AnimatedVisibility(visible = voiceEnabledPreStream) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Commentary Focus Topic",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(VoiceCommentaryCatalog.topics) { topic ->
                                        FilterChip(
                                            selected = selectedVoiceTopic == topic,
                                            onClick = { selectedVoiceTopic = topic },
                                            label = { Text(topic) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricViolet,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val isTitleValid = title.trim().length >= 3
                Button(
                    onClick = {
                        onGoLive(
                            title.trim(),
                            selectedCategory,
                            selectedRegion,
                            voiceEnabledPreStream,
                            selectedVoiceTopic
                        )
                    },
                    enabled = isTitleValid,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_broadcast_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("GO LIVE TO THE WORLD", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BroadcastControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(if (active) Color.White.copy(alpha = 0.16f) else LiveCrimson.copy(alpha = 0.85f))
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
