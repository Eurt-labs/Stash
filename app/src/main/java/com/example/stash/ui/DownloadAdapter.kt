package com.example.stash.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stash.R
import com.example.stash.download.DownloadItem
import com.example.stash.download.DownloadState
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * RecyclerView adapter for displaying download items with live progress.
 */
class DownloadAdapter : ListAdapter<DownloadItem, DownloadAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val trackTitle: TextView = view.findViewById(R.id.trackTitle)
        val trackArtist: TextView = view.findViewById(R.id.trackArtist)
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

    private var onItemClickListener: ((DownloadItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (DownloadItem) -> Unit) {
        onItemClickListener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item)
        }

        holder.trackTitle.text = item.trackInfo.title
        holder.trackArtist.text = item.trackInfo.artists.joinToString(", ")

        // Progress bar
        val progressPercent = (item.progress * 100).toInt()
        holder.progressBar.progress = progressPercent

        when (item.state) {
            DownloadState.QUEUED -> {
                holder.statusBadge.text = "QUEUED"
                holder.statusBadge.setTextColor(color(holder, R.color.text_hint))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = 0
                holder.progressText.text = "Waiting…"
                holder.speedText.text = ""
            }
            DownloadState.SEARCHING -> {
                holder.statusBadge.text = "SEARCHING"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_warning))
                holder.progressBar.isIndeterminate = true
                holder.progressText.text = "Finding on YouTube…"
                holder.speedText.text = ""
            }
            DownloadState.DOWNLOADING -> {
                holder.statusBadge.text = "DOWNLOADING"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_primary))
                if (progressPercent < 0) {
                    holder.progressBar.isIndeterminate = true
                    holder.progressText.text = "Starting…"
                } else {
                    holder.progressBar.isIndeterminate = false
                    holder.progressBar.progress = progressPercent
                    holder.progressText.text = "$progressPercent%"
                }
                holder.speedText.text = item.speed ?: ""
            }
            DownloadState.CONVERTING -> {
                holder.statusBadge.text = "CONVERTING"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_primary_light))
                holder.progressBar.isIndeterminate = true
                holder.progressText.text = "Converting audio…"
                holder.speedText.text = ""
            }
            DownloadState.TAGGING -> {
                holder.statusBadge.text = "TAGGING"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_primary_light))
                holder.progressBar.isIndeterminate = true
                holder.progressText.text = "Adding metadata…"
                holder.speedText.text = ""
            }
            DownloadState.COMPLETE -> {
                holder.statusBadge.text = "DONE"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_success))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = 100
                holder.progressText.text = "Download complete"
                holder.speedText.text = "✓"
            }
            DownloadState.FAILED -> {
                holder.statusBadge.text = "FAILED"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_error))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = 0
                holder.progressText.text = item.error ?: "Unknown error"
                holder.speedText.text = ""
            }
            DownloadState.CANCELLED -> {
                holder.statusBadge.text = "CANCELLED"
                holder.statusBadge.setTextColor(color(holder, R.color.text_hint))
                holder.progressBar.isIndeterminate = false
                holder.progressText.text = "Cancelled"
                holder.speedText.text = ""
            }
            DownloadState.PAUSED -> {
                holder.statusBadge.text = "PAUSED"
                holder.statusBadge.setTextColor(color(holder, R.color.accent_warning))
                holder.progressBar.isIndeterminate = false
                holder.progressBar.progress = progressPercent
                holder.progressText.text = "$progressPercent% — Paused"
                holder.speedText.text = ""
            }
        }
    }

    private fun color(holder: ViewHolder, colorRes: Int): Int {
        return holder.itemView.context.getColor(colorRes)
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem == newItem
        }
    }
}
