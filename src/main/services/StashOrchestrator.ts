import path from 'path'
import fs from 'fs'
import crypto from 'crypto'
import {
  TrackInfo,
  DownloadBatch,
  DownloadItem,
  DownloadQuality,
  DownloadFormat,
  DownloadState,
  ParsedLink
} from '../../shared/types'
import { LinkParser } from './LinkParser'
import { DownloadEngine } from './DownloadEngine'
import { ConversionEngine } from './ConversionEngine'
import { MetadataTagger } from './MetadataTagger'
import { FileManager } from './FileManager'

export class StashOrchestrator {
  private outputDir: string = FileManager.getDefaultDownloadDir()
  private quality: DownloadQuality = 'HIGH'
  private format: DownloadFormat = 'AUTO'

  private batches: Map<string, DownloadBatch> = new Map()
  private isProcessingQueue = false
  private downloadEngine = new DownloadEngine()
  private conversionEngine = new ConversionEngine()

  // Event callbacks
  public onBatchesChanged?: (batches: Record<string, DownloadBatch>) => void
  public onFetchingStatusChanged?: (status: { isFetching: boolean; message: string }) => void
  public onToastMessage?: (toast: { type: 'success' | 'error' | 'info'; message: string }) => void

  public getSettings() {
    return {
      outputDir: this.outputDir,
      quality: this.quality,
      format: this.format
    }
  }

  public setOutputDirectory(dir: string): string {
    if (!fs.existsSync(dir)) {
      try {
        fs.mkdirSync(dir, { recursive: true })
      } catch (e) {
        console.error('Failed to create custom directory:', e)
        return this.outputDir
      }
    }
    this.outputDir = dir
    return this.outputDir
  }

  public setQuality(quality: DownloadQuality): void {
    this.quality = quality
  }

  public setFormat(format: DownloadFormat): void {
    this.format = format
  }

  public getAllBatches(): Record<string, DownloadBatch> {
    const obj: Record<string, DownloadBatch> = {}
    for (const [id, batch] of this.batches.entries()) {
      obj[id] = { ...batch, items: [...batch.items] }
    }
    return obj
  }

  private notifyUpdate(): void {
    if (this.onBatchesChanged) {
      this.onBatchesChanged(this.getAllBatches())
    }
  }

  /**
   * Phase 1: Fetch metadata
   */
  public async fetchMetadata(link: string): Promise<{ parsedLink: ParsedLink; tracks: TrackInfo[] }> {
    const parsed = LinkParser.parse(link)
    if (!parsed) {
      throw new Error('Unsupported or invalid link format')
    }

    if (this.onFetchingStatusChanged) {
      this.onFetchingStatusChanged({ isFetching: true, message: 'Analyzing source link...' })
    }

    try {
      let fetchUrl = parsed.originalUrl
      const isPlaylistOrSearch = parsed.contentType === 'playlist' || parsed.contentType === 'album' || parsed.originalUrl.startsWith('ytsearch')

      if (this.onFetchingStatusChanged) {
        this.onFetchingStatusChanged({ isFetching: true, message: `Querying metadata from ${parsed.platform}...` })
      }

      const tracks = await this.downloadEngine.extractInfo(fetchUrl, false, (count) => {
        if (this.onFetchingStatusChanged) {
          this.onFetchingStatusChanged({ isFetching: true, message: `Discovered ${count} track(s)...` })
        }
      })

      if (tracks.length === 0) {
        throw new Error('No playable media found for this link')
      }

      return { parsedLink: parsed, tracks }
    } finally {
      if (this.onFetchingStatusChanged) {
        this.onFetchingStatusChanged({ isFetching: false, message: '' })
      }
    }
  }

  /**
   * Enqueues tracks into a new batch
   */
  public enqueueBatch(
    name: string,
    tracks: TrackInfo[],
    quality: DownloadQuality = this.quality,
    format: DownloadFormat = this.format,
    outputDir: string = this.outputDir
  ): DownloadBatch {
    if (!tracks || tracks.length === 0) {
      throw new Error('Cannot enqueue empty track list')
    }

    const batchId = `batch_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`
    const items: DownloadItem[] = tracks.map((track, idx) => {
      const itemId = `item_${batchId}_${idx}_${track.id}`
      return {
        id: itemId,
        batchId,
        trackInfo: track,
        quality,
        format,
        outputDir,
        state: 'QUEUED',
        progress: 0,
        speed: '',
        eta: '',
        statusMessage: 'In queue'
      }
    })

    const batch: DownloadBatch = {
      id: batchId,
      name,
      items,
      outputDir,
      quality,
      format,
      createdAt: Date.now(),
      isCompleted: false
    }

    this.batches.set(batchId, batch)
    this.notifyUpdate()

    // Trigger queue runner asynchronously
    this.processQueue().catch((err) => console.error('Queue runner error:', err))

    return batch
  }

