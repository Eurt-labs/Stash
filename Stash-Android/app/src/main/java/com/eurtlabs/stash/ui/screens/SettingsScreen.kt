package com.eurtlabs.stash.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.downloader.LogManager
import com.eurtlabs.stash.data.downloader.YoutubeDLManager
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.MediaType
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.ui.theme.getThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: StashSettings,
    onSelectMediaType: (MediaType) -> Unit,
    onSelectTheme: (ColorTheme) -> Unit,
    onSelectFormat: (DownloadFormat) -> Unit,
    onSelectQuality: (DownloadQuality) -> Unit,
    onChangeStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val context = LocalContext.current

    val currentMediaType = settings.mediaType
    val currentFormat = settings.format
    val currentQuality = settings.quality

    val isLosslessAudio = currentMediaType == MediaType.AUDIO && (currentFormat == DownloadFormat.FLAC || currentFormat == DownloadFormat.WAV)

    val availableFormats = if (currentMediaType == MediaType.AUDIO) {
        DownloadFormat.values().filter { it.isAudioOnly }
    } else {
        DownloadFormat.values().filter { !it.isAudioOnly }
    }

    val availableQualities = if (currentMediaType == MediaType.AUDIO) {
        DownloadQuality.values().filter { it.isAudioOnly }
    } else {
        DownloadQuality.values().filter { !it.isAudioOnly }
    }

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp, top = 10.dp, start = 18.dp, end = 18.dp)
    ) {
        // Section: Media Mode Toggle with Compact Animated Floating Cloud Capsule
        item {
            Text(
                text = "DOWNLOAD MODE",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Compact Animated Cloud Liquid Glass Mode Track
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF1B1B20),
                                Color(0xFF111115)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp)
            ) {
                val halfWidth = maxWidth / 2
                val bubbleOffset by animateDpAsState(
                    targetValue = if (currentMediaType == MediaType.AUDIO) 0.dp else halfWidth,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
                    label = "modeBubble"
                )

                // Floating Cloud Capsule
                Box(
                    modifier = Modifier
                        .offset(x = bubbleOffset)
                        .width(halfWidth)
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    palette.surfaceVariant.copy(alpha = 0.95f),
                                    palette.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MediaType.values().forEach { mode ->
                        val isSelected = currentMediaType == mode
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 0.98f,
                            animationSpec = spring(stiffness = 500f),
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectMediaType(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) palette.primary else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Horizontal Format Selection (Sleek Compact Cloud Pills)
        item {
            SettingsSectionHeader(
                icon = if (currentMediaType == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Codec & Format" else "Video Container Format",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Select container for music downloads" else "Select container for video downloads"
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableFormats) { format ->
                    val isSelected = currentFormat == format
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(stiffness = 500f),
                        label = "formatScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    if (isSelected) {
                                        listOf(
                                            palette.surfaceVariant.copy(alpha = 0.95f),
                                            palette.surface.copy(alpha = 0.85f)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFF1B1B20),
                                            Color(0xFF111115)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = if (isSelected) 1.3.dp else 1.dp,
                                brush = Brush.verticalGradient(
                                    if (isSelected) listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                                    else listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectFormat(format) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = format.ext.uppercase(),
                                color = if (isSelected) palette.primary else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (format == DownloadFormat.FLAC || format == DownloadFormat.WAV) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(palette.primary.copy(alpha = 0.20f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LOSSLESS",
                                        color = palette.primary,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Quality / Bitrate (Sleek Compact Cloud Pills)
        item {
            SettingsSectionHeader(
                icon = Icons.Filled.Tune,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Bitrate & Compression" else "Video Resolution Quality",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Target bitrate for downloaded music" else "Target resolution for downloaded video stream"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLosslessAudio) {
                // FLAC / WAV: Compact Lossless Studio Audio Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(palette.primary.copy(alpha = 0.16f), Color(0xFF131317))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(palette.primary.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Lossless Studio Audio Active (${currentFormat.ext.uppercase()})",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "Bit-perfect uncompressed master stream. Lossy bitrate compression disabled.",
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                // Horizontal Compact Cloud Lossy Bitrates or Video Resolutions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableQualities) { quality ->
                        val isSelected = currentQuality == quality
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1.0f,
                            animationSpec = spring(stiffness = 500f),
                            label = "qualityScale"
                        )

                        val shortLabel = quality.label.substringBefore(" (")

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        if (isSelected) {
                                            listOf(
                                                palette.surfaceVariant.copy(alpha = 0.95f),
                                                palette.surface.copy(alpha = 0.85f)
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFF1B1B20),
                                                Color(0xFF111115)
                                            )
                                        }
                                    )
                                )
                                .border(
                                    width = if (isSelected) 1.3.dp else 1.dp,
                                    brush = Brush.verticalGradient(
                                        if (isSelected) listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                                        else listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onSelectQuality(quality) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = shortLabel,
                                    color = if (isSelected) palette.primary else palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Color Theme (Compact Cloud Pill Badges)
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "App Color Theme",
                subtitle = "Select clean monochromatic or tinted palette"
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ColorTheme.values()) { theme ->
                    val isSelected = settings.theme == theme
                    val themePal = getThemePalette(theme)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(themePal.surface.copy(alpha = 0.95f), themePal.background)
                                )
                            )
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) palette.primary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(themePal.primary)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Text(
                                text = theme.displayName,
                                color = themePal.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Storage Location (Compact Cloud Capsule)
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Folder,
                title = "Storage & Downloads Folder",
                subtitle = "Where completed audio and video files are saved"
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF1B1B20),
                                Color(0xFF111115)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onChangeStorage() }
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Location",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = settings.outputDir.ifEmpty { "Music / Stash" },
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceVariant)
                            .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Change",
                            color = palette.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Diagnostics, Engine & Logs
        item {
            SettingsSectionHeader(
                icon = Icons.Filled.Tune,
                title = "Engine & Diagnostics",
                subtitle = "Core yt-dlp & FFmpeg runtime status and logs"
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF1B1B20),
                                Color(0xFF111115)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Export Logs Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            LogManager.exportLogs(context)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Diagnostic Logs (.txt)",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Generate and attach full debug log file via Share Sheet",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = "Export 📤",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Update Engine Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            if (!isUpdating) {
                                isUpdating = true
                                updateStatus = "Updating engine..."
                                scope.launch(Dispatchers.IO) {
                                    val result = YoutubeDLManager.updateEngine(context)
                                    withContext(Dispatchers.Main) {
                                        updateStatus = result
                                        isUpdating = false
                                    }
                                }
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Core Engine (yt-dlp)",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = updateStatus ?: "Fetch latest binary updates from official release channel",
                            color = if (updateStatus != null) palette.primary else palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = if (isUpdating) "..." else "Update ↻",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stash Media Downloader",
                    color = palette.textSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalStashPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(15.dp)
        )
        Column {
            Text(
                text = title,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}
