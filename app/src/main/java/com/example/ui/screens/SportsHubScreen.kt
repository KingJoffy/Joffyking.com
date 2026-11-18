package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.LiveStream
import com.example.data.model.ProHighlightSpotlight
import com.example.data.model.SportHighlight
import com.example.data.model.SportsDataCatalog
import com.example.data.model.UiState
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan

private enum class SportsHubTab(val label: String) {
    HIGHLIGHTS("Highlights"),
    SCOREBOARDS("Live Scores"),
    STATS_DATA("Stats & Standings"),
    SPORTS_GAMES("All Sports")
}

private val playTypeOptions = listOf(
    "Buzzer Beater",
    "Poster Dunk",
    "Clutch TD",
    "Pick-Six",
    "Top Bins",
    "Walk-Off HR",
    "Knockout"
)

@Composable
fun SportsHubScreen(
    streamsState: UiState<List<LiveStream>>,
    highlightsState: UiState<List<SportHighlight>>,
    currentUserId: String,
    onWatchCategory: (GameCategory) -> Unit,
    onGoLiveInCategory: (GameCategory) -> Unit,
    onPublishHighlight: (
        category: GameCategory,
        sportLeague: String,
        title: String,
        playType: String,
        statLine: String,
        quarterOrTime: String,
        description: String
    ) -> Unit,
    onToggleHype: (SportHighlight) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onCallPlayer: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit
) {
    var activeTab by remember { mutableStateOf(SportsHubTab.HIGHLIGHTS) }
    var selectedLeague by remember { mutableStateOf("ALL") }
    var selectedStatsLeague by remember { mutableStateOf("NBA") }
    var activeSpotlight by remember { mutableStateOf<ProHighlightSpotlight?>(null) }
    var showPostDialog by remember { mutableStateOf(false) }

    val liveStreams = (streamsState as? UiState.Success)?.data.orEmpty()
    val nba2k27 = remember { GameCatalog.findById("nba_2k27") }
    val madden27 = remember { GameCatalog.findById("madden_27") }
    val communityHighlights = (highlightsState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsBasketball,
                                contentDescription = null,
                                tint = AmberBusy,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Sports Arena & Data",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Text(
                            text = "NBA 2K27 • Madden NFL 27 • Live Stats & Highlights",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                    Button(
                        onClick = { showPostDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("post_highlight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Highlight")
                    }
                }
            }

            // Big Featured Showcase: NBA 2K27 & Madden NFL 27
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Headliners",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            FeaturedSportsCard(
                                category = nba2k27,
                                subtitle = "Pro-AM • Rec 5v5 • MyCAREER • Live Shot Data",
                                liveCount = liveStreams.count { it.gameId == nba2k27.id },
                                onWatchCategory = { onWatchCategory(nba2k27) },
                                onGoLive = { onGoLiveInCategory(nba2k27) },
                                modifier = Modifier.width(292.dp)
                            )
                        }
                        item {
                            FeaturedSportsCard(
                                category = madden27,
                                subtitle = "MUT 27 • Franchise • H2H Ranked • QB Telemetry",
                                liveCount = liveStreams.count { it.gameId == madden27.id },
                                onWatchCategory = { onWatchCategory(madden27) },
                                onGoLive = { onGoLiveInCategory(madden27) },
                                modifier = Modifier.width(292.dp)
                            )
                        }
                    }
                }
            }

            // Hub Section Switcher Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SportsHubTab.entries) { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(tab.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("sports_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            when (activeTab) {
                SportsHubTab.HIGHLIGHTS -> {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Featured 2K27 & Madden 27 Reels",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberBusy,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(SportsDataCatalog.officialSpotlights, key = { it.id }) { spotlight ->
                                    OfficialHighlightSpotlightCard(
                                        spotlight = spotlight,
                                        onWatchReel = { activeSpotlight = spotlight }
                                    )
                                }
                            }
                        }
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
                                text = "Community Highlights",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                            TextButton(onClick = { showPostDialog = true }) {
                                Text("+ Share Your Play", color = NeonCyan)
                            }
                        }
                    }

                    when (highlightsState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
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
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = highlightsState.message,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            if (communityHighlights.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No community sports clips posted yet",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "Drop your best NBA 2K27 buzzer beater or Madden 27 clutch TD!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedButton(onClick = { showPostDialog = true }) {
                                                Text("Post First Highlight", color = NeonCyan)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(communityHighlights, key = { it.id }) { highlight ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        CommunityHighlightCard(
                                            highlight = highlight,
                                            currentUserId = currentUserId,
                                            onToggleHype = { onToggleHype(highlight) },
                                            onCallAuthor = {
                                                onCallPlayer(
                                                    highlight.authorId,
                                                    highlight.authorUsername,
                                                    highlight.authorAvatarData,
                                                    highlight.gameTitle
                                                )
                                            },
                                            onDeleteOwn = { onDeleteHighlight(highlight.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SportsHubTab.SCOREBOARDS -> {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(SportsDataCatalog.leagues) { league ->
                                FilterChip(
                                    selected = selectedLeague == league,
                                    onClick = { selectedLeague = league },
                                    label = { Text(league) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    val filteredScores = SportsDataCatalog.scoreboards.filter {
                        selectedLeague == "ALL" || it.league == selectedLeague
                    }
                    items(filteredScores, key = { it.id }) { matchup ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ScoreboardMatchupCard(
                                matchup = matchup,
                                onStreamGame = {
                                    onWatchCategory(GameCatalog.findById(matchup.gameId))
                                }
                            )
                        }
                    }
                }

                SportsHubTab.STATS_DATA -> {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(SportsDataCatalog.leagues.filter { it != "ALL" }) { league ->
                                FilterChip(
                                    selected = selectedStatsLeague == league,
                                    onClick = { selectedStatsLeague = league },
                                    label = { Text(league) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberBusy,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    val leaders = SportsDataCatalog.statLeaders.filter { it.league == selectedStatsLeague }
                    if (leaders.isNotEmpty()) {
                        item {
                            Text(
                                text = "$selectedStatsLeague Player Stat Leaders & OVR Ratings",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberBusy,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(leaders, key = { "${it.league}_${it.playerName}" }) { leader ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StatLeaderCard(leader = leader)
                            }
                        }
                    }

                    val standingRows = SportsDataCatalog.standings.filter { it.league == selectedStatsLeague }
                    if (standingRows.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StandingsTableCard(
                                    league = selectedStatsLeague,
                                    rows = standingRows
                                )
                            }
                        }
                    }
                }

                SportsHubTab.SPORTS_GAMES -> {
                    items(GameCatalog.sportsCategories, key = { it.id }) { category ->
                        val count = liveStreams.count { it.gameId == category.id }
                        SportsGameDirectoryRow(
                            category = category,
                            liveCount = count,
                            onWatch = { onWatchCategory(category) },
                            onGoLive = { onGoLiveInCategory(category) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    activeSpotlight?.let { spotlight ->
        HighlightTheaterDialog(
            spotlight = spotlight,
            onDismiss = { activeSpotlight = null },
            onGoLiveInGame = {
                activeSpotlight = null
                onGoLiveInCategory(GameCatalog.findById(spotlight.gameId))
            }
        )
    }

    if (showPostDialog) {
        PostHighlightDialog(
            onDismiss = { showPostDialog = false },
            onSubmit = { cat, league, title, playType, statLine, quarter, desc ->
                showPostDialog = false
                onPublishHighlight(cat, league, title, playType, statLine, quarter, desc)
            }
        )
    }
}

@Composable
private fun SportsGameDirectoryRow(
    category: GameCategory,
    liveCount: Int,
    onWatch: () -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(category.accentHex)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onWatch),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = category.coverRes),
                    contentDescription = category.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category.genre} • ${category.tags.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (liveCount > 0) "$liveCount Live Broadcasts" else "Ready to Stream",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (liveCount > 0) LiveCrimson else NeonCyan
                )
            }
            Button(
                onClick = onGoLive,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Live", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PostHighlightDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        category: GameCategory,
        sportLeague: String,
        title: String,
        playType: String,
        statLine: String,
        quarterOrTime: String,
        description: String
    ) -> Unit
) {
    val sportsGames = remember { GameCatalog.sportsCategories }
    var selectedGame by remember { mutableStateOf(sportsGames.first()) }
    var selectedPlayType by remember { mutableStateOf(playTypeOptions.first()) }
    var title by remember { mutableStateOf("") }
    var statLine by remember { mutableStateOf("") }
    var quarterClock by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Post Sports Highlight", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sports Game", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sportsGames, key = { it.id }) { game ->
                        FilterChip(
                            selected = selectedGame.id == game.id,
                            onClick = { selectedGame = game },
                            label = { Text(game.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text("Play Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playTypeOptions) { type ->
                        FilterChip(
                            selected = selectedPlayType == type,
                            onClick = { selectedPlayType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberBusy,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Highlight Title *") },
                    placeholder = { Text("e.g., Green 3PT Fadeaway to Win Rec!") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("highlight_title_input")
                )
                OutlinedTextField(
                    value = statLine,
                    onValueChange = { if (it.length <= 80) statLine = it },
                    label = { Text("Stat Line (optional)") },
                    placeholder = { Text("e.g., 42 PTS · 11 AST · 8 3PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quarterClock,
                    onValueChange = { if (it.length <= 30) quarterClock = it },
                    label = { Text("Quarter / Clock (optional)") },
                    placeholder = { Text("e.g., Q4 · 0:00.4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text("Play Breakdown (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val league = when (selectedGame.id) {
                        "nba_2k27" -> "NBA"
                        "madden_27" -> "NFL"
                        "ea_fc_27" -> "SOCCER"
                        "college_football_27" -> "CFB"
                        "mlb_the_show_27" -> "MLB"
                        "nhl_27" -> "NHL"
                        "ufc_6" -> "UFC"
                        else -> "SPORTS"
                    }
                    onSubmit(
                        selectedGame,
                        league,
                        title.trim(),
                        selectedPlayType,
                        statLine,
                        quarterClock,
                        description
                    )
                },
                enabled = title.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("submit_highlight_button")
            ) {
                Text("Publish Highlight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
