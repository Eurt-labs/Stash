package com.eurtlabs.stash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.data.model.NavigationTab
import com.eurtlabs.stash.ui.components.BatchQueueList
import com.eurtlabs.stash.ui.components.BottomNavBar
import com.eurtlabs.stash.ui.components.SearchInputBar
import com.eurtlabs.stash.ui.components.TopBar
import com.eurtlabs.stash.ui.screens.LibraryScreen
import com.eurtlabs.stash.ui.screens.SearchScreen
import com.eurtlabs.stash.ui.screens.SettingsScreen
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.ui.theme.StashTheme
import com.eurtlabs.stash.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()
    private var activeTabState by mutableStateOf(NavigationTab.QUEUE)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce modern Android 15 & 16 Edge-to-Edge window rendering
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkNotificationPermission()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val batches by viewModel.batches.collectAsState()
            val isFetching by viewModel.isFetching.collectAsState()
            val fetchingMessage by viewModel.fetchingMessage.collectAsState()

            val activeDownloadsCount = batches.flatMap { it.items }.count {
                it.state == DownloadState.DOWNLOADING || it.state == DownloadState.CONVERTING || it.state == DownloadState.TAGGING
            }
            val completedCount = batches.flatMap { it.items }.count { it.state == DownloadState.COMPLETED }

            StashTheme(theme = settings.theme) {
                val palette = LocalStashPalette.current

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.background)
                    ) {
                        // Header
                        TopBar(
                            activeDownloadsCount = activeDownloadsCount,
                            modifier = Modifier.statusBarsPadding()
                        )

                        // Main Content with Animated Tab Transitions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            AnimatedContent(
                                targetState = activeTabState,
                                transitionSpec = {
                                    fadeIn(animationSpec = spring(stiffness = 400f)) togetherWith
                                            fadeOut(animationSpec = spring(stiffness = 400f))
                                },
                                label = "TabTransition"
                            ) { tab ->
                                when (tab) {
                                    NavigationTab.QUEUE -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
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
                                    }
                                    NavigationTab.SEARCH -> {
                                        SearchScreen(
                                            isFetching = isFetching,
                                            fetchingMessage = fetchingMessage,
                                            onAnalyzeUrl = { url ->
                                                viewModel.parseAndEnqueue(url)
                                                activeTabState = NavigationTab.QUEUE
                                            }
                                        )
                                    }
                                    NavigationTab.LIBRARY -> {
                                        LibraryScreen(batches = batches)
                                    }
                                    NavigationTab.SETTINGS -> {
                                        SettingsScreen(
                                            settings = settings,
                                            onSelectMediaType = { viewModel.updateMediaType(it) },
                                            onSelectTheme = { viewModel.updateTheme(it) },
                                            onSelectFormat = { viewModel.updateFormat(it) },
                                            onSelectQuality = { viewModel.updateQuality(it) }
                                        )
                                    }
                                }
                            }
                        }

                        // WhatsApp-style Bottom Navigation Bar
                        BottomNavBar(
                            currentTab = activeTabState,
                            onTabSelected = { activeTabState = it },
                            activeQueueCount = activeDownloadsCount,
                            libraryCount = completedCount
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.parseAndEnqueue(sharedText)
                activeTabState = NavigationTab.QUEUE
            }
        }
    }
}
