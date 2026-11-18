package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GamerProfile
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UiState
import com.example.ui.components.GamerAvatar
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.util.AvatarUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    myProfileState: UiState<GamerProfile?>,
    friendshipsState: UiState<List<Friendship>>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    onSaveProfile: (username: String, bio: String, avatarData: String, favoriteGame: String, discoverable: Boolean) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onDeclineFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onCallFriend: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenMatchHistory: () -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile = (myProfileState as? UiState.Success)?.data
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()

    val incomingRequests = friendships.filter {
        it.status == "pending" && it.recipientId == currentUserId
    }
    val outgoingRequests = friendships.filter {
        it.status == "pending" && it.requesterId == currentUserId
    }
    val acceptedFriends = friendships.filter { it.status == "accepted" }
    val leaderboardEntries = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val myRankIndex = leaderboardEntries.indexOfFirst { it.userId == currentUserId }
    val myRankEntry = if (myRankIndex >= 0) leaderboardEntries[myRankIndex] else null

    var username by remember(profile?.userId, profile?.username) {
        mutableStateOf(profile?.username ?: "")
    }
    var bio by remember(profile?.userId, profile?.bio) {
        mutableStateOf(profile?.bio ?: "")
    }
    var avatarData by remember(profile?.userId, profile?.avatarData) {
        mutableStateOf(profile?.avatarData ?: GameCatalog.avatarPresets.first().key)
    }
    var favoriteGame by remember(profile?.userId, profile?.favoriteGame) {
        mutableStateOf(profile?.favoriteGame ?: GameCatalog.categories.first().title)
    }
    var discoverable by remember(profile?.userId, profile?.discoverable) {
        mutableStateOf(profile?.discoverable ?: true)
    }
    var isProcessingAvatar by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null && username.isBlank()) {
            username = profile.username
            bio = profile.bio
            avatarData = profile.avatarData
            favoriteGame = profile.favoriteGame
            discoverable = profile.discoverable
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingAvatar = true
            scope.launch {
                val encoded = AvatarUtils.encodeUriToAvatarData(context, uri)
                isProcessingAvatar = false
                encoded.onSuccess { dataUri ->
                    avatarData = dataUri
                    onShowMessage("Avatar ready — tap Save Profile to publish!")
                }.onFailure {
                    onShowMessage("Could not process selected photo.")
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gamer Profile & Squad",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = LiveCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = LiveCrimson)
                    }
                }
            }

            // Competitive W/L Record Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, AmberBusy.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = AmberBusy,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (myRankIndex >= 0) "Global Rank #${myRankIndex + 1}" else "Competitive W/L Record",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${myRankEntry?.wins ?: 0}W - ${myRankEntry?.losses ?: 0}L • ${myRankEntry?.winRatePercent ?: 0}% Win Rate • ${myRankEntry?.rating ?: 1000} MMR",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenLeaderboard,
                                border = BorderStroke(1.dp, AmberBusy.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_leaderboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = AmberBusy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Leaderboard", color = AmberBusy)
                            }
                            OutlinedButton(
                                onClick = onOpenMatchHistory,
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_match_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Match History", color = NeonCyan)
                            }
                        }
                    }
                }
            }

            // Profile Editor Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamerAvatar(
                                avatarData = avatarData,
                                username = username.ifBlank { "G" },
                                size = 76.dp,
                                onlineStatus = profile?.onlineStatus ?: "online"
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = username.ifBlank { "Your Gamertag" },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${acceptedFriends.size} Friends",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isProcessingAvatar,
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("upload_avatar_button")
                                ) {
                                    if (isProcessingAvatar) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upload Avatar", color = NeonCyan)
                                }
                            }
                        }

                        // Preset avatar quick-picker
                        Column {
                            Text(
                                text = "Or pick a Cyber Avatar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(GameCatalog.avatarPresets, key = { it.key }) { preset ->
                                    val selected = avatarData == preset.key
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(
                                                width = if (selected) 2.5.dp else 0.dp,
                                                color = if (selected) NeonCyan else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .padding(3.dp)
                                            .clickable { avatarData = preset.key }
                                    ) {
                                        GamerAvatar(
                                            avatarData = preset.key,
                                            username = preset.monogram,
                                            size = 44.dp
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { if (it.length <= 24) username = it },
                            label = { Text("Username (3-24 chars)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_username_input")
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 240) bio = it },
                            label = { Text("Short Biography") },
                            placeholder = { Text("Tell other gamers what you play & stream...") },
                            minLines = 2,
                            maxLines = 4,
                            supportingText = { Text("${bio.length}/240") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_bio_input")
                        )

                        Column {
                            Text(
                                text = "Main Game",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GameCatalog.categories, key = { it.id }) { cat ->
                                    FilterChip(
                                        selected = favoriteGame == cat.title,
                                        onClick = { favoriteGame = cat.title },
                                        label = { Text(cat.title) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Discoverable in Search",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Allow other gamers to find you & send friend requests",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = discoverable,
                                onCheckedChange = { discoverable = it }
                            )
                        }

                        Button(
                            onClick = {
                                onSaveProfile(username, bio, avatarData, favoriteGame, discoverable)
                            },
                            enabled = username.trim().length in 3..24,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Save Profile")
                        }
                    }
                }
            }

            // Incoming Friend Requests
            if (incomingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Incoming Friend Requests (${incomingRequests.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }
                items(incomingRequests, key = { "in_${it.id}" }) { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamerAvatar(
                                avatarData = req.requesterAvatarData,
                                username = req.requesterName,
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = req.requesterName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Wants to join your squad",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeclineFriend(req.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("decline_friend_${req.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Decline friend request",
                                    tint = LiveCrimson
                                )
                            }
                            Button(
                                onClick = { onAcceptFriend(req.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("accept_friend_${req.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Accept")
                            }
                        }
                    }
                }
            }

            // Outgoing Pending Requests
            if (outgoingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Sent Friend Requests (${outgoingRequests.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(outgoingRequests, key = { "out_${it.id}" }) { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamerAvatar(
                                avatarData = req.recipientAvatarData,
                                username = req.recipientName,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = req.recipientName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Pending response",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { onRemoveFriend(req.id) }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Current Friends List
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Current Friends (${acceptedFriends.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (acceptedFriends.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "No friends added yet. Use the Search tab to find gamers and send friend requests!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(acceptedFriends, key = { "friend_${it.id}" }) { friendship ->
                    val isRequester = friendship.requesterId == currentUserId
                    val friendId = if (isRequester) friendship.recipientId else friendship.requesterId
                    val friendName = if (isRequester) friendship.recipientName else friendship.requesterName
                    val friendAvatar = if (isRequester) friendship.recipientAvatarData else friendship.requesterAvatarData

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_card_$friendId"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamerAvatar(
                                avatarData = friendAvatar,
                                username = friendName,
                                size = 46.dp,
                                onlineStatus = "online"
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = friendName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Squad Friend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldOnline
                                )
                            }
                            IconButton(
                                onClick = { onCallFriend(friendId, friendName, friendAvatar, "Friend Call") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("call_friend_$friendId")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video call $friendName",
                                    tint = NeonCyan
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFriend(friendship.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remove_friend_$friendId")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove $friendName from friends",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
