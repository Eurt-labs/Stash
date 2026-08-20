package com.eurtlabs.stash.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: StashSettings,
    onDismiss: () -> Unit,
    onSelectMediaType: (MediaType) -> Unit,
    onSelectTheme: (ColorTheme) -> Unit,
    onSelectFormat: (DownloadFormat) -> Unit,
    onSelectQuality: (DownloadQuality) -> Unit
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Preferences & Quality",
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MediaType.values().forEach { mode ->
                    val isSelected = currentMediaType == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primary else Color.Transparent)
                            .clickable { onSelectMediaType(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) palette.onPrimary else palette.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Formats
            Text(
                text = if (currentMediaType == MediaType.AUDIO) "Audio Codec" else "Video Format",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableFormats) { format ->
                    val isSelected = currentFormat == format
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primary else palette.surfaceVariant)
                            .border(1.dp, if (isSelected) palette.primary else palette.border, RoundedCornerShape(10.dp))
                            .clickable { onSelectFormat(format) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format.label,
                            color = if (isSelected) palette.onPrimary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Qualities
            Text(
                text = if (currentMediaType == MediaType.AUDIO) "Bitrate" else "Resolution",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                availableQualities.forEach { quality ->
                    val isSelected = currentQuality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primary.copy(alpha = 0.15f) else palette.surfaceVariant)
                            .border(1.dp, if (isSelected) palette.primary else palette.border, RoundedCornerShape(10.dp))
                            .clickable { onSelectQuality(quality) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.label,
                            color = if (isSelected) palette.primary else palette.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.5.sp
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
