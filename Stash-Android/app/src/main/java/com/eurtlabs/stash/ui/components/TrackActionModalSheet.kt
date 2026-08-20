package com.eurtlabs.stash.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import java.io.File

@Composable
fun TrackActionModalSheet(
    item: DownloadItem?,
    onDismiss: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (item == null) return

    val palette = LocalStashPalette.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }

    val file = item.finalFilePath?.let { File(it) }
    val isFileAvailable = file != null && file.exists()

    // Full screen overlay with dark dim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(interactionSource = interactionSource, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Liquid Glass Modal Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clickable(interactionSource = interactionSource, indication = null) { /* Consume taps */ }
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            palette.surface.copy(alpha = 0.95f),
                            palette.surfaceVariant.copy(alpha = 0.85f),
                            palette.background.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Grab Bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Track Header Info (Art + Title + Artist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surfaceVariant)
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!item.trackInfo.albumArtUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.trackInfo.albumArtUrl,
                                contentDescription = item.trackInfo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.trackInfo.title,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.trackInfo.artists.joinToString(", ").ifEmpty { "Media Track" },
                            color = palette.textSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.primary.copy(alpha = 0.20f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.format.ext.uppercase(),
                                    color = palette.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "•  ${item.quality.label.substringBefore(" ")}",
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Progress Bar (if active download)
                if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.TAGGING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = (item.progress / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = palette.primary,
                        trackColor = palette.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.statusMessage,
                        color = palette.primary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Pills Section (Liquid Glass Pills)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Downloading State Options: Pause / Cancel
                    if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.FETCHING || item.state == DownloadState.QUEUED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidActionPill(
                                icon = Icons.Default.Pause,
                                label = "Pause Download",
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onPause(item.id)
                                    onDismiss()
                                }
                            )

                            LiquidActionPill(
                                icon = Icons.Default.Close,
                                label = "Cancel Download",
                                color = palette.error,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onCancel(item.id)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // Paused or Cancelled: Resume
                    if (item.state == DownloadState.IDLE || item.state == DownloadState.CANCELLED) {
                        LiquidActionPill(
                            icon = Icons.Default.PlayArrow,
                            label = "Resume Download",
                            color = palette.primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onResume(item.id)
                                onDismiss()
                            }
                        )
                    }

                    // Failed: Retry
                    if (item.state == DownloadState.FAILED) {
                        LiquidActionPill(
                            icon = Icons.Default.Refresh,
                            label = "Retry Download",
                            color = palette.primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onRetry(item.id)
                                onDismiss()
                            }
                        )
                    }

                    // Completed: Play & Share
                    if (isFileAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidActionPill(
                                icon = Icons.Default.PlayArrow,
                                label = "Play Media",
                                color = palette.primary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file!!)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, if (item.format.isAudioOnly) "audio/*" else "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Play Media"))
                                    onDismiss()
                                }
                            )

                            LiquidActionPill(
                                icon = Icons.Default.Share,
                                label = "Share File",
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file!!)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = if (item.format.isAudioOnly) "audio/*" else "video/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // Secondary Utilities: Copy Link & Remove
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidActionPill(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy URL",
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val url = item.trackInfo.sourceUrl
                                if (url.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(url))
                                }
                                onDismiss()
                            }
                        )

                        LiquidActionPill(
                            icon = Icons.Default.DeleteOutline,
                            label = "Remove",
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDelete(item.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidActionPill(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalStashPalette.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        palette.surface.copy(alpha = 0.90f),
                        palette.surfaceVariant.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1
        )
    }
}
