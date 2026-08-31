package com.eurtlabs.stash.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eurtlabs.stash.data.model.SearchResultItem
import com.eurtlabs.stash.player.MusicPlayerManager
import com.eurtlabs.stash.ui.components.AudioEqualizerVisualizer
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.util.ArtworkUtils

@Composable
fun DiscoverScreen(
    quickPicks: List<SearchResultItem>,
    trendingHits: List<SearchResultItem>,
    isLoading: Boolean,
    selectedMood: String,
    activeDownloadsMap: Map<String, Float> = emptyMap(),
    downloadedTrackIds: Set<String> = emptySet(),
    onSelectMood: (String) -> Unit,
    onPlayTrack: (SearchResultItem, List<SearchResultItem>) -> Unit,
    onDownloadTrack: (SearchResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val playerState by MusicPlayerManager.playerState.collectAsState()

    val moods = listOf(
        "Top Hits", "Relax", "Energize", "Focus", "Workout",
        "Commute", "Party", "Lo-Fi", "Pop", "Rock",
        "Electronic", "Acoustic", "Hip-Hop", "Indie"
    )

    if (quickPicks.isEmpty() && isLoading) {
        com.eurtlabs.stash.ui.components.DiscoverScreenSkeleton(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
    ) {
        // Filter Chips Row
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(moods) { mood ->
                    val isSelected = selectedMood.equals(mood, ignoreCase = true) || (mood == "Top Hits" && (selectedMood == "All" || selectedMood == "Top Hits"))
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
                                interactionSource = remember(mood) { MutableInteractionSource() },
                                indication = null
                            ) { onSelectMood(mood) }
                            .padding(horizontal = 16.dp, vertical = 7.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mood,
                            color = if (isSelected) palette.onPrimary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Now Playing / Your Vibe Glass Card
        if (playerState.currentTrack != null) {
            item {
                val currentTrack = playerState.currentTrack!!
                val artUrl = currentTrack.albumArtUrl ?: ""

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.surfaceVariant.copy(alpha = 0.92f),
                                    palette.primary.copy(alpha = 0.22f),
                                    palette.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(palette.primary.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = currentTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (playerState.isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioEqualizerVisualizer(
                                        tint = palette.primary,
                                        modifier = Modifier.size(24.dp, 16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (playerState.isPlaying) "NOW PLAYING" else "PAUSED",
                                    color = palette.primary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack.title,
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack.artists.firstOrNull() ?: "",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { MusicPlayerManager.togglePlayPause() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = palette.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Section: Quick Picks
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "QUICK PICKS",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickPicks) { track ->
                    val isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying
                    val downloadProgress = activeDownloadsMap[track.id]
                    val isDownloaded = downloadedTrackIds.contains(track.id)

                    QuickPickCard(
                        track = track,
                        isPlaying = isPlaying,
                        downloadProgress = downloadProgress,
                        isDownloaded = isDownloaded,
                        onPlay = { onPlayTrack(track, quickPicks) },
                        onDownload = { onDownloadTrack(track) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Trending Now
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "TRENDING NOW",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        itemsIndexed(trendingHits) { index, track ->
            val isPlaying = playerState.currentTrack?.id == track.id && playerState.isPlaying
            val downloadProgress = activeDownloadsMap[track.id]
            val isDownloaded = downloadedTrackIds.contains(track.id)

            TrendingTrackRow(
                index = index + 1,
                track = track,
                isPlaying = isPlaying,
                downloadProgress = downloadProgress,
                isDownloaded = isDownloaded,
                onPlay = { onPlayTrack(track, trendingHits) },
                onDownload = { onDownloadTrack(track) }
            )
        }
    }
}

@Composable
private fun QuickPickCard(
    track: SearchResultItem,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val palette = LocalStashPalette.current
    val artUrl = ArtworkUtils.getHighResArtworkUrl(track.thumbnailUrl, track.id)

    Column(
        modifier = Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.surfaceVariant.copy(alpha = 0.92f),
                        palette.surface.copy(alpha = 0.72f)
                    )
                )
            )
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    if (isPlaying) listOf(palette.primary, palette.primary.copy(alpha = 0.4f))
                    else listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember(track.id) { MutableInteractionSource() },
                indication = null
            ) { onPlay() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(palette.surfaceVariant)
        ) {
            AsyncImage(
                model = artUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.primary)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    AudioEqualizerVisualizer(tint = palette.onPrimary)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = palette.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            color = if (isPlaying) palette.primary else palette.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.artist,
                color = palette.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            DownloadStatusWidget(
                progress = downloadProgress,
                isDownloaded = isDownloaded,
                onDownload = onDownload
            )
        }
    }
}

@Composable
private fun TrendingTrackRow(
    index: Int,
    track: SearchResultItem,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val palette = LocalStashPalette.current
    val artUrl = ArtworkUtils.getHighResArtworkUrl(track.thumbnailUrl, track.id)

    val rankColor = when (index) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> palette.textSecondary.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (isPlaying) listOf(palette.primary.copy(alpha = 0.16f), palette.surfaceVariant.copy(alpha = 0.90f))
                    else listOf(palette.surfaceVariant.copy(alpha = 0.80f), palette.surface.copy(alpha = 0.60f))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    if (isPlaying) listOf(palette.primary.copy(alpha = 0.50f), Color.White.copy(alpha = 0.10f))
                    else listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember(track.id) { MutableInteractionSource() },
                indication = null
            ) { onPlay() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            color = rankColor,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            modifier = Modifier.width(22.dp)
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = artUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center
                ) {
                    AudioEqualizerVisualizer(tint = palette.primary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) palette.primary else palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = palette.textSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DownloadStatusWidget(
            progress = downloadProgress,
            isDownloaded = isDownloaded,
            onDownload = onDownload
        )
    }
}

@Composable
fun DownloadStatusWidget(
    progress: Float?,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current

    if (progress != null) {
        // Active downloading ring with live percentage
        Box(
            modifier = modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = (progress / 100f).coerceIn(0.05f, 1f),
                color = palette.primary,
                trackColor = palette.primary.copy(alpha = 0.20f),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "${progress.toInt()}%",
                color = palette.primary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else if (isDownloaded) {
        // Downloaded Checkmark
        Box(
            modifier = modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Downloaded",
                tint = palette.success,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        // Normal Download Button
        IconButton(
            onClick = onDownload,
            modifier = modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Song",
                tint = palette.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
