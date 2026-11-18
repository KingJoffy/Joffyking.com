package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.GamerProfile
import com.example.data.model.LiveStream
import com.example.data.model.UiState
import com.example.ui.components.GamerAvatar
import com.example.ui.components.StreamCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.SearchFilterTab

@Composable
fun SearchScreen(
    query: String,
    activeTab: SearchFilterTab,
    streamsState: UiState<List<LiveStream>>,
    gamersState: UiState<List<GamerProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    currentUserId: String,
    onQueryChange: (String) -> Unit,
    onTabChange: (SearchFilterTab) -> Unit,
    onCategoryClick: (GameCategory) -> Unit,
    onWatchStream: (LiveStream) -> Unit,
    onSendFriendRequest: (GamerProfile) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onCallUser: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit
) {
    val q = query.trim().lowercase()
    val streams = (streamsState as? UiState.Success)?.data.orEmpty()
    val gamers = (gamersState as? UiState.Success)?.data.orEmpty()
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()

    val matchedGames = remember(q) {
        if (q.isEmpty()) GameCatalog.categories
        else GameCatalog.categories.filter { cat ->
            cat.title.lowercase().contains(q) ||
                cat.genre.lowercase().contains(q) ||
                cat.tags.any { it.lowercase().contains(q) }
        }
    }

    val matchedStreams = remember(q, streams) {
        if (q.isEmpty()) streams
        else streams.filter { s ->
            s.titleLower.contains(q) ||
                s.gameTitleLower.contains(q) ||
                s.hostUsername.lowercase().contains(q)
        }
    }

    val matchedGamers = remember(q, gamers) {
        if (q.isEmpty()) gamers
        else gamers.filter { g ->
            g.usernameLower.contains(q) ||
                g.favoriteGame.lowercase().contains(q) ||
                g.bio.lowercase().contains(q)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search games, streams, or gamers...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = NeonCyan
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SearchFilterTab.entries) { tab ->
                        val label = when (tab) {
                            SearchFilterTab.ALL -> "All"
                            SearchFilterTab.GAMES -> "Games (${matchedGames.size})"
                            SearchFilterTab.STREAMS -> "Live (${matchedStreams.size})"
                            SearchFilterTab.GAMERS -> "Gamers (${matchedGamers.size})"
                        }
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { onTabChange(tab) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Games section
            if (activeTab == SearchFilterTab.ALL || activeTab == SearchFilterTab.GAMES) {
                item {
                    Text(
                        text = "Games",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }
                if (matchedGames.isEmpty()) {
                    item {
                        Text(
                            text = "No matching game categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(matchedGames, key = { "game_${it.id}" }) { game ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(game) }
                                .testTag("search_game_${game.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = Color(game.accentHex),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = game.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${game.genre} • ${game.tags.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val liveCount = streams.count { it.gameId == game.id }
                                if (liveCount > 0) {
                                    Text(
                                        text = "$liveCount LIVE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gamers section
            if (activeTab == SearchFilterTab.ALL || activeTab == SearchFilterTab.GAMERS) {
                item {
                    Text(
                        text = "Gamers",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }
                if (matchedGamers.isEmpty()) {
                    item {
                        Text(
                            text = if (q.isEmpty()) "No other discoverable gamers online yet." else "No gamers matched \"$query\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(matchedGamers, key = { "gamer_${it.userId}" }) { gamer ->
                        val relation = friendships.firstOrNull {
                            it.memberUids.contains(gamer.userId)
                        }
                        GamerSearchRow(
                            gamer = gamer,
                            friendship = relation,
                            currentUserId = currentUserId,
                            onSendRequest = { onSendFriendRequest(gamer) },
                            onAcceptRequest = { relation?.let { onAcceptFriendRequest(it.id) } },
                            onCallClick = {
                                onCallUser(
                                    gamer.userId,
                                    gamer.username,
                                    gamer.avatarData,
                                    gamer.favoriteGame
                                )
                            }
                        )
                    }
                }
            }

            // Live Streams section
            if (activeTab == SearchFilterTab.ALL || activeTab == SearchFilterTab.STREAMS) {
                item {
                    Text(
                        text = "Live Streams",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }
                if (matchedStreams.isEmpty()) {
                    item {
                        Text(
                            text = if (q.isEmpty()) "No active live streams right now." else "No live streams matched \"$query\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(matchedStreams, key = { "stream_${it.id}" }) { stream ->
                        StreamCard(
                            stream = stream,
                            onWatchClick = { onWatchStream(stream) },
                            onCallHostClick = if (stream.hostId != currentUserId) {
                                {
                                    onCallUser(
                                        stream.hostId,
                                        stream.hostUsername,
                                        stream.hostAvatarData,
                                        stream.gameTitle
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamerSearchRow(
    gamer: GamerProfile,
    friendship: Friendship?,
    currentUserId: String,
    onSendRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onCallClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_gamer_${gamer.userId}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GamerAvatar(
                avatarData = gamer.avatarData,
                username = gamer.username,
                size = 48.dp,
                onlineStatus = gamer.onlineStatus
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gamer.username,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (gamer.favoriteGame.isNotBlank()) {
                    Text(
                        text = "Plays ${gamer.favoriteGame}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
                if (gamer.bio.isNotBlank()) {
                    Text(
                        text = gamer.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))

            when {
                friendship?.status == "accepted" -> {
                    Surface(
                        color = EmeraldOnline.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Friends",
                                tint = EmeraldOnline,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Friends",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOnline
                            )
                        }
                    }
                }
                friendship?.status == "pending" && friendship.recipientId == currentUserId -> {
                    Button(
                        onClick = onAcceptRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Accept", style = MaterialTheme.typography.labelMedium)
                    }
                }
                friendship?.status == "pending" -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Pending", style = MaterialTheme.typography.labelMedium)
                    }
                }
                else -> {
                    IconButton(
                        onClick = onSendRequest,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_friend_${gamer.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Send friend request to ${gamer.username}",
                            tint = ElectricViolet
                        )
                    }
                }
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("call_gamer_${gamer.userId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video call ${gamer.username}",
                    tint = NeonCyan
                )
            }
        }
    }
}
