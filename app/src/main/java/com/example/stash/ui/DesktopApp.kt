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
    val isFetching by orchestrator.isFetching.collectAsState()
    val fetchingStatus by orchestrator.fetchingStatus.collectAsState()
    val isUpdating by orchestrator.isUpdating.collectAsState()
    val updateStatus by orchestrator.updateStatus.collectAsState()

    // Dropdown expanded states
    var qualityDropdownExpanded by remember { mutableStateOf(false) }
    var formatDropdownExpanded by remember { mutableStateOf(false) }
    val collapsedBatches = remember { mutableStateMapOf<String, Boolean>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDependencyDialog by remember { mutableStateOf(false) }
    var isYtDlpInstalled by remember { mutableStateOf(false) }
    var isFfmpegInstalled by remember { mutableStateOf(false) }

    var ytDlpChecked by remember { mutableStateOf(false) }
    var ffmpegChecked by remember { mutableStateOf(false) }
    var showWarningPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ytDlpOk = checkCommand("yt-dlp", "--version")
        val ffmpegOk = checkCommand("ffmpeg", "-version")
        
        isYtDlpInstalled = ytDlpOk
        isFfmpegInstalled = ffmpegOk
        
        if (!ytDlpOk || !ffmpegOk) {
            showDependencyDialog = true
        }
    }

    val lightColors = lightColors(
        primary = Color(0xFF6366F1),       // Premium Indigo
        primaryVariant = Color(0xFF4F46E5),// Deep Indigo
        secondary = Color(0xFF10B981),     // Emerald
        background = Color(0xFFF8FAFC),    // Slate 50 (clean light background)
        surface = Color(0xFFFFFFFF),       // White card surface
        error = Color(0xFFEF4444),         // Red
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF0F172A),  // Slate 900 (dark text)
        onSurface = Color(0xFF1E293B)      // Slate 800 (dark text)
    )

    MaterialTheme(colors = lightColors) {
        Column(modifier = Modifier.fillMaxSize().background(lightColors.background).padding(24.dp)) {

            // ── Header ──
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stash Downloader", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "~By eurt-labs",
                    style = MaterialTheme.typography.caption.copy(fontSize = 12.sp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supports downloading YouTube Videos, YouTube Music Tracks/Playlists/Albums, and other media sources.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colors.surface)
                        .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
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
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val selectedPath = chooseDirectory(outputDir)
                            if (selectedPath != null) {
                                orchestrator.setOutputDirectory(selectedPath)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            val icon = when (quality) {
                                DownloadQuality.LOW -> "🟢"
                                DownloadQuality.MID -> "🟡"
                                DownloadQuality.HIGH -> "🔴"
                            }
                            Text("$icon ${quality.getLabelForFormat(format)}")
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
                                    Text("$icon ${q.getLabelForFormat(format)}")
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
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
                    label = { Text("Enter Link or Artist Name") },
                    placeholder = { Text("e.g. Taylor Swift, or paste YouTube URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isProcessing && !isFetching,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = MaterialTheme.colors.onSurface,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colors.primary,
                        backgroundColor = MaterialTheme.colors.surface
                    )
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
                                    errorMessage = e.message ?: "Failed to process link"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && !isFetching && linkInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isProcessing || isFetching) "Processing..." else "Download")
                }
            }

            // Fetching/Processing live feedback card
            if (isFetching || isProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (fetchingStatus.isNotBlank()) fetchingStatus else "Processing and batching tracks...",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Action Buttons ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { orchestrator.cancelAll() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Stop All", color = MaterialTheme.colors.onError)
                }
                OutlinedButton(
                    onClick = { orchestrator.queueManager.clearFinished() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Clear Finished")
                }
                Button(
                    onClick = { orchestrator.checkForUpdates() },
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Update", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isUpdating) "Updating..." else "Check for Updates")
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
                                shape = RoundedCornerShape(12.dp),
                                elevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Batch header
                                    val isCollapsed = collapsedBatches[batch.id] == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { collapsedBatches[batch.id] = !isCollapsed }
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    batch.name,
                                                    style = MaterialTheme.typography.subtitle1,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                 Text(
                                                     if (isCollapsed) "▶" else "▼",
                                                     style = MaterialTheme.typography.body2,
                                                     color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                                 )
                                            }
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

                                    if (!isCollapsed) {
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
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                // Pause/Resume actions
                                                if (item.state == DownloadState.QUEUED || item.state == DownloadState.DOWNLOADING || item.state == DownloadState.CONVERTING) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clickable { orchestrator.pauseTrack(item.id) }
                                                            .padding(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "⏸",
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colors.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else if (item.state == DownloadState.PAUSED) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clickable { orchestrator.resumeTrack(item.id) }
                                                            .padding(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "▶",
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colors.primary,
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
            if (updateStatus.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { orchestrator.clearUpdateStatus() },
                    title = { Text("Update Status") },
                    text = { 
                        Text(
                            text = updateStatus,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        ) 
                    },
                    confirmButton = {
                        Button(onClick = { orchestrator.clearUpdateStatus() }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Error") },
                    text = { 
                        Text(
                            text = errorMessage!!,
                            fontSize = 14.sp
                        ) 
                    },
                    confirmButton = {
                        Button(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (showDependencyDialog) {
                AlertDialog(
                    onDismissRequest = { /* Prevent dismissing by clicking outside */ },
                    title = {
                        Text(
                            text = "⚠️ Missing System Dependencies",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colors.error
                        )
                    },
                    text = {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Stash requires both 'yt-dlp' and 'ffmpeg' to be installed and available in your system's Environment PATH to download and convert files.",
                                style = MaterialTheme.typography.body2
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (!isYtDlpInstalled) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { ytDlpChecked = !ytDlpChecked }
                                ) {
                                    Checkbox(
                                        checked = ytDlpChecked,
                                        onCheckedChange = { ytDlpChecked = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "I understand I need to install 'yt-dlp' (Important)",
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            if (!isFfmpegInstalled) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { ffmpegChecked = !ffmpegChecked }
                                ) {
                                    Checkbox(
                                        checked = ffmpegChecked,
                                        onCheckedChange = { ffmpegChecked = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "I understand I need to install 'ffmpeg' (Important)",
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            if (showWarningPrompt) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "❌ Please check the boxes to confirm you understand that these dependencies are required.",
                                    color = MaterialTheme.colors.error,
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val ytDlpRequirementMet = isYtDlpInstalled || ytDlpChecked
                                val ffmpegRequirementMet = isFfmpegInstalled || ffmpegChecked
                                
                                if (ytDlpRequirementMet && ffmpegRequirementMet) {
                                    showDependencyDialog = false
                                } else {
                                    showWarningPrompt = true
                                }
                            }
                        ) {
                            Text("Proceed")
                        }
                    }
                )
            }
        }
    }
}

private fun chooseDirectory(currentDir: String): String? {
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("win")) {
        try {
            val script = """
                Add-Type -AssemblyName System.Windows.Forms
                ${'$'}dialog = New-Object System.Windows.Forms.FolderBrowserDialog
                ${'$'}dialog.SelectedPath = "$currentDir"
                ${'$'}dialog.Description = "Select Output Folder"
                if (${'$'}dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
                    Write-Output ${'$'}dialog.SelectedPath
                } else {
                    Write-Output "__CANCEL__"
                }
            """.trimIndent()
            val process = ProcessBuilder("powershell", "-NoProfile", "-Command", script)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (output == "__CANCEL__") {
                return null
            }
            if (output.isNotBlank() && !output.startsWith("Error")) {
                return output
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Fallback to JFileChooser
    var selectedPath: String? = null
    val chooser = javax.swing.JFileChooser(currentDir).apply {
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Output Folder"
    }
    val result = chooser.showOpenDialog(null)
    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
        selectedPath = chooser.selectedFile.absolutePath
    }
    return selectedPath
}

private fun checkCommand(cmd: String, arg: String): Boolean {
    return try {
        val process = ProcessBuilder(cmd, arg)
            .redirectErrorStream(true)
            .start()
        process.destroy()
        true
    } catch (e: Exception) {
        false
    }
}
