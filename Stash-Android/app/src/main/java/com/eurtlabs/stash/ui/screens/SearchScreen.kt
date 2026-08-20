package com.eurtlabs.stash.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eurtlabs.stash.data.model.SearchFilter
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.ui.components.SearchInputBar
import com.eurtlabs.stash.ui.theme.LocalStashPalette

@Composable
fun SearchScreen(
    isSearching: Boolean,
    searchResults: List<SearchResultItem>,
    selectedFilter: SearchFilter,
    onFilterChanged: (SearchFilter) -> Unit,
    onSearch: (String) -> Unit,
    onDownloadItem: (SearchResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val context = LocalContext.current

    val quickSearches = listOf(
        "The Weeknd",
        "Daft Punk",
        "Coldplay",
        "Billie Eilish",
        "Taylor Swift",
        "Lofi Hip Hop",
        "Synthwave 80s"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
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
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SearchFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primary else palette.surface)
                            .border(1.dp, if (isSelected) palette.primary else palette.border, RoundedCornerShape(10.dp))
                            .clickable { onFilterChanged(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Loading spinner when searching
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = palette.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Searching YouTube Music & Videos...",
                            color = palette.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        } else if (searchResults.isNotEmpty()) {
            // Search Results List
            item {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            items(items = searchResults, key = { it.id }) { resultItem ->
                SearchResultCard(
                    item = resultItem,
                    onDownload = { onDownloadItem(resultItem) }
                )
            }
        } else {
            // Default Initial State: Quick Paste, Trending Searches, and Source Overview
            item {
                // Quick Paste Action Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank()) {
                                onSearch(clip)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Paste from Clipboard",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap to instantly analyze copied YouTube / Music URL",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

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
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickSearches) { searchItem ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surface)
                                .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                                .clickable { onSearch(searchItem) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = searchItem,
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SUPPORTED PLATFORMS",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceInfoRow(
                        icon = Icons.Default.MusicNote,
                        title = "YouTube Music Tracks & Artists",
                        subtitle = "Lossless 320kbps audio with album cover artwork"
                    )
                    SourceInfoRow(
                        icon = Icons.Default.PlayCircleOutline,
                        title = "YouTube Videos & Shorts",
                        subtitle = "Download in Full HD / 4K or pure audio stream"
                    )
                    SourceInfoRow(
                        icon = Icons.Default.VideoLibrary,
                        title = "Full Playlists & Channels",
                        subtitle = "Parallel batch downloading for entire playlists"
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onDownload: () -> Unit
) {
    val palette = LocalStashPalette.current
    var isAdded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .clickable {
                isAdded = true
                onDownload()
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with duration overlay
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceVariant)
                .border(0.5.dp, palette.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val thumbUrl = item.thumbnailUrl ?: "https://i.ytimg.com/vi/${item.id}/hqdefault.jpg"
            AsyncImage(
                model = thumbUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill on bottom-right of thumbnail
            if (item.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.durationText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.artist,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Download Action Pill Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAdded) palette.success.copy(alpha = 0.15f) else palette.primary)
                .clickable {
                    isAdded = true
                    onDownload()
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isAdded) Icons.Default.DownloadDone else Icons.Default.Download,
                    contentDescription = "Download",
                    tint = if (isAdded) palette.success else palette.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isAdded) "Added" else "Get",
                    color = if (isAdded) palette.success else palette.onPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SourceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalStashPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}
