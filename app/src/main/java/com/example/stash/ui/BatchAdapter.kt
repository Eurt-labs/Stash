package com.example.stash.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stash.R
import com.example.stash.download.DownloadBatch
import com.example.stash.download.DownloadState
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * RecyclerView adapter for displaying download batches.
 */
class BatchAdapter : ListAdapter<DownloadBatch, BatchAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val batchTitle: TextView = view.findViewById(R.id.trackTitle)
        val batchSubtitle: TextView = view.findViewById(R.id.trackArtist)
        val statusBadge: TextView = view.findViewById(R.id.statusBadge)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val speedText: TextView = view.findViewById(R.id.speedText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    private var onItemClickListener: ((DownloadBatch) -> Unit)? = null

    fun setOnItemClickListener(listener: (DownloadBatch) -> Unit) {
        onItemClickListener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val batch = getItem(position)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(batch)
        }

        holder.batchTitle.text = batch.name
        holder.batchSubtitle.text = "${batch.completedTracks}/${batch.totalTracks} downloaded"

        val progressPercent = (batch.progress * 100).toInt()
        holder.progressBar.progress = progressPercent

        val context = holder.itemView.context
        when (batch.state) {
            DownloadState.QUEUED -> {
                holder.statusBadge.text = "QUEUED"
                holder.statusBadge.setTextColor(context.getColor(R.color.text_hint))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = 0
                holder.progressText.text = "Waiting…"
                holder.speedText.text = ""
            }
            DownloadState.DOWNLOADING -> {
                holder.statusBadge.text = "DOWNLOADING"
                holder.statusBadge.setTextColor(context.getColor(R.color.accent_primary))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = progressPercent
                holder.progressText.text = "$progressPercent%"
                holder.speedText.text = "${batch.completedTracks}/${batch.totalTracks}"
            }
            DownloadState.COMPLETE -> {
                holder.statusBadge.text = "DONE"
                holder.statusBadge.setTextColor(context.getColor(R.color.accent_success))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = 100
                holder.progressText.text = "All completed"
                holder.speedText.text = "✓"
            }
            DownloadState.FAILED -> {
                holder.statusBadge.text = "FAILED"
                holder.statusBadge.setTextColor(context.getColor(R.color.accent_error))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = progressPercent
                holder.progressText.text = "Failed/Cancelled"
                holder.speedText.text = ""
            }
            DownloadState.CANCELLED -> {
                holder.statusBadge.text = "CANCELLED"
                holder.statusBadge.setTextColor(context.getColor(R.color.text_hint))
                holder.progressBar.isIndeterminate = false
                holder.progressText.text = "Cancelled"
                holder.speedText.text = ""
            }
            else -> {
                holder.statusBadge.text = "QUEUED"
                holder.statusBadge.setTextColor(context.getColor(R.color.text_hint))
                holder.progressBar.isIndeterminate = false
                holder.progressText.text = ""
                holder.speedText.text = ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadBatch>() {
        override fun areItemsTheSame(oldItem: DownloadBatch, newItem: DownloadBatch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadBatch, newItem: DownloadBatch): Boolean {
            return oldItem.id == newItem.id &&
                   oldItem.progress == newItem.progress &&
                   oldItem.completedTracks == newItem.completedTracks &&
                   oldItem.totalTracks == newItem.totalTracks &&
                   oldItem.state == newItem.state
        }
    }
}
