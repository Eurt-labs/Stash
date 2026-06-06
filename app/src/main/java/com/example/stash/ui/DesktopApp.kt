package com.example.stash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stash.StashOrchestrator
import kotlinx.coroutines.launch

@Composable
fun DesktopApp(orchestrator: StashOrchestrator) {
    var linkInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    
    val batches by orchestrator.downloadBatches.collectAsState()

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Stash Downloader", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    label = { Text("Enter Spotify/YouTube Link") },
                    modifier = Modifier.weight(1f)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { orchestrator.cancelAll() },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("Stop All Downloads", color = MaterialTheme.colors.onError)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Download Queue", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                batches.values.forEach { batch ->
                    item {
                        Text("Batch: ${batch.name} (${batch.items.size} tracks)", style = MaterialTheme.typography.subtitle1)
                    }
                    items(batch.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.trackInfo.title, style = MaterialTheme.typography.body1)
                                Text(
                                    "State: ${item.state} | Progress: ${(item.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
