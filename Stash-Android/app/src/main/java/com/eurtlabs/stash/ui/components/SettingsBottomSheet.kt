package com.eurtlabs.stash.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.ui.theme.SurfaceCard
import com.eurtlabs.stash.ui.theme.SurfaceDark
import com.eurtlabs.stash.ui.theme.TextPrimary
import com.eurtlabs.stash.ui.theme.TextSecondary
import com.eurtlabs.stash.ui.theme.getThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: StashSettings,
    onDismiss: () -> Unit,
    onSelectTheme: (ColorTheme) -> Unit,
    onSelectFormat: (DownloadFormat) -> Unit,
    onSelectQuality: (DownloadQuality) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Preferences & Quality",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Audio / Video Format
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadFormat.values().forEach { format ->
                    val isSelected = settings.format == format
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else SurfaceCard)
                            .clickable { onSelectFormat(format) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format.name,
                            color = TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Bitrate / Resolution Quality
            Text(
                text = "Bitrate / Resolution",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DownloadQuality.values().forEach { quality ->
                    val isSelected = settings.quality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SurfaceCard)
                            .clickable { onSelectQuality(quality) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.label,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Text(text = "✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Theme & Artist Palettes
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ColorTheme.values()) { theme ->
                    val palette = getThemePalette(theme)
                    val isSelected = settings.theme == theme
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectTheme(theme) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .padding(2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = theme.displayName,
                            color = if (isSelected) palette.primary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
