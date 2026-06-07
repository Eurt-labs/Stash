package com.example.stash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stash.StashOrchestrator
import com.example.stash.download.DownloadFormat
import com.example.stash.download.DownloadQuality
import com.example.stash.download.DownloadState
import kotlinx.coroutines.launch
import javax.swing.JFileChooser

@Composable
fun DesktopApp(orchestrator: StashOrchestrator) {
    var linkInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val batches by orchestrator.downloadBatches.collectAsState()
    val outputDir by orchestrator.outputDir.collectAsState()
    val quality by orchestrator.quality.collectAsState()
    val format by orchestrator.format.collectAsState()

    // Dropdown expanded states
    var qualityDropdownExpanded by remember { mutableStateOf(false) }
    var formatDropdownExpanded by remember { mutableStateOf(false) }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // ── Header ──
            Text("Stash Downloader", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // ── Output Folder Picker ──
            Text("Output Folder", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = outputDir,
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val chooser = JFileChooser(outputDir).apply {
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            dialogTitle = "Select Output Folder"
                        }
                        val result = chooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            orchestrator.setOutputDirectory(chooser.selectedFile.absolutePath)
                        }
                    }
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Browse", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browse")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Quality & Format Row ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quality Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quality", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { qualityDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val icon = when (quality) {
                                DownloadQuality.LOW -> "🟢"
                                DownloadQuality.MID -> "🟡"
                                DownloadQuality.HIGH -> "🔴"
                            }
                            Text("$icon ${quality.label}")
                        }
                        DropdownMenu(
                            expanded = qualityDropdownExpanded,
                            onDismissRequest = { qualityDropdownExpanded = false }
                        ) {
                            DownloadQuality.entries.forEach { q ->
                                val icon = when (q) {
                                    DownloadQuality.LOW -> "🟢"
                                    DownloadQuality.MID -> "🟡"
                                    DownloadQuality.HIGH -> "🔴"
                                }
                                DropdownMenuItem(onClick = {
                                    orchestrator.setQuality(q)
                                    qualityDropdownExpanded = false
                                }) {
                                    Text("$icon ${q.label}")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Format Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text("Format", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { formatDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(format.label)
                        }
                        DropdownMenu(
                            expanded = formatDropdownExpanded,
                            onDismissRequest = { formatDropdownExpanded = false }
                        ) {
                            DownloadFormat.entries.forEach { f ->
                                DropdownMenuItem(onClick = {
                                    orchestrator.setFormat(f)
                                    formatDropdownExpanded = false
                                }) {
                                    Text(f.label)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Link Input + Download Button ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    label = { Text("Enter Spotify/YouTube Link") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val link = linkInput
                        if (link.isNotBlank()) {
                            isProcessing = true
                            scope.launch {
                                try {
                                    orchestrator.processLink(link)
                                    linkInput = ""
                                } catch (e: Exception) {
                                    System.err.println("Failed to process link: ${e.message}")
                                    e.printStackTrace()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && linkInput.isNotBlank()
                ) {
                    Text(if (isProcessing) "Processing..." else "Download")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Action Buttons ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { orchestrator.cancelAll() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                ) {
                    Text("Stop All", color = MaterialTheme.colors.onError)
                }
                OutlinedButton(
                    onClick = { orchestrator.queueManager.clearFinished() }
                ) {
                    Text("Clear Finished")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Download Queue ──
            Text("Download Queue", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            if (batches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No downloads yet. Paste a link above to get started.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn {
                    batches.values.forEach { batch ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                elevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Batch header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                batch.name,
                                                style = MaterialTheme.typography.subtitle1,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${batch.completedTracks}/${batch.totalTracks} completed • ${batch.state.name}",
                                                style = MaterialTheme.typography.caption
                                            )
                                        }
                                        // Cancel batch button
                                        if (batch.state == DownloadState.DOWNLOADING || batch.state == DownloadState.QUEUED) {
                                            IconButton(onClick = { orchestrator.cancelBatch(batch.id) }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Cancel Batch",
                                                    tint = MaterialTheme.colors.error
                                                )
                                            }
                                        }
                                    }

                                    // Batch progress bar
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = batch.progress,
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = when (batch.state) {
                                            DownloadState.COMPLETE -> Color(0xFF4CAF50)
                                            DownloadState.FAILED -> MaterialTheme.colors.error
                                            else -> MaterialTheme.colors.primary
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Individual track items
                                    batch.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // State icon
                                            val stateIcon = when (item.state) {
                                                DownloadState.QUEUED -> Icons.Default.MoreVert
                                                DownloadState.SEARCHING -> Icons.Default.Search
                                                DownloadState.DOWNLOADING -> Icons.Default.ArrowDropDown
                                                DownloadState.CONVERTING -> Icons.Default.Refresh
                                                DownloadState.MOVING -> Icons.Default.Star
                                                DownloadState.TAGGING -> Icons.Default.Edit
                                                DownloadState.COMPLETE -> Icons.Default.Done
                                                DownloadState.FAILED -> Icons.Default.Warning
                                                DownloadState.CANCELLED -> Icons.Default.Clear
                                                DownloadState.PAUSED -> Icons.Default.Refresh
                                            }
                                            val stateColor = when (item.state) {
                                                DownloadState.COMPLETE -> Color(0xFF4CAF50)
                                                DownloadState.FAILED -> MaterialTheme.colors.error
                                                DownloadState.CANCELLED -> Color.Gray
                                                else -> MaterialTheme.colors.primary
                                            }
                                            Icon(
                                                stateIcon,
                                                contentDescription = item.state.name,
                                                tint = stateColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.trackInfo.title,
                                                    style = MaterialTheme.typography.body2,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row {
                                                    Text(
                                                        item.state.name,
                                                        style = MaterialTheme.typography.overline,
                                                        color = stateColor
                                                    )
                                                    if (item.speed != null) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            item.speed,
                                                            style = MaterialTheme.typography.overline,
                                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    if (item.error != null) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            item.error,
                                                            style = MaterialTheme.typography.overline,
                                                            color = MaterialTheme.colors.error
                                                        )
                                                    }
                                                }
                                            }

                                            // Per-item progress
                                            if (item.state == DownloadState.DOWNLOADING || item.state == DownloadState.CONVERTING) {
                                                Text(
                                                    "${(item.progress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.caption,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
