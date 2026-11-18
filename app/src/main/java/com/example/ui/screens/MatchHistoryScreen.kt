package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.MatchPerformanceSummary
import com.example.data.model.MatchRecord
import com.example.data.model.UiState
import com.example.data.model.computeMatchPerformanceSummary
import com.example.ui.components.LogMatchDialog
import com.example.ui.components.LogMatchInput
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import java.text.SimpleDateFormat
import java.util.Locale

private enum class MatchFilterOption(val label: String) {
    ALL("All"),
    WINS("Wins"),
    LOSSES("Losses"),
    DRAWS("Draws"),
    POINTS_STAKED("Points Staked")
}

@Composable
fun MatchHistoryScreen(
    matchHistoryState: UiState<List<MatchRecord>>,
    friendshipsState: UiState<List<Friendship>>,
    currentUserId: String,
    onRecordMatch: (LogMatchInput) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var outcomeFilter by remember { mutableStateOf(MatchFilterOption.ALL) }
    var selectedGameTitle by remember { mutableStateOf<String?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var inspectingMatch by remember { mutableStateOf<MatchRecord?>(null) }

    val records = (matchHistoryState as? UiState.Success)?.data.orEmpty()
    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()

    val playedGames = remember(records) {
        records.map { it.gameTitle }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredRecords = remember(records, searchQuery, outcomeFilter, selectedGameTitle) {
        val q = searchQuery.trim().lowercase()
        records.filter { rec ->
            val matchesOutcome = when (outcomeFilter) {
                MatchFilterOption.ALL -> true
                MatchFilterOption.WINS -> rec.outcome == "win"
                MatchFilterOption.LOSSES -> rec.outcome == "loss"
                MatchFilterOption.DRAWS -> rec.outcome == "draw"
                MatchFilterOption.POINTS_STAKED -> rec.wagerPoints > 0L || rec.payoutPoints > 0L
            }
            val matchesGame = selectedGameTitle == null ||
                rec.gameTitle.equals(selectedGameTitle, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                rec.opponentName.lowercase().contains(q) ||
                rec.gameTitle.lowercase().contains(q) ||
                rec.matchMode.lowercase().contains(q) ||
                rec.notes.lowercase().contains(q) ||
                rec.resolvedScoreDisplay.lowercase().contains(q)
            matchesOutcome && matchesGame && matchesQuery
        }
    }

    val summary = remember(filteredRecords) {
        computeMatchPerformanceSummary(filteredRecords)
    }

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
                                .testTag("match_history_back_button")
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
                                text = "Match History",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Outcomes, opponents, scores & Arena Points",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { showLogDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("history_log_match_button")
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

            // Virtual points compliance notice
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = NeonCyan.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Free-to-Play Virtual Arena Points Performance Tracker (No Real-Money Betting)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Performance Analytics Card + Equity Curve
            item {
                PerformanceAnalyticsCard(
                    summary = summary,
                    onOpenLeaderboard = onOpenLeaderboard
                )
            }

            // Search opponent / game / notes
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search opponent username, game, mode, or score...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("match_history_search_input")
                )
            }

            // Outcome filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MatchFilterOption.entries, key = { it.name }) { option ->
                        FilterChip(
                            selected = outcomeFilter == option,
                            onClick = { outcomeFilter = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("history_filter_${option.name.lowercase()}")
                        )
                    }
                }
            }

            // Game title filters (shown once matches exist across games)
            if (playedGames.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGameTitle == null,
                                onClick = { selectedGameTitle = null },
                                label = { Text("All Titles") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.24f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                        items(playedGames, key = { it }) { title ->
                            FilterChip(
                                selected = selectedGameTitle == title,
                                onClick = {
                                    selectedGameTitle = if (selectedGameTitle == title) null else title
                                },
                                label = { Text(title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.24f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Matches (${filteredRecords.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (summary.averageMargin != null) {
                        val sign = if (summary.averageMargin >= 0.0) "+" else ""
                        Text(
                            text = "Avg Margin: $sign${String.format(Locale.US, "%.1f", summary.averageMargin)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                }
            }

            when (matchHistoryState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
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
                                text = matchHistoryState.message,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (records.isEmpty()) {
                        item {
                            EmptyMatchHistoryCard(
                                title = "No Matches Logged Yet",
                                body = "Record match outcomes, opponent usernames, final scores, and Arena Points challenges to track your win rate, streaks, and net performance.",
                                actionLabel = "Log First Match",
                                onAction = { showLogDialog = true }
                            )
                        }
                    } else if (filteredRecords.isEmpty()) {
                        item {
                            EmptyMatchHistoryCard(
                                title = "No Matches Match Filter",
                                body = "Try clearing your opponent search or switching the outcome/game filter.",
                                actionLabel = "Reset Filters",
                                onAction = {
                                    searchQuery = ""
                                    outcomeFilter = MatchFilterOption.ALL
                                    selectedGameTitle = null
                                }
                            )
                        }
                    } else {
                        items(filteredRecords, key = { it.id }) { match ->
                            DetailedMatchCard(
                                match = match,
                                onClick = { inspectingMatch = match }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        LogMatchDialog(
            friendships = friendships,
            currentUserId = currentUserId,
            onDismiss = { showLogDialog = false },
            onSubmit = { input ->
                showLogDialog = false
                onRecordMatch(input)
            }
        )
    }

    inspectingMatch?.let { match ->
        MatchDetailInspectionDialog(
            match = match,
            onDismiss = { inspectingMatch = null },
            onDelete = {
                inspectingMatch = null
                onDeleteMatch(match.id)
            }
        )
    }
}

@Composable
private fun PerformanceAnalyticsCard(
    summary: MatchPerformanceSummary,
    onOpenLeaderboard: () -> Unit
) {
    val netPositive = summary.netPoints >= 0L
    val netColor = when {
        summary.netPoints > 0L -> EmeraldOnline
        summary.netPoints < 0L -> LiveCrimson
        else -> NeonCyan
    }
    val roiSign = if (summary.roiPercent > 0) "+" else ""
    val netSign = if (summary.netPoints > 0L) "+" else ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_performance_summary_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, netColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ElectricViolet.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NET ARENA PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (netPositive) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = netColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$netSign${summary.netPoints} PTS",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = netColor
                        )
                    }
                }
                Surface(
                    color = netColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, netColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$roiSign${summary.roiPercent}% ROI",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = netColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    label = "RECORD",
                    value = "${summary.wins}W-${summary.losses}L-${summary.draws}D",
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = "WIN RATE",
                    value = "${summary.winRatePercent}%",
                    accent = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = "STREAK",
                    value = "${summary.currentStreak} (Best ${summary.bestWinStreak})",
                    accent = AmberBusy,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    label = "STAKED",
                    value = "${summary.totalStakedPoints} PTS",
                    accent = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = "RETURNED",
                    value = "${summary.totalPayoutPoints} PTS",
                    accent = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = "BEST WIN",
                    value = "+${summary.biggestWinNetPoints} PTS",
                    accent = ElectricViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cumulative Virtual Points Equity Curve
            PerformanceTrendChart(
                curve = summary.cumulativeNetCurve,
                lineColor = netColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenLeaderboard) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberBusy,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("View Global Rankings", color = AmberBusy)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PerformanceTrendChart(
    curve: List<Long>,
    lineColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0F1F).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUMULATIVE ARENA POINTS TREND",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (curve.size >= 2) "${curve.size} matches" else "Log 2+ matches for curve",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
            Spacer(Modifier.height(8.dp))
            if (curve.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your net Arena Points curve will chart here as matches accumulate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                ) {
                    val minVal = minOf(0L, curve.minOrNull() ?: 0L).toFloat()
                    val maxVal = maxOf(100L, curve.maxOrNull() ?: 100L).toFloat()
                    val span = (maxVal - minVal).coerceAtLeast(1f)
                    val padH = 10.dp.toPx()
                    val padV = 10.dp.toPx()
                    val chartW = (size.width - padH * 2f).coerceAtLeast(1f)
                    val chartH = (size.height - padV * 2f).coerceAtLeast(1f)

                    fun yFor(value: Float): Float {
                        val norm = (value - minVal) / span
                        return size.height - padV - norm * chartH
                    }

                    // Dashed zero baseline
                    val zeroY = yFor(0f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(padH, zeroY),
                        end = Offset(size.width - padH, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    val stepX = chartW / (curve.size - 1).coerceAtLeast(1)
                    val points = curve.mapIndexed { index, value ->
                        Offset(padH + index * stepX, yFor(value.toFloat()))
                    }

                    val linePath = Path().apply {
                        points.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, zeroY)
                        lineTo(points.first().x, zeroY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.34f),
                                lineColor.copy(alpha = 0.02f)
                            )
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    points.forEachIndexed { index, pt ->
                        val nodeColor = if (curve[index] >= 0L) EmeraldOnline else LiveCrimson
                        drawCircle(
                            color = nodeColor,
                            radius = if (index == points.lastIndex) 5.dp.toPx() else 3.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedMatchCard(
    match: MatchRecord,
    onClick: () -> Unit
) {
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
    val dateLabel = remember(match.createdAt) {
        val date = match.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(date)
        } else {
            "Just logged"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("match_card_${match.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Outcome + Game + Mode + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = outcomeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = match.gameTitle,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (match.matchMode.isNotBlank()) {
                            Text(
                                text = match.matchMode,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Middle Row: Opponent Username & Final Scoreboard
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val opponentLabel = match.opponentName.ifBlank { "Ranked Opponent" }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opponentLabel.take(2).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OPPONENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (match.opponentName.isNotBlank()) "@${match.opponentName}" else "Open Lobby",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "FINAL SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = match.resolvedScoreDisplay,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            match.scoreMargin?.let { margin ->
                                val marginSign = if (margin > 0L) "+" else ""
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "($marginSign$margin)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Row: Virtual Points Staked / Returned / Net + Notes
            val hasPoints = match.wagerPoints > 0L || match.payoutPoints > 0L
            if (hasPoints) {
                val net = match.netVirtualPoints
                val netAccent = when {
                    net > 0L -> EmeraldOnline
                    net < 0L -> LiveCrimson
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val netSign = if (net > 0L) "+" else ""
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stake: ${match.wagerPoints} PTS • Return: ${match.payoutPoints} PTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = netAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$netSign$net PTS (${match.matchRoiPercent}%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = netAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (match.notes.isNotBlank()) {
                Text(
                    text = "\"${match.notes}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyMatchHistoryCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SportsScore,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(38.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(actionLabel, color = NeonCyan)
            }
        }
    }
}

@Composable
private fun MatchDetailInspectionDialog(
    match: MatchRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = when (match.outcome) {
        "win" -> EmeraldOnline
        "loss" -> LiveCrimson
        else -> AmberBusy
    }
    val dateLabel = remember(match.createdAt) {
        val date = match.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(date)
        } else {
            "Logged just now"
        }
    }
    val net = match.netVirtualPoints
    val netSign = if (net > 0L) "+" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(match.gameTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = match.outcome.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Opponent", match.opponentName.ifBlank { "Open Lobby" })
                DetailRow("Final Score", match.resolvedScoreDisplay)
                if (match.matchMode.isNotBlank()) {
                    DetailRow("Mode", match.matchMode)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                DetailRow("Staked Points", "${match.wagerPoints} PTS")
                DetailRow("Returned Points", "${match.payoutPoints} PTS")
                DetailRow("Net Performance", "$netSign$net PTS (${match.matchRoiPercent}% ROI)")
                if (match.notes.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = match.notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_match_record_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete match record",
                    tint = LiveCrimson,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Delete Log", color = LiveCrimson)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
