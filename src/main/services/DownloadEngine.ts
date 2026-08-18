import { spawn, ChildProcess } from 'child_process'
import path from 'path'
import fs from 'fs'
import readline from 'readline'
import { TrackInfo, Platform, DownloadQuality, DownloadFormat } from '../../shared/types'
import { DependencyResolver } from './DependencyResolver'
import { FileManager } from './FileManager'

export class DownloadEngine {
  private activeProcesses: Map<string, ChildProcess> = new Map()

  /**
   * Extracts metadata using `yt-dlp --dump-json`
   */
  public async extractInfo(
    url: string,
    flatPlaylist = false,
    onTrackExtracted?: (count: number) => void
  ): Promise<TrackInfo[]> {
    const ytDlpPath = DependencyResolver.resolveExecutable('yt-dlp')
    const args = [
      '--dump-json',
      '--no-download',
      '--no-warnings',
      '--no-check-certificates',
      '--socket-timeout', '30',
      '--ignore-errors',
      '--no-abort-on-error'
    ]
    
    if (flatPlaylist) {
      args.push('--flat-playlist')
    }
    args.push(url)

    return new Promise<TrackInfo[]>((resolve, reject) => {
      const child = spawn(ytDlpPath, args, { windowsHide: true })
      const tracks: TrackInfo[] = []
      const errorLines: string[] = []

      const rl = readline.createInterface({ input: child.stdout })

      rl.on('line', (line) => {
        const trimmed = line.trim()
        if (trimmed.startsWith('{')) {
          try {
            const data = JSON.parse(trimmed)
            const track = this.jsonToTrackInfo(data, url)
            tracks.push(track)
            if (onTrackExtracted) {
              onTrackExtracted(tracks.length)
            }
          } catch (e) {
            console.error('Error parsing JSON track info:', e)
          }
        }
      })

      child.stderr.on('data', (data) => {
        const str = data.toString()
        if (str.includes('ERROR:') || errorLines.length < 5) {
          errorLines.push(str)
        }
      })

      child.on('close', (code) => {
        if (code === 0 || tracks.length > 0) {
          resolve(tracks)
        } else {
          const errMsg = errorLines.join('\n').trim() || `yt-dlp exited with code ${code}`
          reject(new Error(`Failed to fetch metadata: ${errMsg}`))
        }
      })

      child.on('error', (err) => {
        reject(new Error(`Failed to launch yt-dlp: ${err.message}`))
      })
    })
  }

