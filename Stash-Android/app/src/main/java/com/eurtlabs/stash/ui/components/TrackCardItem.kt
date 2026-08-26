package com.eurtlabs.stash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current

    val animatedProgress by animateFloatAsState(
        targetValue = (item.progress / 100f).coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress"
    )

    // Liquid Glass Card Surface
    LiquidGlassCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        cornerRadius = 18.dp,
        rimAlpha = 0.45f,
        innerPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Album Artwork
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.trackInfo.albumArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.trackInfo.albumArtUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title + Artist + Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.trackInfo.title,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.trackInfo.artists.joinToString(", ").ifEmpty { "Media" },
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(text = "·", fontSize = 10.sp, color = palette.textSecondary.copy(alpha = 0.5f))
                        Text(
                            text = item.format.ext.uppercase(),
                            color = palette.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Live status text
                    if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.TAGGING ||
                        item.state == DownloadState.FAILED || item.state == DownloadState.CANCELLED ||
                        item.state == DownloadState.IDLE
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (item.state) {
                                DownloadState.DOWNLOADING -> if (item.eta.isNotBlank()) "Downloading: ${item.progress.toInt()}% • ETA ${item.eta}" else "Downloading: ${item.progress.toInt()}%"
                                DownloadState.TAGGING -> "Embedding tags..."
                                DownloadState.FAILED -> item.errorMessage?.take(50) ?: "Error"
                                DownloadState.CANCELLED -> "Cancelled"
                                DownloadState.IDLE -> "Paused"
                                else -> item.statusMessage
                            },
                            fontSize = 10.5.sp,
                            color = when (item.state) {
                                DownloadState.FAILED -> palette.error
                                DownloadState.CANCELLED, DownloadState.IDLE -> palette.textSecondary
                                else -> palette.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Compact Right Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.state) {
                                DownloadState.COMPLETED -> palette.primary.copy(alpha = 0.18f)
                                DownloadState.FAILED -> palette.error.copy(alpha = 0.15f)
                                DownloadState.DOWNLOADING, DownloadState.TAGGING -> palette.primary.copy(alpha = 0.15f)
                                else -> palette.surfaceVariant
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.state) {
                        DownloadState.COMPLETED -> Icon(Icons.Default.Check, "Done", tint = palette.primary, modifier = Modifier.size(13.dp))
                        DownloadState.DOWNLOADING, DownloadState.TAGGING -> Text("${item.progress.toInt()}%", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        DownloadState.FAILED -> Icon(Icons.Default.ErrorOutline, "Error", tint = palette.error, modifier = Modifier.size(13.dp))
                        else -> Text(if (item.state == DownloadState.IDLE) "||" else "···", color = palette.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Slim Progress Bar
            AnimatedVisibility(
                visible = item.state == DownloadState.DOWNLOADING || item.state == DownloadState.TAGGING,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = palette.primary,
                    trackColor = palette.surfaceVariant
                )
            }
        }
    }
}
