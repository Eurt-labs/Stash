package com.eurtlabs.stash.ui.components

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.model.DownloadBatch
import com.eurtlabs.stash.data.model.DownloadItem
import com.eurtlabs.stash.ui.theme.LocalStashPalette

@Composable
fun BatchQueueList(
    batches: List<DownloadBatch>,
    onRemoveBatch: (String) -> Unit,
    onRetryItem: (String) -> Unit = {},
    onCancelItem: (String) -> Unit = {},
    onPauseItem: (String) -> Unit = {},
    onDeleteItem: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    var selectedItemForModal by remember { mutableStateOf<DownloadItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (batches.isEmpty()) {
            Box(
                modifier = Modifier
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
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        palette.surface.copy(alpha = 0.92f),
                                        palette.surfaceVariant.copy(alpha = 0.70f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.06f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Queue is Empty",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Switch to Search tab or paste a link to download audio and video.",
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
            ) {
                batches.forEach { batch ->
                    item(key = "header_${batch.id}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = batch.name,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${batch.items.size} track(s) • ${batch.format.name} • ${batch.quality.label.substringBefore(" ")}",
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }

                            // Pill-shaped Clear button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onRemoveBatch(batch.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Batch",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    items(items = batch.items, key = { it.id }) { item ->
                        TrackCardItem(
                            item = item,
                            onClick = {
                                selectedItemForModal = item
                            }
                        )
                    }
                }
            }
        }

        // Liquid Glass Modal Action Sheet
        TrackActionModalSheet(
            item = selectedItemForModal,
            onDismiss = { selectedItemForModal = null },
            onPause = { itemId -> onPauseItem(itemId) },
            onResume = { itemId -> onRetryItem(itemId) },
            onCancel = { itemId -> onCancelItem(itemId) },
            onRetry = { itemId -> onRetryItem(itemId) },
            onDelete = { itemId -> onDeleteItem(itemId) }
        )
    }
}
