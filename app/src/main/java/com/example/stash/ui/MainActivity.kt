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
import com.example.stash.download.DownloadFormat
import com.example.stash.download.DownloadQuality
import com.example.stash.download.DownloadState
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
    private lateinit var qualitySpinner: Spinner
    private lateinit var formatSpinner: Spinner
    private lateinit var downloadButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var clearButton: TextView
    private lateinit var processingLayout: View
    private lateinit var processingText: TextView
    private lateinit var downloadsHeader: TextView

    private val NOTIFICATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        orchestrator = StashOrchestrator(this)

        bindViews()
        setupSpinners()
        setupRecyclerView()
        setupClickListeners()
        observeDownloads()
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
        qualitySpinner = findViewById(R.id.qualitySpinner)
        formatSpinner = findViewById(R.id.formatSpinner)
        downloadButton = findViewById(R.id.downloadButton)
        recyclerView = findViewById(R.id.downloadsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        clearButton = findViewById(R.id.clearButton)
        processingLayout = findViewById(R.id.processingLayout)
        processingText = findViewById(R.id.processingText)
        downloadsHeader = findViewById(R.id.downloadsHeader)
    }

    private fun setupSpinners() {
        // Quality spinner
        val qualityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadQuality.entries.map { it.label }
        )
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qualitySpinner.adapter = qualityAdapter
        qualitySpinner.setSelection(DownloadQuality.entries.indexOf(DownloadQuality.AUDIO_320))

        // Format spinner
        val formatAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DownloadFormat.entries.map { it.label }
        )
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        formatSpinner.adapter = formatAdapter
        formatSpinner.setSelection(0) // MP3 default
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        downloadButton.setOnClickListener {
            startDownload()
        }

        clearButton.setOnClickListener {
            orchestrator.queueManager.clearFinished()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Download Logic
    // ──────────────────────────────────────────────────────────────

    private fun startDownload() {
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

        val quality = DownloadQuality.entries[qualitySpinner.selectedItemPosition]
        val format = DownloadFormat.entries[formatSpinner.selectedItemPosition]

        // Disable button during processing
        downloadButton.isEnabled = false
        downloadButton.text = "Processing…"
        processingLayout.visibility = View.VISIBLE
        processingText.text = "Analyzing link & fetching metadata..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    orchestrator.processLink(
                        link = link,
                        quality = quality,
                        format = format
                    )
                }

                // Success — clear input
                linkInput.text?.clear()
                Toast.makeText(this@MainActivity, R.string.download_started, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                downloadButton.isEnabled = true
                downloadButton.text = getString(R.string.download_button)
                processingLayout.visibility = View.GONE
            }
        }
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
}
