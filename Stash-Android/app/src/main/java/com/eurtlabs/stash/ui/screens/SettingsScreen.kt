package com.eurtlabs.stash.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.MediaType
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.ui.theme.getThemePalette

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

    val currentMediaType = settings.mediaType
    val currentFormat = settings.format
    val currentQuality = settings.quality

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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Section: Media Mode Toggle (Music / Video)
        item {
            Text(
                text = "DOWNLOAD MODE",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MediaType.values().forEach { mode ->
                    val isSelected = currentMediaType == mode
                    val modeBg by animateColorAsState(
                        targetValue = if (isSelected) palette.primary else Color.Transparent,
                        animationSpec = spring(),
                        label = "modeBg"
                    )
                    val modeTextColor by animateColorAsState(
                        targetValue = if (isSelected) palette.onPrimary else palette.textSecondary,
                        animationSpec = spring(),
                        label = "modeTextColor"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(modeBg)
                            .clickable { onSelectMediaType(mode) }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (mode == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = modeTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            color = modeTextColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Dynamic Output Formats (Audio / Video)
        item {
            SettingsSectionHeader(
                icon = if (currentMediaType == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Codec / Container" else "Video Container Format",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Choose lossless or compressed audio output" else "Choose video format"
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableFormats) { format ->
                    val isSelected = currentFormat == format
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) palette.primary else palette.surface,
                        animationSpec = spring(),
                        label = "formatBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) palette.onPrimary else palette.textPrimary,
                        animationSpec = spring(),
                        label = "formatText"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, if (isSelected) palette.primary else palette.border, RoundedCornerShape(12.dp))
                            .clickable { onSelectFormat(format) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format.label,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Dynamic Quality / Bitrate / Resolution
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Speed,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Bitrate & Compression" else "Video Resolution Quality",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Target bitrate for downloaded music" else "Target resolution for downloaded video stream"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableQualities.forEach { quality ->
                    val isSelected = currentQuality == quality

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectQuality(quality) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = quality.label,
                                color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.5.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = palette.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Monochromatic Theme Engine
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "Monochromatic Color Theme",
                subtitle = "Curated minimalist & high-contrast palettes"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ColorTheme.values().forEach { theme ->
                    val themePalette = getThemePalette(theme)
                    val isSelected = settings.theme == theme

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primary else palette.border,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dual Circle Preview
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themePalette.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themePalette.primary)
                                )
                            }

                            Column {
                                Text(
                                    text = theme.displayName,
                                    color = palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = theme.subtitle,
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = palette.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Destination & Engine
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Folder,
                title = "Download Storage",
                subtitle = "App storage directory on your phone"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .clickable { onChangeStorage() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Location",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = settings.outputDir.ifEmpty { "Music / Stash" },
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Change",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Diagnostics, Engine & Logs
            SettingsSectionHeader(
                icon = androidx.compose.material.icons.filled.Speed,
                title = "Engine & Diagnostics",
                subtitle = "Core yt-dlp & FFmpeg runtime status and logs"
            )

            Spacer(modifier = Modifier.height(10.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            var updateStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            var isUpdating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Export Logs Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            com.eurtlabs.stash.data.downloader.LogManager.exportLogs(context)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Diagnostic Logs",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Generate and share full engine debug & error logs",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Text(
                        text = "Export 📤",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Update Engine Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            if (!isUpdating) {
                                isUpdating = true
                                updateStatus = "Updating engine..."
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val result = com.eurtlabs.stash.data.downloader.YoutubeDLManager.updateEngine(context)
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        updateStatus = result
                                        isUpdating = false
                                    }
                                }
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Core Engine (yt-dlp)",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = updateStatus ?: "Fetch latest binary updates from official release channel",
                            color = if (updateStatus != null) palette.primary else palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Text(
                        text = if (isUpdating) "..." else "Update ↻",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stash v2.0.0 for Android • Native NDK Engine",
                    color = palette.textSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = title,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}
