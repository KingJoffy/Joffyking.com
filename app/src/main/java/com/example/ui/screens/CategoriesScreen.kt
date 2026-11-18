package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GameCatalog
import com.example.data.model.GameCategory
import com.example.data.model.LiveStream
import com.example.data.model.UiState
import com.example.ui.components.StreamCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan

@Composable
fun CategoriesScreen(
    streamsState: UiState<List<LiveStream>>,
    currentUserId: String,
    initialGameId: String? = null,
    onWatchStream: (LiveStream) -> Unit,
    onGoLiveInCategory: (GameCategory) -> Unit,
    onCallUser: (userId: String, username: String, avatarData: String, gameContext: String) -> Unit
) {
    var activeCategory by remember(initialGameId) {
        mutableStateOf(initialGameId?.let { GameCatalog.findById(it) })
    }
    var categoryFilter by remember { mutableStateOf("ALL") }
    val liveStreams = (streamsState as? UiState.Success)?.data.orEmpty()

    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    val displayedCategories = remember(categoryFilter) {
        when (categoryFilter) {
            "SPORTS" -> GameCatalog.categories.filter { it.isSports }
            "ESPORTS" -> GameCatalog.categories.filter { !it.isSports }
            else -> GameCatalog.categories
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (activeCategory == null) {
            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Game Categories",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Browse Sports (NBA 2K27, Madden 27) & top gaming titles to stream or watch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoryFilter == "ALL",
                            onClick = { categoryFilter = "ALL" },
                            label = { Text("All Games (${GameCatalog.categories.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = categoryFilter == "SPORTS",
                            onClick = { categoryFilter = "SPORTS" },
                            label = { Text("Sports Games (${GameCatalog.sportsCategories.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_sports_categories")
                        )
                    }
                    item {
                        FilterChip(
                            selected = categoryFilter == "ESPORTS",
                            onClick = { categoryFilter = "ESPORTS" },
                            label = { Text("FPS & RPG") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 164.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayedCategories, key = { it.id }) { category ->
                        val count = liveStreams.count { it.gameId == category.id }
                        GameCategoryCard(
                            category = category,
                            liveCount = count,
                            onClick = { activeCategory = category }
                        )
                    }
                }
            }
        } else {
            val cat = activeCategory!!
            val matchingStreams = liveStreams.filter { it.gameId == cat.id }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeCategory = null },
                            modifier = Modifier.testTag("category_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to all game categories"
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${cat.genre} • ${matchingStreams.size} live streams",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                        Button(
                            onClick = { onGoLiveInCategory(cat) },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                            modifier = Modifier.testTag("stream_category_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Stream")
                        }
                    }
                }

                if (matchingStreams.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No one is live in ${cat.title} yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Tap Stream above to launch the first ${cat.title} broadcast!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(matchingStreams, key = { it.id }) { stream ->
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
private fun GameCategoryCard(
    category: GameCategory,
    liveCount: Int,
    onClick: () -> Unit
) {
    val accent = Color(category.accentHex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .testTag("category_card_${category.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
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
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            if (liveCount > 0) {
                Surface(
                    color = LiveCrimson,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "$liveCount LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Surface(
                    color = ElectricViolet.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = category.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
