package com.eurtlabs.stash.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.eurtlabs.stash.data.model.DownloadBatch
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.ui.components.AudioEqualizerVisualizer
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.util.ArtworkUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    batches: List<DownloadBatch>,
    onRemoveFromLibrary: (String) -> Unit = {},
    onDeleteFromDevice: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val context = LocalContext.current

    val sortModeState = remember { mutableStateOf(0) }
    val sortAscendingState = remember { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    val completedItems = batches.flatMap { it.items }
        .filter { it.state == DownloadState.COMPLETED && !it.finalFilePath.isNullOrBlank() }
        .let { items ->
            when (sortModeState.value) {
                1 -> if (sortAscendingState.value) items.sortedBy { it.trackInfo.title.lowercase() } else items.sortedByDescending { it.trackInfo.title.lowercase() }
                2 -> if (sortAscendingState.value) items.sortedBy { File(it.finalFilePath ?: "").length() } else items.sortedByDescending { File(it.finalFilePath ?: "").length() }
                else -> if (sortAscendingState.value) items else items.reversed()
            }
        }

    if (itemToDelete != null) {
        val target = itemToDelete!!
        com.eurtlabs.stash.ui.components.LiquidGlassConfirmDialog(
            title = "Delete Downloaded Song?",
            message = "Are you sure you want to remove this track from your library and delete the audio file from device storage?",
            confirmText = "Delete",
            cancelText = "Cancel",
            onConfirm = {
                onDeleteFromDevice(target.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (completedItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(palette.surface.copy(alpha = 0.92f), palette.surfaceVariant.copy(alpha = 0.70f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Library is Empty",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Completed music downloads will appear here persistently for offline listening and sharing.",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp, top = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED LIBRARY",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = "${completedItems.size} files",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Sort Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Recent", "Name", "Size").forEachIndexed { index, label ->
                        val isSelected = sortModeState.value == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(listOf(palette.primary, palette.primary.copy(alpha = 0.85f)))
                                    } else {
                                        Brush.verticalGradient(listOf(palette.surface, palette.surfaceVariant))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) palette.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (sortModeState.value == index) {
                                        sortAscendingState.value = !sortAscendingState.value
                                    } else {
                                        sortModeState.value = index
                                        sortAscendingState.value = false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) palette.onPrimary else palette.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = if (sortAscendingState.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Sort direction",
                                        tint = palette.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(items = completedItems, key = { it.id }) { item ->
                LibraryItemCard(
                    item = item,
                    allItems = completedItems,
                    context = context,
                    onDeleteClick = { itemToDelete = item }
                )
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: DownloadItem,
    allItems: List<DownloadItem>,
    context: Context,
    onDeleteClick: () -> Unit
) {
    val palette = LocalStashPalette.current
    val playerState by com.eurtlabs.stash.player.MusicPlayerManager.playerState.collectAsState()
    val isCurrentlyPlaying = (playerState.currentItem?.id == item.id || playerState.currentTrack?.id == item.id) && playerState.isPlaying

    val file = item.finalFilePath?.let { File(it) }
    val fileSizeMb = if (file != null && file.exists()) {
        String.format("%.1f MB", file.length().toDouble() / (1024 * 1024))
    } else ""
    val highResArt = ArtworkUtils.getHighResArtworkUrl(item.trackInfo.albumArtUrl, item.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    if (isCurrentlyPlaying) {
                        listOf(
                            palette.primary.copy(alpha = 0.22f),
                            palette.primary.copy(alpha = 0.08f)
                        )
                    } else {
                        listOf(
                            palette.surface.copy(alpha = 0.92f),
                            palette.surfaceVariant.copy(alpha = 0.70f)
                        )
                    }
                )
            )
            .border(
                width = if (isCurrentlyPlaying) 1.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    if (isCurrentlyPlaying) {
                        listOf(
                            palette.primary.copy(alpha = 0.8f),
                            palette.primary.copy(alpha = 0.2f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                // ALWAYS play directly inside Stash built-in Music Player! Never open external apps!
                com.eurtlabs.stash.player.MusicPlayerManager.playLibraryItem(
                    item = item,
                    allItems = allItems
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (highResArt.isNotBlank()) {
                AsyncImage(
                    model = highResArt,
                    contentDescription = item.trackInfo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioEqualizerVisualizer(tint = palette.primary)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.trackInfo.title,
                color = if (isCurrentlyPlaying) palette.primary else palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.trackInfo.artists.joinToString(", "),
                    color = palette.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (fileSizeMb.isNotEmpty()) {
                    Text(
                        text = "• $fileSizeMb",
                        color = palette.textSecondary.copy(alpha = 0.7f),
                        fontSize = 10.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.format.ext.uppercase(),
                        color = palette.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
            onClick = {
                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = if (item.format.isAudioOnly) "audio/*" else "video/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                }
            },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete from Device",
                tint = palette.error.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
