package com.example.stash.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stash.R
import com.example.stash.StashApplication
import com.example.stash.download.DownloadItem
import com.example.stash.download.DownloadState
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap

class BatchDetailActivity : AppCompatActivity() {

    private lateinit var adapter: DownloadAdapter
    private lateinit var toolbarTitle: TextView
    private lateinit var batchNameText: TextView
    private lateinit var batchStatusText: TextView
    private lateinit var batchProgressBar: LinearProgressIndicator
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: ImageButton

    private var batchId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_detail)

        batchId = intent.getStringExtra("BATCH_ID")
        if (batchId == null) {
            finish()
            return
        }

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        observeBatch()
    }

    private fun bindViews() {
        toolbarTitle = findViewById(R.id.toolbarTitle)
        batchNameText = findViewById(R.id.batchNameText)
        batchStatusText = findViewById(R.id.batchStatusText)
        batchProgressBar = findViewById(R.id.batchProgressBar)
        recyclerView = findViewById(R.id.tracksRecyclerView)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter()
        adapter.setOnItemClickListener { item ->
            // Open the downloaded file if it is complete
            if (item.state == DownloadState.COMPLETE && item.filePath != null) {
                openDownloadedFile(item.filePath)
            } else if (item.state == DownloadState.FAILED) {
                Toast.makeText(this, "Download failed: ${item.error}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Download state: ${item.state}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun observeBatch() {
        lifecycleScope.launch {
            StashApplication.orchestrator.downloadBatches.collectLatest { batches ->
                val batch = batches[batchId]
                if (batch == null) {
                    finish()
                    return@collectLatest
                }

                toolbarTitle.text = batch.name
                batchNameText.text = batch.name
                
                val status = when (batch.state) {
                    DownloadState.QUEUED -> "Queued"
                    DownloadState.DOWNLOADING -> "Downloading (${batch.completedTracks}/${batch.totalTracks} complete)"
                    DownloadState.COMPLETE -> "Complete"
                    DownloadState.FAILED -> "Failed (${batch.failedTracks} failed)"
                    else -> batch.state.name
                }
                batchStatusText.text = "Status: $status"
                batchProgressBar.progress = (batch.progress * 100).toInt()

                adapter.submitList(batch.items)
            }
        }
    }

    private fun openDownloadedFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "File does not exist: $filePath", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val mime = contentResolver.getType(uri) 
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open file with"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
