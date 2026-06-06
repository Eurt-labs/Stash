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
import com.example.stash.download.DownloadFormat
import com.example.stash.download.DownloadQuality
import com.example.stash.download.DownloadState
import com.example.stash.model.TrackInfo
import com.example.stash.parser.LinkParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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
    private lateinit var adapter: DownloadAdapter

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

    private val NOTIFICATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        orchestrator = StashOrchestrator(this)

        bindViews()
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
        orchestrator.shutdown()
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
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter()
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
    }

    private fun startFetching() {
        val link = linkInput.text?.toString()?.trim() ?: ""

        if (link.isEmpty()) {
            Toast.makeText(this, R.string.error_no_link, Toast.LENGTH_SHORT).show()
            return
        }

        if (!LinkParser.isSupported(link)) {
            Toast.makeText(this, R.string.unsupported_link, Toast.LENGTH_SHORT).show()
            return
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

        // Setup Quality Spinner
        val qualityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadQuality.entries.map { it.label }
        )
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        previewQualitySpinner.adapter = qualityAdapter
        previewQualitySpinner.setSelection(DownloadQuality.entries.indexOf(DownloadQuality.AUDIO_320))

        // Setup Format Spinner
        val formatAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadFormat.entries.map { it.label }
        )
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        previewFormatSpinner.adapter = formatAdapter
        previewFormatSpinner.setSelection(0) // MP3 default

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmDownloadButton.setOnClickListener {
            val quality = DownloadQuality.entries[previewQualitySpinner.selectedItemPosition]
            val format = DownloadFormat.entries[previewFormatSpinner.selectedItemPosition]
            
            // Enqueue downloads!
            orchestrator.enqueueTracks(tracks, quality, format)
            
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
            orchestrator.downloadItems.collectLatest { items ->
                val itemList = items.values.toList().reversed() // Newest first

                adapter.submitList(itemList)

                // Toggle empty state
                emptyState.visibility = if (itemList.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (itemList.isEmpty()) View.GONE else View.VISIBLE

                // Update downloads section header
                val activeCount = items.values.count { 
                    it.state == DownloadState.DOWNLOADING || 
                    it.state == DownloadState.SEARCHING ||
                    it.state == DownloadState.CONVERTING ||
                    it.state == DownloadState.TAGGING
                }
                val queuedCount = items.values.count { it.state == DownloadState.QUEUED }
                
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
