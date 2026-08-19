# 🔌 Stash IPC API Specification

Stash uses Electron's `contextBridge` to expose a type-safe IPC interface (`window.stashAPI`) to the React frontend.

---

## 📡 Methods (`window.stashAPI`)

### 1. `parseAndFetch(input: string)`
* **Description**: Parses a URL or search string, extracts metadata for tracks/playlists, and returns track details.
* **Returns**: `Promise<{ parsedLink: ParsedLink; tracks: TrackInfo[] }>`

### 2. `startDownload(options)`
* **Description**: Queues a batch of tracks for sequential download, transcode, and tagging.
* **Parameters**:
  * `batchId: string`
  * `tracks: TrackInfo[]`
  * `format: DownloadFormat` (`'MP3' | 'AAC' | 'FLAC' | 'OPUS' | 'WAV' | 'MP4'`)
  * `quality: DownloadQuality` (`'HIGH' | 'MID' | 'LOW' | '4K' | '2K' | 'LOSSLESS'`)
  * `outputDir: string`

### 3. `cancelDownload(downloadId: string)`
* **Description**: Immediately terminates an active child download/transcode process.

### 4. `getDependencyStatus(forceRefresh?: boolean)`
* **Description**: Checks whether `yt-dlp`, `ffmpeg`, and `ffprobe` are present and returns their versions.
* **Returns**: `Promise<DependencyStatus>`

### 5. `updateYtDlp()`
* **Description**: Triggers an on-demand update/bootstrap of `yt-dlp` to the latest nightly build.
* **Returns**: `Promise<{ success: boolean; message: string }>`

### 6. `checkForAppUpdates()`
* **Description**: Queries GitHub API for the latest published Stash desktop release.
* **Returns**: `Promise<AppUpdateInfo>`

### 7. `selectDirectory()`
* **Description**: Opens the native Windows folder picker dialog.
* **Returns**: `Promise<string | null>`

---

## 🔔 Event Listeners

* `onQueueUpdated(callback: (batches: DownloadBatch[]) => void)`
* `onFetchingStatusChanged(callback: (status: FetchingStatus) => void)`
* `onDirectorySelected(callback: (dir: string) => void)`
