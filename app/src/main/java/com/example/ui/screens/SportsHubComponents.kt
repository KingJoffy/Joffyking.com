package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCategory
import com.example.data.model.ProHighlightSpotlight
import com.example.data.model.SportHighlight
import com.example.data.model.SportMatchupScore
import com.example.data.model.SportStandingRow
import com.example.data.model.SportStatLeader
import com.example.ui.components.GamerAvatar
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay

@Composable
fun FeaturedSportsCard(
    category: GameCategory,
    subtitle: String,
    liveCount: Int,
    onWatchCategory: () -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(category.accentHex)
    Card(
        modifier = modifier
            .height(210.dp)
            .testTag("featured_sport_${category.id}")
            .clickable(onClick = onWatchCategory),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                            listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (category.id == "nba_2k27") {
                                Icons.Default.SportsBasketball
                            } else {
                                Icons.Default.SportsFootball
                            },
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = category.genre.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
                Surface(
                    color = if (liveCount > 0) LiveCrimson else Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (liveCount > 0) "$liveCount LIVE" else "FEATURED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGoLive,
                        colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Go Live", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onWatchCategory,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.85f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Streams", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialHighlightSpotlightCard(
    spotlight: ProHighlightSpotlight,
    onWatchReel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(305.dp)
            .clickable(onClick = onWatchReel)
            .testTag("spotlight_reel_${spotlight.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.55f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Image(
                    painter = painterResource(id = spotlight.coverRes),
                    contentDescription = spotlight.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )
                Surface(
                    color = LiveCrimson,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${spotlight.gameTitle} • ${spotlight.playType}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play highlight breakdown",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = spotlight.quarterClock,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = spotlight.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = spotlight.playerAndTeam,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = spotlight.statLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HighlightTheaterDialog(
    spotlight: ProHighlightSpotlight,
    onDismiss: () -> Unit,
    onGoLiveInGame: () -> Unit
) {
    var activeStep by remember(spotlight.id) { mutableIntStateOf(0) }
    LaunchedEffect(spotlight.id) {
        activeStep = 0
        for (i in 1 until spotlight.playByPlaySteps.size) {
            delay(1700L)
            activeStep = i
        }
    }

    val progress by animateFloatAsState(
        targetValue = (activeStep + 1).toFloat() / spotlight.playByPlaySteps.size.coerceAtLeast(1),
        label = "reel_progress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${spotlight.gameTitle} • ${spotlight.playType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = spotlight.title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    Image(
                        painter = painterResource(id = spotlight.coverRes),
                        contentDescription = spotlight.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = spotlight.playerAndTeam,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Text(
                            text = spotlight.peakTelemetry,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberBusy
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    color = NeonCyan,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )

                spotlight.playByPlaySteps.forEachIndexed { idx, step ->
                    val highlighted = idx <= activeStep
                    Surface(
                        color = if (idx == activeStep) ElectricViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (idx == activeStep) BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)) else null
                    ) {
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoLiveInGame,
                colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson)
            ) {
                Text("Stream ${spotlight.gameTitle}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CommunityHighlightCard(
    highlight: SportHighlight,
    currentUserId: String,
    onToggleHype: () -> Unit,
    onCallAuthor: () -> Unit,
    onDeleteOwn: () -> Unit
) {
    val isMine = highlight.authorId == currentUserId
    val isHyped = highlight.hypedByUids.contains(currentUserId)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_highlight_${highlight.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isHyped) AmberBusy.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamerAvatar(
                    avatarData = highlight.authorAvatarData,
                    username = highlight.authorUsername,
                    size = 40.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = highlight.authorUsername,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${highlight.gameTitle} • ${highlight.playType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
                if (highlight.quarterOrTime.isNotBlank()) {
                    Surface(
                        color = ElectricViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = highlight.quarterOrTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (highlight.statLine.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = highlight.statLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberBusy
                )
            }
            if (highlight.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleHype,
                    border = BorderStroke(
                        1.dp,
                        if (isHyped) AmberBusy else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("hype_highlight_${highlight.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Hype highlight",
                        tint = if (isHyped) AmberBusy else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${highlight.hypeCount} Hype",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isHyped) AmberBusy else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isMine) {
                        IconButton(
                            onClick = onCallAuthor,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video call ${highlight.authorUsername}",
                                tint = NeonCyan
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDeleteOwn,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete highlight",
                                tint = LiveCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreboardMatchupCard(
    matchup: SportMatchupScore,
    onStreamGame: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (matchup.liveNow) LiveCrimson.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricViolet.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = matchup.league,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = if (matchup.liveNow) "● ${matchup.clockStatus}" else matchup.clockStatus,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (matchup.liveNow) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = matchup.awayTeam, style = MaterialTheme.typography.titleSmall)
                    Text(text = matchup.homeTeam, style = MaterialTheme.typography.titleSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${matchup.awayScore}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (matchup.awayScore >= matchup.homeScore) NeonCyan else Color.White
                    )
                    Text(
                        text = "${matchup.homeScore}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (matchup.homeScore > matchup.awayScore) NeonCyan else Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = matchup.keyPerformers,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (matchup.liveNow && matchup.homeWinProbability in 1..99) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${matchup.awayShort} ${100 - matchup.homeWinProbability}% Win Prob",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricViolet
                    )
                    Text(
                        text = "${matchup.homeShort} ${matchup.homeWinProbability}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (100 - matchup.homeWinProbability) / 100f },
                    color = ElectricViolet,
                    trackColor = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onStreamGame) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Stream / Watch Companion", color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun StatLeaderCard(leader: SportStatLeader) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AmberBusy.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberBusy.copy(alpha = 0.45f))
            ) {
                Text(
                    text = leader.ratingBadge,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AmberBusy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${leader.playerName} • ${leader.team} (${leader.position})",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = leader.categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Text(
                    text = leader.breakdown,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = EmeraldOnline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = leader.primaryValue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldOnline
                )
            }
        }
    }
}

@Composable
fun StandingsTableCard(
    league: String,
    rows: List<SportStandingRow>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$league Standings & Ratings",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )
            Spacer(Modifier.height(10.dp))
            rows.forEachIndexed { idx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${row.rank}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (row.rank == 1) AmberBusy else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.team,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${row.conferenceOrDivision} • ${row.extraStat} • ${row.diffOrRating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${row.wins}-${row.losses}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        color = if (row.streak.startsWith("W")) EmeraldOnline.copy(alpha = 0.16f) else LiveCrimson.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = row.streak,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (row.streak.startsWith("W")) EmeraldOnline else LiveCrimson,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}
