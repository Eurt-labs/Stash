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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

    // Full screen overlay with deep frosted dark backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(interactionSource = interactionSource, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Liquid Glass Modal Sheet — WebGL shader refraction aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = palette.primary)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF1F1F26),   // specular top offset (shader gradient)
                            Color(0xFF17171C),   // frosted core
                            Color(0xFF111115)    // bottom edge
                        )
                    )
                )
                .drawBehind {
                    // Inner lens luminance glow (simulates shader lens distortion center brightness)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.015f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = size.width * 0.6f
                        ),
                        topLeft = Offset(size.width * 0.1f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.5f)
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.60f),  // bright meniscus top (rb2 rim)
                            Color.White.copy(alpha = 0.18f),  // mid fade
                            Color.White.copy(alpha = 0.03f)   // invisible bottom
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null) { /* Consume taps */ }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Grab Bar with Specular Highlight
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.60f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Track Header Info (Art + Title + Artist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surfaceVariant)
                            .border(1.2.dp, Brush.verticalGradient(listOf(Color.White.copy(0.40f), Color.White.copy(0.05f))), RoundedCornerShape(18.dp)),
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

                        Spacer(modifier = Modifier.height(5.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.primary.copy(alpha = 0.20f))
                                    .border(0.8.dp, palette.primary.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.format.ext.uppercase(),
                                    color = palette.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "•  ${item.quality.label.substringBefore(" ")}",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp
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

                // Action Pills Section (Refracted Liquid Glass Cloud Capsules)
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
                            LiquidCloudActionPill(
                                icon = Icons.Default.Pause,
                                label = "Pause Download",
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onPause(item.id)
                                    onDismiss()
                                }
                            )

                            LiquidCloudActionPill(
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
                        LiquidCloudActionPill(
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
                        LiquidCloudActionPill(
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
                            LiquidCloudActionPill(
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

                            LiquidCloudActionPill(
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
                        LiquidCloudActionPill(
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

                        LiquidCloudActionPill(
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
private fun LiquidCloudActionPill(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalStashPalette.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF282830),
                        Color(0xFF1C1C22)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}
