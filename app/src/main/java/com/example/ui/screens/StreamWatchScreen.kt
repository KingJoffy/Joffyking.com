package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameCatalog
import com.example.data.model.GamerProfile
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

@Composable
fun StreamWatchScreen(
    stream: LiveStream,
    messages: List<StreamMessage>,
    voiceParticipants: List<VoiceParticipant>,
    inVoiceChannel: Boolean,
    voiceMicMuted: Boolean,
    voiceHostMutedMe: Boolean,
    voiceFocusTag: String,
    speakerEnabled: Boolean,
    webRtcEngine: WebRtcEngine,
    currentUserId: String,
    isAlreadyFriend: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String, String) -> Unit,
    onAddFriend: (GamerProfile) -> Unit,
    onCallHost: () -> Unit,
    onJoinVoice: (withMicPermission: Boolean, focusTag: String) -> Unit,
    onLeaveVoice: () -> Unit,
    onToggleVoiceMic: (hasAudioPermission: Boolean) -> Unit,
    onSelectVoiceFocus: (String) -> Unit,
    onToggleSpeakerphone: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        when (pendingAction) {
            "join" -> onJoinVoice(granted, voiceFocusTag)
            "unmute" -> if (granted) onToggleVoiceMic(true) else onToggleVoiceMic(false)
        }
        pendingAction = null
    }

    val remoteVideoActive by webRtcEngine.remoteVideoActive.collectAsStateWithLifecycle()
    val voicePeerCount by webRtcEngine.voiceActivePeerCount.collectAsStateWithLifecycle()
    val category = remember(stream.gameId) { GameCatalog.findById(stream.gameId) }
    var chatDraft by remember { mutableStateOf("") }
    val quickReactions = listOf("GG!", "CLUTCH", "HYPE", "LET'S GO")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Video Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
                .background(Color.Black)
        ) {
            if (remoteVideoActive) {
                WebRtcVideoSurface(
                    webRtcEngine = webRtcEngine,
                    isLocal = false,
                    mirror = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = category.coverRes),
                    contentDescription = stream.gameTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GamerAvatar(
                        avatarData = stream.hostAvatarData,
                        username = stream.hostUsername,
                        size = 64.dp,
                        onlineStatus = "streaming"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (stream.hostId == currentUserId) "Previewing Your Broadcast" else "WebRTC Stream Connected",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${stream.gameTitle} • ${stream.region}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("watch_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave stream",
                        tint = Color.White
                    )
                }
                LiveBadge(viewerCount = stream.viewerCount)
            }
        }

        // Host Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamerAvatar(
                    avatarData = stream.hostAvatarData,
                    username = stream.hostUsername,
                    size = 42.dp,
                    onlineStatus = "streaming"
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stream.hostUsername} • ${stream.gameTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
                if (stream.hostId != currentUserId) {
                    if (!isAlreadyFriend) {
                        IconButton(
                            onClick = {
                                onAddFriend(
                                    GamerProfile(
                                        userId = stream.hostId,
                                        username = stream.hostUsername,
                                        avatarData = stream.hostAvatarData
                                    )
                                )
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add ${stream.hostUsername} as friend",
                                tint = ElectricViolet
                            )
                        }
                    }
                    IconButton(
                        onClick = onCallHost,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video call ${stream.hostUsername}",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }

        // Audio-Only Voice Commentary Channel Card
        Surface(
            color = if (inVoiceChannel) {
                ElectricViolet.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            border = BorderStroke(
                1.dp,
                when {
                    inVoiceChannel -> EmeraldOnline.copy(alpha = 0.7f)
                    stream.voiceChannelEnabled -> NeonCyan.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("viewer_voice_commentary_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = if (inVoiceChannel) EmeraldOnline.copy(alpha = 0.22f) else NeonCyan.copy(alpha = 0.16f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (inVoiceChannel) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                                    contentDescription = "Audio-only voice commentary",
                                    tint = if (inVoiceChannel) EmeraldOnline else NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Voice Commentary · ${stream.voiceTopic.ifBlank { "Tactics & Feedback" }}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    inVoiceChannel -> "Audio-Only Active (Cam Off) · ${voiceParticipants.size} in room · $voicePeerCount WebRTC links"
                                    stream.voiceChannelEnabled -> "${voiceParticipants.size} in audio-only room · Discuss tactics or feedback"
                                    else -> "Host paused voice commentary"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inVoiceChannel) EmeraldOnline else NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    if (inVoiceChannel) {
                        OutlinedButton(
                            onClick = onLeaveVoice,
                            border = BorderStroke(1.dp, LiveCrimson),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveCrimson),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("leave_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Leave voice commentary",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Leave")
                        }
                    } else if (stream.voiceChannelEnabled || stream.hostId == currentUserId) {
                        Button(
                            onClick = {
                                if (hasAudioPermission) {
                                    onJoinVoice(true, voiceFocusTag)
                                } else {
                                    pendingAction = "join"
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("join_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Join Audio")
                        }
                    }
                }

                // Active Voice Commentary Speaker Chips
                if (voiceParticipants.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(voiceParticipants, key = { it.userId }) { speaker ->
                            val isSpeakerHost = speaker.role == "host"
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        speaker.hostMuted -> AmberBusy.copy(alpha = 0.6f)
                                        speaker.muted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        isSpeakerHost -> ElectricViolet.copy(alpha = 0.8f)
                                        else -> EmeraldOnline.copy(alpha = 0.7f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    GamerAvatar(
                                        avatarData = speaker.avatarData,
                                        username = speaker.username,
                                        size = 22.dp
                                    )
                                    Text(
                                        text = if (speaker.userId == currentUserId) "${speaker.username} (You)" else speaker.username,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Surface(
                                        color = if (isSpeakerHost) ElectricViolet.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isSpeakerHost) "Host" else speaker.focusTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSpeakerHost) Color.White else NeonCyan,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (speaker.muted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = if (speaker.muted) "Muted" else "Speaking",
                                        tint = when {
                                            speaker.hostMuted -> AmberBusy
                                            speaker.muted -> LiveCrimson
                                            else -> EmeraldOnline
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Viewer Voice Controls (Focus Tag + Mic + Speakerphone)
                AnimatedVisibility(visible = inVoiceChannel) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(VoiceCommentaryCatalog.focusTags) { tag ->
                                FilterChip(
                                    selected = voiceFocusTag == tag,
                                    onClick = { onSelectVoiceFocus(tag) },
                                    label = { Text(tag) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("voice_focus_${tag}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (voiceMicMuted && !hasAudioPermission) {
                                        pendingAction = "unmute"
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        onToggleVoiceMic(hasAudioPermission)
                                    }
                                },
                                enabled = !voiceHostMutedMe,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        voiceHostMutedMe -> AmberBusy.copy(alpha = 0.25f)
                                        voiceMicMuted -> LiveCrimson
                                        else -> EmeraldOnline
                                    },
                                    contentColor = if (voiceMicMuted && !voiceHostMutedMe) Color.White else ObsidianBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("voice_mic_toggle")
                            ) {
                                Icon(
                                    imageVector = if (voiceMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Toggle commentary mic",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        voiceHostMutedMe -> "Muted by Host"
                                        voiceMicMuted -> "Unmute Mic"
                                        else -> "Mic Live"
                                    }
                                )
                            }

                            OutlinedButton(
                                onClick = onToggleSpeakerphone,
                                border = BorderStroke(1.dp, if (speakerEnabled) NeonCyan else MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (speakerEnabled) NeonCyan else Color.White
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("voice_speaker_toggle")
                            ) {
                                Icon(
                                    imageVector = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Toggle speakerphone",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (speakerEnabled) "Speaker" else "Earpiece")
                            }
                        }
                    }
                }
            }
        }

        // Live Chat
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "Welcome to ${stream.hostUsername}'s live chat! Join Voice Audio above or say GG below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    Row(verticalAlignment = Alignment.Top) {
                        GamerAvatar(
                            avatarData = msg.senderAvatarData,
                            username = msg.senderUsername,
                            size = 28.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (msg.messageType == "reaction") ElectricViolet.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${msg.senderUsername}: ${msg.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick reactions & composer
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickReactions) { label ->
                AssistChip(
                    onClick = { onSendMessage(label, "reaction") },
                    label = { Text(label) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = NeonCyan)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatDraft,
                onValueChange = { if (it.length <= 300) chatDraft = it },
                placeholder = { Text("Send live chat message...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stream_chat_input")
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = chatDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendMessage(trimmed, "chat")
                        chatDraft = ""
                    }
                },
                enabled = chatDraft.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(ElectricViolet)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send chat message",
                    tint = Color.White
                )
            }
        }
    }
}
