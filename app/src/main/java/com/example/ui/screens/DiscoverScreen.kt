package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FoodPerkOffer
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.GamerProfile
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LiveStream
import com.example.data.model.SportsDataCatalog
import com.example.data.model.UiState
import com.example.ui.components.GamerAvatar
import com.example.ui.components.LiveBadge
import com.example.ui.components.StreamCard
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan

@Composable
fun DiscoverScreen(
    myProfile: GamerProfile?,
    streamsState: UiState<List<LiveStream>>,
    friendshipsState: UiState<List<Friendship>>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    selectedGameId: String?,
    currentUserId: String,
    onSelectGame: (String?) -> Unit,
    onWatchStream: (LiveStream) -> Unit,
    onGoLiveClick: () -> Unit,
    onBrowseCategoriesClick: () -> Unit,
    onOpenSportsHub: () -> Unit,
    onOpenCategory: (GameCategory) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenMatchHistory: () -> Unit,
    onCallUser: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val acceptedFriends = (friendshipsState as? UiState.Success)?.data
        ?.filter { it.status == "accepted" }
        .orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroBroadcastCard(
                    myProfile = myProfile,
                    onGoLiveClick = onGoLiveClick,
                    onBrowseCategoriesClick = onBrowseCategoriesClick
                )
            }

            item {
                SportsArenaSpotlightCard(
                    onOpenSportsHub = onOpenSportsHub,
                    onOpenNba2k27 = { onOpenCategory(GameCatalog.findById("nba_2k27")) },
                    onOpenMadden27 = { onOpenCategory(GameCatalog.findById("madden_27")) }
                )
            }

            if (acceptedFriends.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quick Video Call Squad",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(acceptedFriends, key = { it.id }) { friendship ->
                                val isRequester = friendship.requesterId == currentUserId
                                val friendId = if (isRequester) friendship.recipientId else friendship.requesterId
                                val friendName = if (isRequester) friendship.recipientName else friendship.requesterName
                                val friendAvatar = if (isRequester) friendship.recipientAvatarData else friendship.requesterAvatarData
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clickable {
                                            onCallUser(friendId, friendName, friendAvatar, "Squad Call")
                                        }
                                        .testTag("quick_call_friend_$friendId")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GamerAvatar(
                                            avatarData = friendAvatar,
                                            username = friendName,
                                            size = 38.dp,
                                            onlineStatus = "online"
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = friendName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Tap to call",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeonCyan
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = "Call $friendName",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                TopRankedSpotlightCard(
                    leaderboardState = leaderboardState,
                    currentUserId = currentUserId,
                    onOpenLeaderboard = onOpenLeaderboard,
                    onOpenMatchHistory = onOpenMatchHistory
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browse by Game",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onBrowseCategoriesClick) {
                            Text("All Games", color = NeonCyan)
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGameId == null,
                                onClick = { onSelectGame(null) },
                                label = { Text("All Live") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(GameCatalog.categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedGameId == cat.id,
                                onClick = { onSelectGame(if (selectedGameId == cat.id) null else cat.id) },
                                label = { Text(cat.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            when (streamsState) {
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
                            color = LiveCrimson.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = streamsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val filtered = if (selectedGameId == null) {
                        streamsState.data
                    } else {
                        streamsState.data.filter { it.gameId == selectedGameId }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedGameId == null) "Live Worldwide" else "${GameCatalog.findById(selectedGameId).title} Live",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${filtered.size} LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = LiveCrimson
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyStreamsCard(
                                gameTitle = selectedGameId?.let { GameCatalog.findById(it).title },
                                onGoLiveClick = onGoLiveClick
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { stream ->
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
                                } else null,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // NexusCast Web Food Discounts & Gamer Fuel Perks
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "NexusCast Web Food Perks",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(GameCatalog.webFoodPerks, key = { it.id }) { perk ->
                            FoodPerkCard(
                                perk = perk,
                                onCopyCode = {
                                    clipboard.setText(AnnotatedString(perk.promoCode))
                                    onShowMessage("Copied web food code ${perk.promoCode}!")
                                },
                                onOpenWebsite = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(perk.websiteUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBroadcastCard(
    myProfile: GamerProfile?,
    onGoLiveClick: () -> Unit,
    onBrowseCategoriesClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_arena),
                contentDescription = "Broadcast studio banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xEE0A0B14),
                                Color(0xCC0A0B14),
                                Color(0x660A0B14)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveBadge()
                    Text(
                        text = if (myProfile != null) "@${myProfile.username}" else "NEXUSCAST",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                }

                Column {
                    Text(
                        text = "Stream Anywhere.\nVideo Call Your Squad.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onGoLiveClick,
                            colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("hero_go_live_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Go Live")
                        }
                        OutlinedButton(
                            onClick = onBrowseCategoriesClick,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Games", color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStreamsCard(
    gameTitle: String?,
    onGoLiveClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (gameTitle != null) "No live $gameTitle streams right now" else "No live broadcasts right now",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Be the first gamer to go live and invite friends to join your broadcast or video call!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onGoLiveClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("empty_go_live_button")
            ) {
                Text("Start Streaming")
            }
        }
    }
}

@Composable
private fun FoodPerkCard(
    perk: FoodPerkOffer,
    onCopyCode: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    Surface(
        modifier = Modifier.width(280.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LiveCrimson.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = perk.discountBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = LiveCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = perk.partnerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = perk.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = perk.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onCopyCode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = perk.promoCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy promo code ${perk.promoCode}",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onOpenWebsite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricViolet)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Redeem food discount on NexusCast website",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TopRankedSpotlightCard(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    onOpenLeaderboard: () -> Unit,
    onOpenMatchHistory: () -> Unit
) {
    val topPlayers = (leaderboardState as? UiState.Success)?.data.orEmpty().take(3)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onOpenLeaderboard)
            .testTag("discover_leaderboard_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AmberBusy.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        tint = AmberBusy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Top Ranked Players",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenMatchHistory) {
                        Text("History", color = AmberBusy)
                    }
                    TextButton(onClick = onOpenLeaderboard) {
                        Text("Ranks ->", color = NeonCyan)
                    }
                }
            }

            if (topPlayers.isEmpty()) {
                Text(
                    text = "Log match outcomes, scores & Arena Points to track performance and climb the #1 global rank!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topPlayers.forEachIndexed { idx, player ->
                        val rankColor = when (idx) {
                            0 -> Color(0xFFFBBF24)
                            1 -> Color(0xFFCBD5E1)
                            else -> Color(0xFFD97706)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = rankColor,
                                modifier = Modifier.width(28.dp)
                            )
                            GamerAvatar(
                                avatarData = player.avatarData,
                                username = player.username,
                                size = 32.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (player.userId == currentUserId) "${player.username} (You)" else player.username,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${player.wins}W - ${player.losses}L • ${player.winRatePercent}% WR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldOnline.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${player.rating} MMR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = rankColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportsArenaSpotlightCard(
    onOpenSportsHub: () -> Unit,
    onOpenNba2k27: () -> Unit,
    onOpenMadden27: () -> Unit
) {
    val liveHighlightScores = SportsDataCatalog.scoreboards.take(2)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onOpenSportsHub)
            .testTag("discover_sports_hub_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = AmberBusy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sports Arena • 2K27 & Madden 27",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = onOpenSportsHub) {
                    Text("Sports Hub ->", color = NeonCyan)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFFF6B00).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenNba2k27)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "NBA 2K27",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF8C38)
                        )
                        Text(
                            text = "Pro-AM • Stats • Reels",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = EmeraldOnline.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldOnline.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenMadden27)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Madden NFL 27",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldOnline
                        )
                        Text(
                            text = "MUT 27 • H2H • Box Scores",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            liveHighlightScores.forEach { score ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${score.league}: ${score.awayShort} ${score.awayScore} - ${score.homeShort} ${score.homeScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        text = score.clockStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = LiveCrimson
                    )
                }
            }
        }
    }
}

