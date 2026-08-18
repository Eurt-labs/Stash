export type Platform = 'youtube' | 'youtube_music' | 'other'

export type ContentType = 'track' | 'playlist' | 'album' | 'video'

export type DownloadQuality = '4K' | '2K' | 'HIGH' | 'MID' | 'LOW'

export type DownloadFormat = 'AUTO' | 'MP3' | 'AAC' | 'FLAC' | 'OPUS' | 'WAV' | 'MP4' | 'OTHER_VIDEO'

export type ColorTheme =
  | 'indigo'
  | 'emerald'
  | 'sunset'
  | 'sapphire'
  | 'amber'
  | 'crimson'
  | 'oled'
  | 'weeknd'
  | 'taylor'
  | 'billie'
  | 'daftpunk'
  | 'travis'
  | 'lana'

export type ThemeMode = 'dark' | 'light'

export type DownloadState = 
  | 'IDLE' 
  | 'QUEUED' 
  | 'FETCHING' 
  | 'DOWNLOADING' 
  | 'CONVERTING' 
  | 'TAGGING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'CANCELLED'

export interface TrackInfo {
  id: string
  title: string
  artists: string[]
  album?: string
  durationMs: number
  albumArtUrl?: string
  source: Platform
  sourceUrl: string
  youtubeUrl?: string
  releaseYear?: string
  trackNumber?: number
  genre?: string
  safeFileName: string
}

export interface ParsedLink {
  platform: Platform
  contentType: ContentType
  id: string
  originalUrl: string
}

export interface DownloadItem {
  id: string
  batchId: string
  trackInfo: TrackInfo
  quality: DownloadQuality
  format: DownloadFormat
  outputDir: string
  state: DownloadState
  progress: number
  speed: string
  eta: string
  statusMessage: string
  errorMessage?: string
  finalFilePath?: string
}

export interface DownloadBatch {
  id: string
  name: string
  items: DownloadItem[]
  outputDir: string
  quality: DownloadQuality
  format: DownloadFormat
  createdAt: number
  isCompleted: boolean
}

export interface DependencyStatus {
  ytDlpInstalled: boolean
  ytDlpVersion?: string
  ytDlpPath?: string
  ffmpegInstalled: boolean
  ffmpegVersion?: string
  ffmpegPath?: string
  ffprobeInstalled: boolean
  ffprobePath?: string
}

export interface AppUpdateStatus {
  currentVersion: string
  latestVersion?: string
  hasUpdate: boolean
  releaseUrl?: string
  releaseNotes?: string
  publishedAt?: string
  error?: string
}

export interface StashSettings {
  outputDir: string
  quality: DownloadQuality
  format: DownloadFormat
}

export interface StashAPI {
  // Config & Dependency APIs
  getSettings: () => Promise<StashSettings>
  setOutputDir: (dir: string) => Promise<string>
  setQuality: (quality: DownloadQuality) => Promise<void>
  setFormat: (format: DownloadFormat) => Promise<void>
  selectDirectory: (defaultPath?: string) => Promise<string | null>
  openDirectory: (dirPath: string) => Promise<void>
  openFile: (filePath: string) => Promise<void>
  openExternalUrl: (url: string) => Promise<void>
  checkDependencies: () => Promise<DependencyStatus>
  updateYtDlp: () => Promise<{ success: boolean; message: string }>
  checkAppUpdate: () => Promise<AppUpdateStatus>

  // Pipeline APIs
  parseAndFetchMetadata: (url: string) => Promise<{ parsedLink: ParsedLink; tracks: TrackInfo[] }>
  enqueueBatch: (name: string, tracks: TrackInfo[], quality?: DownloadQuality, format?: DownloadFormat, outputDir?: string) => Promise<DownloadBatch>
  startBatchDownload: (batchId: string) => Promise<void>
  startTrackDownload: (batchId: string, trackId: string) => Promise<void>
  cancelTrack: (batchId: string, trackId: string) => Promise<void>
  cancelBatch: (batchId: string) => Promise<void>
  removeTrack: (batchId: string, trackId: string) => Promise<void>
  removeBatch: (batchId: string) => Promise<void>
  clearCompletedBatches: () => Promise<void>
  getAllBatches: () => Promise<Record<string, DownloadBatch>>

  // Events & Subscriptions
  onBatchUpdated: (callback: (batches: Record<string, DownloadBatch>) => void) => () => void
  onFetchingStatus: (callback: (status: { isFetching: boolean; message: string }) => void) => () => void
  onToast: (callback: (toast: { type: 'success' | 'error' | 'info'; message: string }) => void) => () => void
}

declare global {
  interface Window {
    stashAPI: StashAPI
  }
}