  /**
   * Main 5-Phase Queue Execution Runner
   */
  public async processQueue(): Promise<void> {
    if (this.isProcessingQueue) return
    this.isProcessingQueue = true

    const cacheDir = FileManager.getCacheDir()

    try {
      while (true) {
        // Find next queued item across batches
        let targetBatch: DownloadBatch | null = null
        let targetItem: DownloadItem | null = null

        for (const batch of this.batches.values()) {
          const item = batch.items.find((i) => i.state === 'QUEUED')
          if (item) {
            targetBatch = batch
            targetItem = item
            break
          }
        }

        if (!targetBatch || !targetItem) {
          break // All done
        }

        const batch = targetBatch
        const item = targetItem
        const track = item.trackInfo

        try {
          // Resolve Format
          let resolvedFormat = item.format
          if (resolvedFormat === 'AUTO') {
            if (track.source === 'youtube_music' || track.sourceUrl.startsWith('ytsearch')) {
              resolvedFormat = 'MP3'
            } else {
              resolvedFormat = 'MP4'
            }
          }

          const targetUrl = track.youtubeUrl || track.sourceUrl

          // ── Phase 2: DOWNLOAD ──
          item.state = 'DOWNLOADING'
          item.statusMessage = 'Downloading audio/video stream...'
          item.progress = 0
          this.notifyUpdate()

          const downloadedRawPath = await this.downloadEngine.download(
            targetUrl,
            track,
            cacheDir,
            item.quality,
            resolvedFormat,
            item.id,
            (percent, speed, eta) => {
              item.progress = percent
              item.speed = speed
              item.eta = eta
              item.statusMessage = `Downloading: ${percent.toFixed(1)}%`
              this.notifyUpdate()
            }
          )

          // ── Phase 3: CONVERT ──
          item.state = 'CONVERTING'
          item.statusMessage = `Transcoding to ${resolvedFormat}...`
          item.progress = 100
          item.speed = ''
          item.eta = ''
          this.notifyUpdate()

          const convertedPath = await this.conversionEngine.convert(
            downloadedRawPath,
            resolvedFormat,
            item.quality,
            item.id,
            (fraction) => {
              item.statusMessage = `Transcoding: ${(fraction * 100).toFixed(0)}%`
              this.notifyUpdate()
            }
          )

          // ── Phase 4: TAG & MOVE ──
          item.state = 'TAGGING'
          item.statusMessage = 'Embedding metadata & album artwork...'
          this.notifyUpdate()

          await MetadataTagger.tagFile(convertedPath, track)

          // Move to user destination folder
          const finalPath = FileManager.moveFile(convertedPath, batch.outputDir)
          item.state = 'COMPLETED'
          item.statusMessage = 'Completed'
          item.progress = 100
          item.finalFilePath = finalPath
          this.notifyUpdate()

        } catch (err: any) {
          if (item.state !== 'CANCELLED') {
            item.state = 'FAILED'
            item.statusMessage = 'Failed'
            item.errorMessage = err?.message || 'Unknown download error'
            console.error(`Error processing track ${track.title}:`, err)
            this.notifyUpdate()
          }
        }

        // ── Phase 5: CLEANUP ──
        // Check if all items in this batch are completed/failed/cancelled
        const remainingInBatch = batch.items.filter((i) => i.state === 'QUEUED' || i.state === 'DOWNLOADING' || i.state === 'CONVERTING' || i.state === 'TAGGING')
        if (remainingInBatch.length === 0) {
          batch.isCompleted = true
          this.notifyUpdate()
        }
      }
    } finally {
      this.isProcessingQueue = false
    }
  }

  public async startTrackDownload(batchId: string, trackId: string): Promise<void> {
    const batch = this.batches.get(batchId)
    if (!batch) return
    const item = batch.items.find((i) => i.id === trackId)
    if (!item) return

    item.state = 'QUEUED'
    item.statusMessage = 'In queue'
    item.errorMessage = undefined
    item.progress = 0
    batch.isCompleted = false
    this.notifyUpdate()

    this.processQueue().catch((err) => console.error(err))
  }

  public async startBatchDownload(batchId: string): Promise<void> {
    const batch = this.batches.get(batchId)
    if (!batch) return

    for (const item of batch.items) {
      if (item.state === 'FAILED' || item.state === 'CANCELLED' || item.state === 'IDLE') {
        item.state = 'QUEUED'
        item.statusMessage = 'In queue'
        item.errorMessage = undefined
        item.progress = 0
      }
    }
    batch.isCompleted = false
    this.notifyUpdate()

    this.processQueue().catch((err) => console.error(err))
  }

  public async cancelTrack(batchId: string, trackId: string): Promise<void> {
    const batch = this.batches.get(batchId)
    if (!batch) return
    const item = batch.items.find((i) => i.id === trackId)
    if (!item) return

    if (item.state === 'DOWNLOADING') {
      this.downloadEngine.cancelDownload(item.id)
    } else if (item.state === 'CONVERTING') {
      this.conversionEngine.cancelConversion(item.id)
    }

    item.state = 'CANCELLED'
    item.statusMessage = 'Cancelled'
    this.notifyUpdate()
  }

  public async cancelBatch(batchId: string): Promise<void> {
    const batch = this.batches.get(batchId)
    if (!batch) return

    for (const item of batch.items) {
      if (item.state === 'DOWNLOADING') {
        this.downloadEngine.cancelDownload(item.id)
      } else if (item.state === 'CONVERTING') {
        this.conversionEngine.cancelConversion(item.id)
      }
      if (item.state !== 'COMPLETED') {
        item.state = 'CANCELLED'
        item.statusMessage = 'Cancelled'
      }
    }
    batch.isCompleted = true
    this.notifyUpdate()
  }

  public async removeTrack(batchId: string, trackId: string): Promise<void> {
    const batch = this.batches.get(batchId)
    if (!batch) return

    await this.cancelTrack(batchId, trackId)
    batch.items = batch.items.filter((i) => i.id !== trackId)
    if (batch.items.length === 0) {
      this.batches.delete(batchId)
    }
    this.notifyUpdate()
  }

  public async removeBatch(batchId: string): Promise<void> {
    await this.cancelBatch(batchId)
    this.batches.delete(batchId)
    this.notifyUpdate()
  }

  public async clearCompletedBatches(): Promise<void> {
    for (const [id, batch] of this.batches.entries()) {
      const activeItems = batch.items.filter((i) => i.state !== 'COMPLETED' && i.state !== 'CANCELLED')
      if (activeItems.length === 0) {
        this.batches.delete(id)
      }
    }
    this.notifyUpdate()
  }
}
