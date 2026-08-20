package com.eurtlabs.stash.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.eurtlabs.stash.ui.theme.BorderSubtle
import com.eurtlabs.stash.ui.theme.GreenSuccess
import com.eurtlabs.stash.ui.theme.RedError
import com.eurtlabs.stash.ui.theme.SurfaceCard
import com.eurtlabs.stash.ui.theme.TextPrimary
import com.eurtlabs.stash.ui.theme.TextSecondary

@Composable
fun TrackCardItem(item: DownloadItem) {
    val animatedProgress by animateFloatAsState(
        targetValue = (item.progress / 100f).coerceIn(0f, 1f),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album Artwork with Coil
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!item.trackInfo.albumArtUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.trackInfo.albumArtUrl,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.trackInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.trackInfo.artists.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // State Badge Pill
            val (badgeBg, badgeText) = when (item.state) {
                DownloadState.COMPLETED -> GreenSuccess.copy(alpha = 0.15f) to GreenSuccess
                DownloadState.DOWNLOADING, DownloadState.CONVERTING, DownloadState.TAGGING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                DownloadState.FAILED -> RedError.copy(alpha = 0.15f) to RedError
                else -> TextSecondary.copy(alpha = 0.12f) to TextSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (item.state) {
                        DownloadState.COMPLETED -> "Done"
                        DownloadState.DOWNLOADING -> "${item.progress.toInt()}%"
                        DownloadState.CONVERTING -> "Convert"
                        DownloadState.TAGGING -> "Tagging"
                        DownloadState.FAILED -> "Failed"
                        else -> "Queued"
                    },
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Progress Indicator
        if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.CONVERTING) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = animatedProgress,
                color = MaterialTheme.colorScheme.primary,
                trackColor = BorderSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
