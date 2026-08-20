package com.eurtlabs.stash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eurtlabs.stash.ui.components.BatchQueueList
import com.eurtlabs.stash.ui.components.SearchInputBar
import com.eurtlabs.stash.ui.components.SettingsBottomSheet
import com.eurtlabs.stash.ui.components.TopBar
import com.eurtlabs.stash.ui.theme.StashTheme
import com.eurtlabs.stash.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val batches by viewModel.batches.collectAsState()
            val isFetching by viewModel.isFetching.collectAsState()
            val fetchingMessage by viewModel.fetchingMessage.collectAsState()

            var showSettingsSheet by remember { mutableStateOf(false) }

            StashTheme(theme = settings.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        TopBar(onOpenSettings = { showSettingsSheet = true })

                        SearchInputBar(
                            isFetching = isFetching,
                            fetchingMessage = fetchingMessage,
                            onAnalyzeUrl = { url -> viewModel.parseAndEnqueue(url) }
                        )

                        BatchQueueList(
                            batches = batches,
                            onRemoveBatch = { batchId -> viewModel.removeBatch(batchId) }
                        )
                    }

                    if (showSettingsSheet) {
                        SettingsBottomSheet(
                            settings = settings,
                            onDismiss = { showSettingsSheet = false },
                            onSelectTheme = { viewModel.updateTheme(it) },
                            onSelectFormat = { viewModel.updateFormat(it) },
                            onSelectQuality = { viewModel.updateQuality(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.parseAndEnqueue(sharedText)
            }
        }
    }
}
