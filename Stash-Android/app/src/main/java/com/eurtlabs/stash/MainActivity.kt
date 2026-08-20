package com.eurtlabs.stash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.eurtlabs.stash.data.model.DownloadState
import com.eurtlabs.stash.data.model.NavigationTab
import com.eurtlabs.stash.ui.components.BatchQueueList
import com.eurtlabs.stash.ui.components.BottomNavBar
import com.eurtlabs.stash.ui.components.SearchInputBar
import com.eurtlabs.stash.ui.components.StorageSelectionDialog
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

    private val appPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions result handled */ }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.confirmStorageCustom(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce modern Android 15 & 16 Edge-to-Edge window rendering
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestAppPermissions()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val batches by viewModel.batches.collectAsState()
            val isFetching by viewModel.isFetching.collectAsState()
            val fetchingMessage by viewModel.fetchingMessage.collectAsState()

            val isSearching by viewModel.isSearching.collectAsState()
            val searchResults by viewModel.searchResults.collectAsState()
            val searchFilter by viewModel.searchFilter.collectAsState()
            val showStorageDialog by viewModel.showStorageDialog.collectAsState()

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

                        // Animated Global Fetching / Analyzing Banner
                        AnimatedVisibility(
                            visible = isFetching,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.surface)
                                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = palette.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Fetching Stream & Metadata...",
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = fetchingMessage.ifEmpty { "Analyzing video/audio stream..." },
                                        color = palette.textSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

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
                                                onRemoveBatch = { batchId -> viewModel.removeBatch(batchId) },
                                                onRetryItem = { itemId -> viewModel.retryItem(itemId) }
                                            )
                                        }
                                    }
                                    NavigationTab.SEARCH -> {
                                        SearchScreen(
                                            isSearching = isSearching,
                                            searchResults = searchResults,
                                            selectedFilter = searchFilter,
                                            onFilterChanged = { viewModel.setSearchFilter(it) },
                                            onSearch = { query -> viewModel.performSearch(query) },
                                            onDownloadItem = { item ->
                                                viewModel.enqueueSearchResult(item)
                                                activeTabState = NavigationTab.QUEUE
                                            },
                                            onDownloadAll = { items, artistName ->
                                                viewModel.enqueueAllSearchResults(items, artistName)
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
                                            onSelectQuality = { viewModel.updateQuality(it) },
                                            onChangeStorage = { viewModel.openStorageDialog() }
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

                    // First Launch or Triggered Storage Folder Selection Dialog
                    if (showStorageDialog) {
                        StorageSelectionDialog(
                            currentPath = settings.outputDir,
                            onChooseDefault = { viewModel.confirmStorageDefault() },
                            onChooseCustom = { folderPickerLauncher.launch(null) },
                            onDismiss = { viewModel.dismissStorageDialog() }
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

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            appPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAutoPasteClipboard()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkAndAutoPasteClipboard()
        }
    }

    private var lastAutoPastedUrl: String? = null

    private fun checkAndAutoPasteClipboard() {
        try {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!clip.isNullOrBlank() && (clip.startsWith("http://", ignoreCase = true) || clip.startsWith("https://", ignoreCase = true))) {
                if (clip != lastAutoPastedUrl) {
                    lastAutoPastedUrl = clip
                    viewModel.parseAndEnqueue(clip)
                    activeTabState = NavigationTab.QUEUE
                }
            }
        } catch (e: Exception) {
            // Ignore clipboard access exceptions
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
