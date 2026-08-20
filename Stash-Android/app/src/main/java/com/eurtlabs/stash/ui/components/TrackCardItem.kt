package com.eurtlabs.stash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.ui.theme.LocalStashPalette

@Composable
fun TrackCardItem(
    item: DownloadItem,
    onRetry: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current

    val animatedProgress by animateFloatAsState(
        targetValue = (item.progress / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 300f),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(palette.surface.copy(alpha = 0.92f), palette.surfaceVariant.copy(alpha = 0.70f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album Artwork with fallback
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surfaceVariant)
                    .border(0.5.dp, palette.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.trackInfo.albumArtUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.trackInfo.albumArtUrl,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.trackInfo.title,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.trackInfo.artists.joinToString(", ").ifEmpty { "Audio Track" },
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = palette.textSecondary.copy(alpha = 0.5f)
                    )

                    Text(
                        text = item.format.name,
                        color = palette.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic live status line (speed, ETA, or error detail)
                if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.TAGGING || item.state == DownloadState.FAILED || item.state == DownloadState.CANCELLED) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (item.state == DownloadState.FAILED) {
                            item.errorMessage?.take(50) ?: "Download error"
                        } else if (item.state == DownloadState.CANCELLED) {
                            "Cancelled"
                        } else {
                            item.statusMessage
                        },
                        fontSize = 11.sp,
                        color = when (item.state) {
                            DownloadState.FAILED -> palette.error
                            DownloadState.CANCELLED -> palette.textSecondary
                            else -> palette.primary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Control Pills
            when (item.state) {
                DownloadState.DOWNLOADING, DownloadState.FETCHING, DownloadState.TAGGING -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceVariant)
                                .clickable { onPause(item.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceVariant)
                                .clickable { onCancel(item.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = palette.error,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                DownloadState.FAILED -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.error.copy(alpha = 0.15f))
                            .clickable { onRetry(item.id) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = palette.error,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Retry",
                            color = palette.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                DownloadState.IDLE, DownloadState.CANCELLED -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.primary.copy(alpha = 0.15f))
                            .clickable { onRetry(item.id) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = palette.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Resume",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {}
            }
        }

        // Animated Progress Bar when downloading
        AnimatedVisibility(
            visible = item.state == DownloadState.DOWNLOADING || item.state == DownloadState.TAGGING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = palette.primary,
                    trackColor = palette.surfaceVariant
                )
            }
        }
    }
}
