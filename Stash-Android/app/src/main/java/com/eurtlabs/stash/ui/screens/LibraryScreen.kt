package com.eurtlabs.stash.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import com.eurtlabs.stash.ui.theme.LocalStashPalette
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

    val sortModeState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) } // 0=Recent, 1=Name, 2=Size
    val sortAscendingState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val completedItems = batches.flatMap { it.items }.filter { it.state == DownloadState.COMPLETED && !it.finalFilePath.isNullOrBlank() }
        .let { items ->
            when (sortModeState.value) {
                1 -> if (sortAscendingState.value) items.sortedBy { it.trackInfo.title.lowercase() } else items.sortedByDescending { it.trackInfo.title.lowercase() }
                2 -> if (sortAscendingState.value) items.sortedBy { java.io.File(it.finalFilePath!!).length() } else items.sortedByDescending { java.io.File(it.finalFilePath!!).length() }
                else -> if (sortAscendingState.value) items else items.reversed() // Recent
            }
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
                    text = "Completed media downloads will appear here persistently for instant playback and sharing.",
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
            contentPadding = PaddingValues(bottom = 130.dp, top = 8.dp)
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
                        com.eurtlabs.stash.ui.components.LiquidGlassPill(
                            isSelected = isSelected,
                            cornerRadius = 16.dp,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { 
                                        if (sortModeState.value == index) {
                                            sortAscendingState.value = !sortAscendingState.value
                                        } else {
                                            sortModeState.value = index
                                            sortAscendingState.value = false // Default to descending for size/recent, wait!
                                            // Actually let's just leave it false initially so Size is descending (largest first)
                                        }
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    
                                    if (isSelected) {
                                        Icon(
                                            imageVector = if (sortAscendingState.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Sort direction",
                                            tint = palette.textPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(items = completedItems, key = { it.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                onRemoveFromLibrary(item.id)
                                true
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                onDeleteFromDevice(item.id)
                                true
                            }
                            else -> false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        val color = if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                        val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.DeleteForever else Icons.Default.DeleteOutline
                        val text = if (direction == SwipeToDismissBoxValue.StartToEnd) "Delete File" else "Clear"

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.35f))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = color.copy(alpha = 0.40f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text,
                                        tint = color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = text,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = text,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text,
                                        tint = color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Box(modifier = Modifier.background(palette.background)) {
                        LibraryItemCard(item = item, context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: DownloadItem,
    context: Context
) {
    val palette = LocalStashPalette.current
    val file = item.finalFilePath?.let { File(it) }
    val fileSizeMb = if (file != null && file.exists()) {
        String.format("%.1f MB", file.length().toDouble() / (1024 * 1024))
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(palette.surface.copy(alpha = 0.92f), palette.surfaceVariant.copy(alpha = 0.70f))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                // Open file in external player
                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, if (item.format.isAudioOnly) "audio/*" else "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Play Media"))
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art with play icon overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!item.trackInfo.albumArtUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.trackInfo.albumArtUrl,
                    contentDescription = item.trackInfo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
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

        // Title and details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.trackInfo.title,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
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

        Spacer(modifier = Modifier.width(8.dp))

        // Share Icon Button
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
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = palette.textSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
