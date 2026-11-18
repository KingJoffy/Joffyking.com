package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GamerProfile
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchRecord
import com.example.data.model.UiState
import com.example.ui.components.GamerAvatar
import com.example.ui.components.LogMatchDialog
import com.example.ui.components.LogMatchInput
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan

private val GoldRank = Color(0xFFFBBF24)
private val SilverRank = Color(0xFFCBD5E1)
private val BronzeRank = Color(0xFFD97706)

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    matchHistoryState: UiState<List<MatchRecord>>,
    friendshipsState: UiState<List<Friendship>>,
    currentUserId: String,
    onRecordMatch: (LogMatchInput) -> Unit,
    onOpenMatchHistory: () -> Unit,
    onSendFriendRequest: (GamerProfile) -> Unit,
    onCallPlayer: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedGameTitle by remember { mutableStateOf<String?>(null) }
    var showLogMatchDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val entries = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val filteredEntries = remember(entries, selectedGameTitle) {
        if (selectedGameTitle == null) entries
        else entries.filter { it.favoriteGame.equals(selectedGameTitle, ignoreCase = true) }
    }
    val myRankIndex = entries.indexOfFirst { it.userId == currentUserId }
    val myEntry = if (myRankIndex >= 0) entries[myRankIndex] else null
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val matchHistory = (matchHistoryState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("leaderboard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Global Leaderboard",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Competitive W/L & MMR rankings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { showLogMatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("log_match_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Log Match")
                    }
                }
            }

            // My Standing Summary Card
            item {
                MyCompetitiveCard(
                    rank = if (myRankIndex >= 0) myRankIndex + 1 else null,
                    entry = myEntry,
                    showHistory = showHistory,
                    onToggleHistory = { showHistory = !showHistory },
                    onOpenMatchHistory = onOpenMatchHistory,
                    onLogMatchClick = { showLogMatchDialog = true }
                )
            }

            if (showHistory) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Matches (${matchHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan
                        )
                        TextButton(onClick = onOpenMatchHistory) {
                            Text("Full Analytics ->", color = NeonCyan)
                        }
                    }
                }
                if (matchHistory.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No matches logged yet. Tap Log Match above to record your first Win or Loss!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(matchHistory.take(10), key = { it.id }) { match ->
                        MatchHistoryRow(match = match)
                    }
                }
            }

            // Game filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedGameTitle == null,
                            onClick = { selectedGameTitle = null },
                            label = { Text("All Games") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(GameCatalog.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedGameTitle == cat.title,
                            onClick = {
                                selectedGameTitle = if (selectedGameTitle == cat.title) null else cat.title
                            },
                            label = { Text(cat.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            when (leaderboardState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = LiveCrimson.copy(alpha = 0.16f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = leaderboardState.message,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (filteredEntries.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "No ranked players found for this filter yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        val topThree = filteredEntries.take(3)
                        item {
                            Text(
                                text = "Top Champions",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldRank
                            )
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                topThree.forEachIndexed { idx, player ->
                                    PodiumPlayerCard(
                                        rank = idx + 1,
                                        player = player,
                                        isMe = player.userId == currentUserId,
                                        isFriend = friendships.any {
                                            it.status == "accepted" && it.memberUids.contains(player.userId)
                                        },
                                        onAddFriend = {
                                            onSendFriendRequest(
                                                GamerProfile(
                                                    userId = player.userId,
                                                    username = player.username,
                                                    avatarData = player.avatarData
                                                )
                                            )
                                        },
                                        onCall = {
                                            onCallPlayer(
                                                player.userId,
                                                player.username,
                                                player.avatarData,
                                                player.favoriteGame
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        val remaining = filteredEntries.drop(3)
                        if (remaining.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Global Standings",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            itemsIndexed(remaining, key = { _, item -> item.userId }) { idx, player ->
                                LeaderboardRankRow(
                                    rank = idx + 4,
                                    player = player,
                                    isMe = player.userId == currentUserId,
                                    isFriend = friendships.any {
                                        it.status == "accepted" && it.memberUids.contains(player.userId)
                                    },
                                    onAddFriend = {
                                        onSendFriendRequest(
                                            GamerProfile(
                                                userId = player.userId,
                                                username = player.username,
                                                avatarData = player.avatarData
                                            )
                                        )
                                    },
                                    onCall = {
                                        onCallPlayer(
                                            player.userId,
                                            player.username,
                                            player.avatarData,
                                            player.favoriteGame
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogMatchDialog) {
        LogMatchDialog(
            friendships = friendships,
            currentUserId = currentUserId,
            onDismiss = { showLogMatchDialog = false },
            onSubmit = { input ->
                showLogMatchDialog = false
                onRecordMatch(input)
            }
        )
    }
}

@Composable
private fun MyCompetitiveCard(
    rank: Int?,
    entry: LeaderboardEntry?,
    showHistory: Boolean,
    onToggleHistory: () -> Unit,
    onOpenMatchHistory: () -> Unit,
    onLogMatchClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ElectricViolet.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = GoldRank,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (rank != null) "Your Global Rank: #$rank" else "Your Competitive Record",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${entry?.rating ?: 1000} MMR • ${entry?.winRatePercent ?: 0}% Win Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
                OutlinedButton(
                    onClick = onToggleHistory,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showHistory) "Hide" else "Quick Log")
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill("WINS", "${entry?.wins ?: 0}", EmeraldOnline)
                StatPill("LOSSES", "${entry?.losses ?: 0}", LiveCrimson)
                StatPill("STREAK", "${entry?.currentStreak ?: 0}", AmberBusy)
                StatPill("BEST", "${entry?.bestStreak ?: 0}", NeonCyan)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ((entry?.totalMatches ?: 0L) == 0L) {
                    TextButton(onClick = onLogMatchClick) {
                        Text("Log first match", color = AmberBusy)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                OutlinedButton(
                    onClick = onOpenMatchHistory,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("leaderboard_open_match_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Detailed Match History ->", color = NeonCyan)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PodiumPlayerCard(
    rank: Int,
    player: LeaderboardEntry,
    isMe: Boolean,
    isFriend: Boolean,
    onAddFriend: () -> Unit,
    onCall: () -> Unit
) {
    val rankColor = when (rank) {
        1 -> GoldRank
        2 -> SilverRank
        else -> BronzeRank
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("podium_player_$rank"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, rankColor.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = rankColor
                    )
                }
                Spacer(Modifier.width(10.dp))
                GamerAvatar(
                    avatarData = player.avatarData,
                    username = player.username,
                    size = 48.dp,
                    onlineStatus = "online"
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMe) "${player.username} (You)" else player.username,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (player.currentStreak >= 2) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Win streak",
                                tint = AmberBusy,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${player.currentStreak}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberBusy
                            )
                        }
                    }
                    Text(
                        text = "${player.favoriteGame.ifBlank { "All Games" }} • ${player.wins}W - ${player.losses}L (${player.winRatePercent}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${player.rating}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = rankColor
                    )
                    Text(
                        text = "MMR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (player.winRatePercent / 100f).coerceIn(0f, 1f) },
                color = rankColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            if (!isMe) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isFriend) {
                        IconButton(
                            onClick = onAddFriend,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add ${player.username} as friend",
                                tint = ElectricViolet
                            )
                        }
                    }
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video call ${player.username}",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRankRow(
    rank: Int,
    player: LeaderboardEntry,
    isMe: Boolean,
    isFriend: Boolean,
    onAddFriend: () -> Unit,
    onCall: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isMe) NeonCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(34.dp)
            )
            GamerAvatar(
                avatarData = player.avatarData,
                username = player.username,
                size = 40.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${player.username} (You)" else player.username,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.wins}W - ${player.losses}L • ${player.winRatePercent}% WR • ${player.rating} MMR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isMe) {
                if (!isFriend) {
                    IconButton(
                        onClick = onAddFriend,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add ${player.username}",
                            tint = ElectricViolet
                        )
                    }
                }
                IconButton(
                    onClick = onCall,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video call ${player.username}",
                        tint = NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryRow(match: MatchRecord) {
    val accent = when (match.outcome) {
        "win" -> EmeraldOnline
        "loss" -> LiveCrimson
        else -> AmberBusy
    }
    val outcomeLabel = when (match.outcome) {
        "win" -> "WIN"
        "loss" -> "LOSS"
        else -> "DRAW"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = outcomeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = match.gameTitle, style = MaterialTheme.typography.titleSmall)
                val score = match.resolvedScoreDisplay
                val detail = buildString {
                    if (match.opponentName.isNotBlank()) append("vs @${match.opponentName}")
                    if (score != "—") {
                        if (isNotEmpty()) append(" • ")
                        append(score)
                    }
                }
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when (match.outcome) {
                        "win" -> "+25 MMR"
                        "loss" -> "-15 MMR"
                        else -> "0 MMR"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
                if (match.wagerPoints > 0L || match.payoutPoints > 0L) {
                    val net = match.netVirtualPoints
                    val sign = if (net > 0L) "+" else ""
                    Text(
                        text = "$sign$net PTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (net >= 0L) EmeraldOnline else LiveCrimson
                    )
                }
            }
        }
    }
}
