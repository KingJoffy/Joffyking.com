package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan

data class LogMatchInput(
    val category: GameCategory,
    val outcome: String,
    val opponentName: String,
    val scoreSummary: String,
    val myScore: Long?,
    val opponentScore: Long?,
    val matchMode: String,
    val wagerPoints: Long,
    val payoutPoints: Long,
    val notes: String
)

private val quickStakePresets = listOf(0L, 100L, 250L, 500L, 1000L)

@Composable
fun LogMatchDialog(
    friendships: List<Friendship> = emptyList(),
    currentUserId: String = "",
    onDismiss: () -> Unit,
    onSubmit: (LogMatchInput) -> Unit
) {
    var outcome by remember { mutableStateOf("win") }
    var selectedCategory by remember { mutableStateOf(GameCatalog.categories.first()) }
    var opponentName by remember { mutableStateOf("") }
    var myScoreText by remember { mutableStateOf("") }
    var opponentScoreText by remember { mutableStateOf("") }
    var customScoreNote by remember { mutableStateOf("") }
    var matchMode by remember { mutableStateOf(selectedCategory.tags.firstOrNull().orEmpty()) }
    var wagerPointsText by remember { mutableStateOf("250") }
    var payoutPointsText by remember { mutableStateOf("500") }
    var payoutManuallyEdited by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val friendNames = remember(friendships, currentUserId) {
        friendships
            .filter { it.status == "accepted" }
            .mapNotNull { friendship ->
                val name = if (friendship.requesterId == currentUserId) {
                    friendship.recipientName
                } else {
                    friendship.requesterName
                }
                name.trim().takeIf { it.isNotEmpty() }
            }
            .distinct()
            .take(8)
    }

    fun applyDefaultPayout(nextOutcome: String, stakeText: String) {
        if (payoutManuallyEdited) return
        val stake = stakeText.toLongOrNull() ?: 0L
        val suggested = when (nextOutcome) {
            "win" -> stake * 2L
            "draw" -> stake
            else -> 0L
        }
        payoutPointsText = suggested.toString()
    }

    val parsedWager = (wagerPointsText.toLongOrNull() ?: 0L).coerceIn(0L, 1_000_000L)
    val parsedPayout = (payoutPointsText.toLongOrNull() ?: 0L).coerceIn(0L, 5_000_000L)
    val previewNet = parsedPayout - parsedWager
    val myScoreParsed = myScoreText.toLongOrNull()?.coerceIn(0L, 9999L)
    val oppScoreParsed = opponentScoreText.toLongOrNull()?.coerceIn(0L, 9999L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Log Detailed Match", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Free-to-play Arena Points • No real-money wagering",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = outcome == "win",
                        onClick = {
                            outcome = "win"
                            applyDefaultPayout("win", wagerPointsText)
                        },
                        label = { Text("Win") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldOnline,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_outcome_win")
                    )
                    FilterChip(
                        selected = outcome == "loss",
                        onClick = {
                            outcome = "loss"
                            applyDefaultPayout("loss", wagerPointsText)
                        },
                        label = { Text("Loss") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiveCrimson,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_outcome_loss")
                    )
                    FilterChip(
                        selected = outcome == "draw",
                        onClick = {
                            outcome = "draw"
                            applyDefaultPayout("draw", wagerPointsText)
                        },
                        label = { Text("Draw") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberBusy,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_outcome_draw")
                    )
                }

                Text(text = "Game & Mode", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GameCatalog.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory.id == cat.id,
                            onClick = {
                                selectedCategory = cat
                                matchMode = cat.tags.firstOrNull().orEmpty()
                            },
                            label = { Text(cat.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                if (selectedCategory.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedCategory.tags, key = { it }) { tag ->
                            FilterChip(
                                selected = matchMode == tag,
                                onClick = { matchMode = if (matchMode == tag) "" else tag },
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = opponentName,
                    onValueChange = { if (it.length <= 40) opponentName = it },
                    label = { Text("Opponent Username") },
                    placeholder = { Text("e.g. ApexPhantom") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("match_opponent_input")
                )

                if (friendNames.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(friendNames, key = { it }) { friend ->
                            FilterChip(
                                selected = opponentName == friend,
                                onClick = { opponentName = friend },
                                label = { Text("@$friend") }
                            )
                        }
                    }
                }

                Text(text = "Final Score", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = myScoreText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            myScoreText = digits
                        },
                        label = { Text("Your Score") },
                        placeholder = { Text("88") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_my_score_input")
                    )
                    OutlinedTextField(
                        value = opponentScoreText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            opponentScoreText = digits
                        },
                        label = { Text("Opp Score") },
                        placeholder = { Text("81") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_opponent_score_input")
                    )
                }

                if (myScoreParsed == null || oppScoreParsed == null) {
                    OutlinedTextField(
                        value = customScoreNote,
                        onValueChange = { if (it.length <= 40) customScoreNote = it },
                        label = { Text("Or Custom Score Summary (e.g. 2-1 OT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Virtual Arena Points Challenge (PTS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberBusy
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickStakePresets, key = { it }) { preset ->
                        FilterChip(
                            selected = parsedWager == preset,
                            onClick = {
                                wagerPointsText = preset.toString()
                                applyDefaultPayout(outcome, preset.toString())
                            },
                            label = { Text(if (preset == 0L) "No Stake" else "$preset PTS") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberBusy,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = wagerPointsText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(7)
                            wagerPointsText = digits
                            applyDefaultPayout(outcome, digits)
                        },
                        label = { Text("Staked PTS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_wager_points_input")
                    )
                    OutlinedTextField(
                        value = payoutPointsText,
                        onValueChange = { raw ->
                            payoutManuallyEdited = true
                            payoutPointsText = raw.filter { it.isDigit() }.take(7)
                        },
                        label = { Text("Return PTS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("match_payout_points_input")
                    )
                }

                val netAccent = when {
                    previewNet > 0L -> EmeraldOnline
                    previewNet < 0L -> LiveCrimson
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = netAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, netAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Virtual Points:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        val sign = if (previewNet > 0L) "+" else ""
                        Text(
                            text = "$sign$previewNet PTS",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = netAccent
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 160) notes = it },
                    label = { Text("Match Review Notes (optional)") },
                    placeholder = { Text("Key plays, strategy, opponent tendencies...") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("match_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val computedScoreSummary = if (myScoreParsed != null && oppScoreParsed != null) {
                        "$myScoreParsed - $oppScoreParsed"
                    } else {
                        customScoreNote.trim()
                    }
                    onSubmit(
                        LogMatchInput(
                            category = selectedCategory,
                            outcome = outcome,
                            opponentName = opponentName.trim(),
                            scoreSummary = computedScoreSummary,
                            myScore = myScoreParsed,
                            opponentScore = oppScoreParsed,
                            matchMode = matchMode.trim(),
                            wagerPoints = parsedWager,
                            payoutPoints = parsedPayout,
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("submit_match_button")
            ) {
                Text("Save Match")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