  /**
   * Downloads raw stream via yt-dlp
   */
  public async download(
    url: string,
    trackInfo: TrackInfo,
    outputDir: string,
    quality: DownloadQuality,
    format: DownloadFormat,
    downloadId: string,
    onProgress?: (percent: number, speed: string, eta: string) => void
  ): Promise<string> {
    const ytDlpPath = DependencyResolver.resolveExecutable('yt-dlp')
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')

    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true })
    }

    const outputTemplate = path.join(outputDir, `${trackInfo.safeFileName}.%(ext)s`)

    const args = [
      '-o', outputTemplate,
      '--no-check-certificates',
      '--no-warnings',
      '--socket-timeout', '30',
      '--retries', '5',
      '--fragment-retries', '5',
      '--fixup', 'never',
      '--newline',
      '--no-playlist',
      '--geo-bypass',
      '--user-agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
    ]

    // Pass ffmpeg directory location so yt-dlp can locate ffmpeg on all environments
    if (fs.existsSync(ffmpegPath)) {
      const ffmpegDir = path.dirname(ffmpegPath)
      args.push('--ffmpeg-location', ffmpegDir)
    }

    if (format === 'MP4' || format === 'OTHER_VIDEO') {
      if (quality === 'LOW') {
        args.push('-f', 'bv*[height<=480]+ba/b[height<=480]/bv*+ba/b')
      } else if (quality === 'MID') {
        args.push('-f', 'bv*[height<=720]+ba/b[height<=720]/bv*+ba/b')
      } else if (quality === 'HIGH') {
        args.push('-f', 'bv*[height<=1080]+ba/b[height<=1080]/bv*+ba/b')
      } else if (quality === '2K') {
        args.push('-f', 'bv*[height<=1440]+ba/b[height<=1440]/bv*+ba/b')
      } else {
        // 4K: Ultra-HD 4K (up to 2160p / 4320p 8K 60fps) + best audio stream
        args.push('-f', 'bv*+ba/b')
      }
      args.push('--merge-output-format', 'mp4')
    } else {
      // Audio: fetch best audio stream without forced early conversion; ConversionEngine handles 320k transcode in Phase 3
      args.push('-f', 'ba/b')
    }

    args.push(url)

    return new Promise<string>((resolve, reject) => {
      const child = spawn(ytDlpPath, args, { windowsHide: true })
      this.activeProcesses.set(downloadId, child)

      const rl = readline.createInterface({ input: child.stdout })

      rl.on('line', (line) => {
        if (line.includes('[download]')) {
          const percent = this.parsePercent(line)
          const { speed, eta } = this.parseSpeedAndEta(line)
          if (percent !== null && onProgress) {
            onProgress(percent, speed, eta)
          }
        }
      })

      let stderrOutput = ''
      child.stderr.on('data', (data) => {
        stderrOutput += data.toString()
      })

      child.on('close', (code) => {
        this.activeProcesses.delete(downloadId)
        if (code === 0) {
          const outputFile = FileManager.findOutputFile(outputDir, trackInfo.safeFileName)
          if (outputFile) {
            resolve(outputFile)
          } else {
            reject(new Error('Download finished but output file was not found'))
          }
        } else {
          reject(new Error(`yt-dlp download failed with code ${code}: ${stderrOutput.slice(-300)}`))
        }
      })

      child.on('error', (err) => {
        this.activeProcesses.delete(downloadId)
        reject(new Error(`yt-dlp process error: ${err.message}`))
      })
    })
  }

  /**
   * Cancels an active download process
   */
  public cancelDownload(downloadId: string): void {
    const proc = this.activeProcesses.get(downloadId)
    if (proc && !proc.killed) {
      try {
        proc.kill('SIGTERM')
      } catch (e) {
        try {
          proc.kill('SIGKILL')
        } catch (err) {
          // ignore
        }
      }
      this.activeProcesses.delete(downloadId)
    }
  }

  private jsonToTrackInfo(json: any, sourceUrl: string): TrackInfo {
    const title = json.title || 'Unknown Title'
    const rawArtist = json.artist || json.channel || json.uploader || 'Unknown Artist'
    const album = json.album || undefined
    const durationSec = typeof json.duration === 'number' ? json.duration : 0
    const durationMs = Math.round(durationSec * 1000)

    let thumbnail: string | undefined = json.thumbnail
    if (!thumbnail && Array.isArray(json.thumbnails) && json.thumbnails.length > 0) {
      const bestThumb = json.thumbnails[json.thumbnails.length - 1]
      thumbnail = bestThumb.url || bestThumb
    }

    let detectedPlatform: Platform = 'other'
    if (sourceUrl.includes('music.youtube.com')) {
      detectedPlatform = 'youtube_music'
    } else if (sourceUrl.includes('youtube.com') || sourceUrl.includes('youtu.be')) {
      detectedPlatform = 'youtube'
    }

    const videoId = json.id
    const videoUrl = json.webpage_url || (videoId ? `https://www.youtube.com/watch?v=${videoId}` : sourceUrl)

    const rawFileName = `${rawArtist} - ${title}`
    const safeFileName = FileManager.sanitizeFileName(rawFileName)

    return {
      id: videoId || Math.random().toString(36).substring(2, 10),
      title,
      artists: [rawArtist],
      album,
      durationMs,
      albumArtUrl: thumbnail,
      source: detectedPlatform,
      sourceUrl,
      youtubeUrl: detectedPlatform === 'other' ? undefined : videoUrl,
      releaseYear: json.release_year ? String(json.release_year) : (json.upload_date ? json.upload_date.substring(0, 4) : undefined),
      trackNumber: typeof json.track_number === 'number' ? json.track_number : undefined,
      genre: json.genre || undefined,
      safeFileName
    }
  }

  private parsePercent(line: string): number | null {
    const match = line.match(/([0-9.]+)%/)
    if (match && match[1]) {
      return parseFloat(match[1])
    }
    return null
  }

  private parseSpeedAndEta(line: string): { speed: string; eta: string } {
    let speed = ''
    let eta = ''

    const speedMatch = line.match(/at\s+([0-9.]+\s*[a-zA-Z/]+)/i)
    if (speedMatch && speedMatch[1]) {
      speed = speedMatch[1]
    }

    const etaMatch = line.match(/ETA\s+([0-9:]+)/i)
    if (etaMatch && etaMatch[1]) {
      eta = etaMatch[1]
    }

    return { speed, eta }
  }
}
