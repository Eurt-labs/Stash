package com.example.stash.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stash.R
import com.example.stash.StashApplication
import com.example.stash.StashOrchestrator
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.stash.download.DownloadBatch
import com.example.stash.download.DownloadFormat
import com.example.stash.download.DownloadQuality
import com.example.stash.download.DownloadState
import com.example.stash.model.TrackInfo
import com.example.stash.parser.LinkParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.ImageButton
import com.google.android.material.card.MaterialCardView
import androidx.core.view.GravityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main screen of the Stash app.
 *
 * Features:
 * - Paste a Spotify or YouTube link
 * - Select quality and format
 * - Tap Download
 * - Watch real-time progress in the list below
 *
 * Auto-detects clipboard content on resume — if it's a supported link,
 * it pre-fills the input field.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var orchestrator: StashOrchestrator
    private lateinit var adapter: BatchAdapter

    // Views
    private lateinit var linkInput: TextInputEditText
    private lateinit var fetchButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var clearButton: TextView
    private lateinit var processingLayout: View
    private lateinit var processingText: TextView
    private lateinit var downloadsHeader: TextView
    private lateinit var folderPathText: TextView
    private lateinit var selectFolderButton: MaterialButton
    private lateinit var platformSpinner: Spinner
    private lateinit var linkInputLayout: TextInputLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var drawerSpotifyFormat: Spinner
    private lateinit var drawerSpotifyQuality: Spinner
    private lateinit var drawerYoutubeFormat: Spinner
    private lateinit var drawerYoutubeQuality: Spinner
    private lateinit var drawerInstagramFormat: Spinner
    private val platforms = arrayOf("Auto Detect", "Spotify", "YouTube", "YouTube Music", "Instagram")

    private val NOTIFICATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        orchestrator = StashApplication.orchestrator

        bindViews()
        setupPlatformSpinner()
        setupDrawerSettings()
        setupRecyclerView()
        setupClickListeners()
        observeDownloads()
        updateFolderUI()
        requestNotificationPermission()
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        autoFillFromClipboard()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * Handles ACTION_SEND intents from other apps (Share → Stash).
     * Extracts the shared URL and pre-fills the link input.
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

            // Spotify share texts often include extra text like "Check out this song..."
            // Extract just the URL
            val urlRegex = Regex("""https?://\S+""")
            val url = urlRegex.find(sharedText)?.value ?: sharedText

            if (LinkParser.isSupported(url)) {
                linkInput.setText(url)
                linkInput.setSelection(url.length)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────────────────

    private fun bindViews() {
        linkInput = findViewById(R.id.linkInput)
        fetchButton = findViewById(R.id.fetchButton)
        recyclerView = findViewById(R.id.downloadsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        clearButton = findViewById(R.id.clearButton)
        processingLayout = findViewById(R.id.processingLayout)
        processingText = findViewById(R.id.processingText)
        downloadsHeader = findViewById(R.id.downloadsHeader)
        folderPathText = findViewById(R.id.folderPathText)
        selectFolderButton = findViewById(R.id.selectFolderButton)
        platformSpinner = findViewById(R.id.platformSpinner)
        linkInputLayout = findViewById(R.id.linkInputLayout)
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        drawerSpotifyFormat = findViewById(R.id.drawerSpotifyFormat)
        drawerSpotifyQuality = findViewById(R.id.drawerSpotifyQuality)
        drawerYoutubeFormat = findViewById(R.id.drawerYoutubeFormat)
        drawerYoutubeQuality = findViewById(R.id.drawerYoutubeQuality)
        drawerInstagramFormat = findViewById(R.id.drawerInstagramFormat)
    }

    private fun setupPlatformSpinner() {
        val platformAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            platforms
        )
        platformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        platformSpinner.adapter = platformAdapter

        platformSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> linkInputLayout.hint = getString(R.string.paste_link_hint_auto)
                    1 -> linkInputLayout.hint = getString(R.string.paste_link_hint_spotify)
                    2 -> linkInputLayout.hint = getString(R.string.paste_link_hint_youtube)
                    3 -> linkInputLayout.hint = getString(R.string.paste_link_hint_youtube_music)
                    4 -> linkInputLayout.hint = getString(R.string.paste_link_hint_instagram)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        linkInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    val parsed = LinkParser.parse(text)
                    if (parsed != null) {
                        val targetPosition = when (parsed.platform) {
                            com.example.stash.model.Platform.SPOTIFY -> 1
                            com.example.stash.model.Platform.YOUTUBE -> 2
                            com.example.stash.model.Platform.YOUTUBE_MUSIC -> 3
                            com.example.stash.model.Platform.INSTAGRAM -> 4
                        }
                        if (platformSpinner.selectedItemPosition != targetPosition) {
                            platformSpinner.setSelection(targetPosition)
                        }
                    }
                }
            }
        })
    }

    private fun setupDrawerSettings() {
        val prefs = getSharedPreferences("stash_prefs", MODE_PRIVATE)

        // 1. Spotify Format Spinner
        val spotifyFormats = DownloadFormat.entries.filter { !it.isVideo }.map { it.label }
        val spotifyFormatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spotifyFormats)
        spotifyFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drawerSpotifyFormat.adapter = spotifyFormatAdapter
        val savedSpotifyFormat = prefs.getString("pref_spotify_format", DownloadFormat.MP3.name)
        val spotifyFormatIndex = DownloadFormat.entries.filter { !it.isVideo }.indexOfFirst { it.name == savedSpotifyFormat }
        if (spotifyFormatIndex >= 0) drawerSpotifyFormat.setSelection(spotifyFormatIndex)

        drawerSpotifyFormat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val format = DownloadFormat.entries.filter { !it.isVideo }[position]
                prefs.edit().putString("pref_spotify_format", format.name).apply()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 2. Spotify Quality Spinner
        val spotifyQualities = DownloadQuality.entries.map { it.label }
        val spotifyQualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spotifyQualities)
        spotifyQualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drawerSpotifyQuality.adapter = spotifyQualityAdapter
        val savedSpotifyQuality = prefs.getString("pref_spotify_quality", DownloadQuality.AUDIO_320.name)
        val spotifyQualityIndex = DownloadQuality.entries.indexOfFirst { it.name == savedSpotifyQuality }
        if (spotifyQualityIndex >= 0) drawerSpotifyQuality.setSelection(spotifyQualityIndex)

        drawerSpotifyQuality.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val quality = DownloadQuality.entries[position]
                prefs.edit().putString("pref_spotify_quality", quality.name).apply()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 3. YouTube Format Spinner
        val youtubeFormats = DownloadFormat.entries.map { it.label }
        val youtubeFormatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, youtubeFormats)
        youtubeFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drawerYoutubeFormat.adapter = youtubeFormatAdapter
        val savedYoutubeFormat = prefs.getString("pref_youtube_format", DownloadFormat.MP3.name)
        val youtubeFormatIndex = DownloadFormat.entries.indexOfFirst { it.name == savedYoutubeFormat }
        if (youtubeFormatIndex >= 0) drawerYoutubeFormat.setSelection(youtubeFormatIndex)

        drawerYoutubeFormat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val format = DownloadFormat.entries[position]
                prefs.edit().putString("pref_youtube_format", format.name).apply()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 4. YouTube Quality Spinner
        val youtubeQualities = DownloadQuality.entries.map { it.label }
        val youtubeQualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, youtubeQualities)
        youtubeQualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drawerYoutubeQuality.adapter = youtubeQualityAdapter
        val savedYoutubeQuality = prefs.getString("pref_youtube_quality", DownloadQuality.AUDIO_320.name)
        val youtubeQualityIndex = DownloadQuality.entries.indexOfFirst { it.name == savedYoutubeQuality }
        if (youtubeQualityIndex >= 0) drawerYoutubeQuality.setSelection(youtubeQualityIndex)

        drawerYoutubeQuality.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val quality = DownloadQuality.entries[position]
                prefs.edit().putString("pref_youtube_quality", quality.name).apply()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        // 5. Instagram Format Spinner
        val instagramFormats = DownloadFormat.entries.map { it.label }
        val instagramFormatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, instagramFormats)
        instagramFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drawerInstagramFormat.adapter = instagramFormatAdapter
        val savedInstagramFormat = prefs.getString("pref_instagram_format", DownloadFormat.VIDEO_BEST.name)
        val instagramFormatIndex = DownloadFormat.entries.indexOfFirst { it.name == savedInstagramFormat }
        if (instagramFormatIndex >= 0) drawerInstagramFormat.setSelection(instagramFormatIndex)

        drawerInstagramFormat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val format = DownloadFormat.entries[position]
                prefs.edit().putString("pref_instagram_format", format.name).apply()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = BatchAdapter()
        adapter.setOnItemClickListener { batch ->
            val intent = Intent(this, BatchDetailActivity::class.java).apply {
                putExtra("BATCH_ID", batch.id)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        fetchButton.setOnClickListener {
            startFetching()
        }

        selectFolderButton.setOnClickListener {
            selectDownloadFolder()
        }

        clearButton.setOnClickListener {
            orchestrator.queueManager.clearFinished()
        }

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun startFetching() {
        val link = linkInput.text?.toString()?.trim() ?: ""

        if (link.isEmpty()) {
            Toast.makeText(this, R.string.error_no_link, Toast.LENGTH_SHORT).show()
            return
        }

        val parsedLink = LinkParser.parse(link)
        if (parsedLink == null) {
            Toast.makeText(this, R.string.unsupported_link, Toast.LENGTH_SHORT).show()
            return
        }

        // Validate platform selection
        val selectedPosition = platformSpinner.selectedItemPosition
        if (selectedPosition > 0) {
            val expectedPlatform = when (selectedPosition) {
                1 -> com.example.stash.model.Platform.SPOTIFY
                2 -> com.example.stash.model.Platform.YOUTUBE
                3 -> com.example.stash.model.Platform.YOUTUBE_MUSIC
                4 -> com.example.stash.model.Platform.INSTAGRAM
                else -> null
            }
            if (parsedLink.platform != expectedPlatform) {
                Toast.makeText(this, R.string.unsupported_link_with_platform, Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!StashApplication.isYtDlpInitialized) {
            Toast.makeText(this, "Initializing download engine, please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button during processing
        fetchButton.isEnabled = false
        fetchButton.text = "Fetching…"
        processingLayout.visibility = View.VISIBLE
        processingText.text = "Analyzing link & fetching metadata..."

        lifecycleScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    orchestrator.fetchMetadata(link)
                }

                if (tracks.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No tracks found", Toast.LENGTH_SHORT).show()
                } else {
                    showFetchPreviewDialog(tracks)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                fetchButton.isEnabled = true
                fetchButton.text = "Fetch Content"
                processingLayout.visibility = View.GONE
            }
        }
    }

    private fun selectDownloadFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, FOLDER_PICKER_REQUEST_CODE)
    }

    private fun updateFolderUI() {
        val prefs = getSharedPreferences("stash_prefs", MODE_PRIVATE)
        val uriStr = prefs.getString("download_folder_uri", null)
        if (uriStr != null) {
            try {
                val uri = Uri.parse(uriStr)
                val docFile = DocumentFile.fromTreeUri(this, uri)
                folderPathText.text = "Folder: ${docFile?.name ?: "Custom Folder"}"
            } catch (e: Exception) {
                folderPathText.text = "Folder: Downloads/Stash"
            }
        } else {
            folderPathText.text = "Folder: Downloads/Stash"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FOLDER_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val treeUri = data?.data ?: return
            
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val prefs = getSharedPreferences("stash_prefs", MODE_PRIVATE)
                prefs.edit().putString("download_folder_uri", treeUri.toString()).apply()
                updateFolderUI()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to select folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFetchPreviewDialog(tracks: List<TrackInfo>) {
        if (tracks.isEmpty()) return

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_fetch_preview, null)
        dialog.setContentView(view)

        val previewImage = view.findViewById<android.widget.ImageView>(R.id.previewImage)
        val previewTitle = view.findViewById<android.widget.TextView>(R.id.previewTitle)
        val previewSubtitle = view.findViewById<android.widget.TextView>(R.id.previewSubtitle)
        val previewTracksRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.previewTracksRecyclerView)
        val previewQualitySpinner = view.findViewById<android.widget.Spinner>(R.id.previewQualitySpinner)
        val previewFormatSpinner = view.findViewById<android.widget.Spinner>(R.id.previewFormatSpinner)
        val cancelButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val confirmDownloadButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.confirmDownloadButton)

        // Set cover art and title info
        val firstTrack = tracks[0]
        previewTitle.text = if (tracks.size == 1) firstTrack.title else "Playlist: ${firstTrack.album ?: "Stash Playlist"}"
        previewSubtitle.text = if (tracks.size == 1) firstTrack.artists.joinToString(", ") else "${tracks.size} track(s)"

        // Fetch cover art image
        firstTrack.albumArtUrl?.let { url ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            withContext(Dispatchers.Main) {
                                previewImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Failed to fetch preview image: ${e.message}")
                }
            }
        }

        // If it's multiple tracks (playlist), show list of track names
        if (tracks.size > 1) {
            previewTracksRecyclerView.visibility = View.VISIBLE
            previewTracksRecyclerView.layoutManager = LinearLayoutManager(this)
            
            previewTracksRecyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    val trackText: TextView = itemView as TextView
                }

                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val tv = TextView(parent.context).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(16, 8, 16, 8)
                        setTextColor(context.getColor(R.color.text_secondary))
                        textSize = 13f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    return ItemViewHolder(tv)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder as ItemViewHolder).trackText.text = "${position + 1}. ${tracks[position].displayName}"
                }

                override fun getItemCount(): Int = tracks.size
            }
        }

        val prefs = getSharedPreferences("stash_prefs", MODE_PRIVATE)
        val defaultFormatName = when (firstTrack.source) {
            com.example.stash.model.Platform.SPOTIFY -> prefs.getString("pref_spotify_format", DownloadFormat.MP3.name)
            com.example.stash.model.Platform.YOUTUBE -> prefs.getString("pref_youtube_format", DownloadFormat.MP3.name)
            com.example.stash.model.Platform.YOUTUBE_MUSIC -> prefs.getString("pref_youtube_format", DownloadFormat.MP3.name)
            com.example.stash.model.Platform.INSTAGRAM -> prefs.getString("pref_instagram_format", DownloadFormat.VIDEO_BEST.name)
        }
        val defaultQualityName = when (firstTrack.source) {
            com.example.stash.model.Platform.SPOTIFY -> prefs.getString("pref_spotify_quality", DownloadQuality.AUDIO_320.name)
            com.example.stash.model.Platform.YOUTUBE -> prefs.getString("pref_youtube_quality", DownloadQuality.AUDIO_320.name)
            com.example.stash.model.Platform.YOUTUBE_MUSIC -> prefs.getString("pref_youtube_quality", DownloadQuality.AUDIO_320.name)
            com.example.stash.model.Platform.INSTAGRAM -> prefs.getString("pref_youtube_quality", DownloadQuality.AUDIO_320.name)
        }

        // Setup Quality Spinner
        val qualityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadQuality.entries.map { it.label }
        )
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        previewQualitySpinner.adapter = qualityAdapter
        val qualityIndex = DownloadQuality.entries.indexOfFirst { it.name == defaultQualityName }.coerceAtLeast(0)
        previewQualitySpinner.setSelection(qualityIndex)

        // Setup Format Spinner
        val formatAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadFormat.entries.map { it.label }
        )
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        previewFormatSpinner.adapter = formatAdapter
        val formatIndex = DownloadFormat.entries.indexOfFirst { it.name == defaultFormatName }.coerceAtLeast(0)
        previewFormatSpinner.setSelection(formatIndex)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmDownloadButton.setOnClickListener {
            val quality = DownloadQuality.entries[previewQualitySpinner.selectedItemPosition]
            val format = DownloadFormat.entries[previewFormatSpinner.selectedItemPosition]
            
            // Enqueue downloads!
            val batchName = if (tracks.size == 1) {
                tracks[0].title
            } else {
                tracks[0].album ?: "Stash Playlist"
            }
            orchestrator.enqueueTracks(tracks, batchName, quality, format)
            
            // Clear input and notify user
            linkInput.text?.clear()
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ──────────────────────────────────────────────────────────────
    // Observe download progress
    // ──────────────────────────────────────────────────────────────

    private fun observeDownloads() {
        lifecycleScope.launch {
            orchestrator.downloadBatches.collectLatest { batches ->
                val itemList = batches.values.toList().sortedByDescending { it.timestamp }

                adapter.submitList(itemList)

                // Toggle empty state
                emptyState.visibility = if (itemList.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (itemList.isEmpty()) View.GONE else View.VISIBLE

                // Update downloads section header
                val activeCount = batches.values.count { 
                    it.state == DownloadState.DOWNLOADING
                }
                val queuedCount = batches.values.count { it.state == DownloadState.QUEUED }
                
                downloadsHeader.text = buildString {
                    append("Downloads")
                    if (activeCount > 0 || queuedCount > 0) {
                        append(" (")
                        val parts = mutableListOf<String>()
                        if (activeCount > 0) parts.add("$activeCount active")
                        if (queuedCount > 0) parts.add("$queuedCount queued")
                        append(parts.joinToString(", "))
                        append(")")
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Clipboard auto-fill
    // ──────────────────────────────────────────────────────────────

    private fun autoFillFromClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return

            val text = clip.getItemAt(0).text?.toString() ?: return

            // Only auto-fill if the input is currently empty and clipboard has a supported link
            if (linkInput.text.isNullOrBlank() && LinkParser.isSupported(text)) {
                linkInput.setText(text)
                linkInput.setSelection(text.length) // Cursor at end
            }
        } catch (_: Exception) {
            // Clipboard access can fail silently — that's fine
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Permissions
    // ──────────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }

    companion object {
        private const val FOLDER_PICKER_REQUEST_CODE = 102
    }
}
