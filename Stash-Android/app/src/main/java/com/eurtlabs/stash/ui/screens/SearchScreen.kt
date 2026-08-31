package com.eurtlabs.stash.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eurtlabs.stash.data.model.SearchFilter
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.player.MusicPlayerManager
import com.eurtlabs.stash.ui.components.AudioEqualizerVisualizer
import com.eurtlabs.stash.ui.components.SearchInputBar
import com.eurtlabs.stash.ui.theme.LocalStashPalette

@Composable
fun SearchScreen(
    isSearching: Boolean,
    searchResults: List<SearchResultItem>,
    selectedFilter: SearchFilter,
    activeDownloadsMap: Map<String, Float> = emptyMap(),
    downloadedTrackIds: Set<String> = emptySet(),
    onFilterChanged: (SearchFilter) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit = {},
    onPlayTrack: (SearchResultItem, List<SearchResultItem>) -> Unit = { _, _ -> },
    onDownloadItem: (SearchResultItem) -> Unit,
    onDownloadAll: (List<SearchResultItem>, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val context = LocalContext.current
    val playerState by MusicPlayerManager.playerState.collectAsState()

    val quickSearches = listOf(
        "The Weeknd",
        "Daft Punk",
        "Coldplay",
        "Billie Eilish",
        "Taylor Swift",
        "Travis Scott",
        "Eminem",
        "Lofi Hip Hop"
    )

    if (searchResults.isNotEmpty()) {
        androidx.activity.compose.BackHandler {
            onClearSearch()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
    ) {
        // Search bar
        item {
            SearchInputBar(
                isFetching = isSearching,
                fetchingMessage = "Searching YouTube & Music...",
                onAnalyzeUrl = { query -> onSearch(query) }
            )
        }

        // Filter chips (All | Songs & Music | Artists | Videos)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SearchFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(palette.primary, palette.primary.copy(alpha = 0.85f))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(palette.surface.copy(alpha = 0.90f), palette.surfaceVariant.copy(alpha = 0.65f))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                brush = Brush.verticalGradient(
                                    if (isSelected) listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.15f))
                                    else listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember(filter) { MutableInteractionSource() },
                                indication = null
                            ) { onFilterChanged(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) palette.onPrimary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isSearching) {
            item {
                com.eurtlabs.stash.ui.components.SearchResultsSkeleton()
            }
        } else if (searchResults.isNotEmpty()) {
            // Results Header
            item {
                Text(
                    text = if (selectedFilter == SearchFilter.ARTISTS) "ALL SONGS (${searchResults.size})" else "SEARCH RESULTS (${searchResults.size})",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            items(items = searchResults, key = { it.id }) { resultItem ->
                val isPlaying = playerState.currentTrack?.id == resultItem.id && playerState.isPlaying
                val downloadProgress = activeDownloadsMap[resultItem.id]
                val isDownloaded = downloadedTrackIds.contains(resultItem.id)

                SearchResultCard(
                    item = resultItem,
                    isPlaying = isPlaying,
                    downloadProgress = downloadProgress,
                    isDownloaded = isDownloaded,
                    onPlay = { onPlayTrack(resultItem, searchResults) },
                    onDownload = { onDownloadItem(resultItem) }
                )
            }
        } else {
            // Default Initial State: Quick Paste, Trending Searches
            item {
                // Quick Paste Action Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(palette.surfaceVariant.copy(alpha = 0.92f), palette.surface.copy(alpha = 0.70f))
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) onSearch(text)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Paste Link from Clipboard",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Instant streaming or download from YouTube link",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Searches Chips
            item {
                Text(
                    text = "POPULAR SEARCHES",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickSearches) { query ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.surfaceVariant)
                                .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                                .clickable { onSearch(query) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = query,
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val palette = LocalStashPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (isPlaying) {
                        listOf(palette.primary.copy(alpha = 0.22f), palette.primary.copy(alpha = 0.08f))
                    } else {
                        listOf(palette.surface.copy(alpha = 0.88f), palette.surfaceVariant.copy(alpha = 0.60f))
                    }
                )
            )
            .border(
                width = if (isPlaying) 1.2.dp else 0.8.dp,
                brush = Brush.verticalGradient(
                    if (isPlaying) listOf(palette.primary.copy(alpha = 0.7f), palette.primary.copy(alpha = 0.1f))
                    else listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.03f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember(item.id) { MutableInteractionSource() },
                indication = null
            ) { onPlay() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with play/wave overlay
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val thumbUrl = com.eurtlabs.stash.util.ArtworkUtils.getHighResArtworkUrl(item.thumbnailUrl, item.id)
            AsyncImage(
                model = thumbUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.50f)),
                    contentAlignment = Alignment.Center
                ) {
                    AudioEqualizerVisualizer(tint = palette.primary)
                }
            } else if (item.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.durationText,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = if (isPlaying) palette.primary else palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.artist,
                fontSize = 11.5.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Circular Live Percentage Download Status Widget
        com.eurtlabs.stash.ui.screens.DownloadStatusWidget(
            progress = downloadProgress,
            isDownloaded = isDownloaded,
            onDownload = onDownload
        )
    }
}
