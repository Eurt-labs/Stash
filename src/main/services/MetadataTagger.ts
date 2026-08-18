import fs from 'fs'
import path from 'path'
import os from 'os'
import axios from 'axios'
import NodeID3 from 'node-id3'
import { spawn } from 'child_process'
import { TrackInfo } from '../../shared/types'
import { DependencyResolver } from './DependencyResolver'

export class MetadataTagger {
  private static readonly MAX_ART_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB max

  /**
   * Tags an audio or video file with track metadata and embedded album artwork across all formats.
   */
  public static async tagFile(filePath: string, trackInfo: TrackInfo): Promise<void> {
    if (!fs.existsSync(filePath)) {
      console.warn(`Cannot tag non-existent file: ${filePath}`)
      return
    }

    const ext = path.extname(filePath).toLowerCase()
    let coverJpgPath: string | null = null
    let coverBuffer: Buffer | null = null

    // 1. Download and convert artwork to standard Baseline JPEG
    if (trackInfo.albumArtUrl) {
      const artResult = await this.prepareArtworkJpeg(trackInfo.albumArtUrl)
      if (artResult) {
        coverJpgPath = artResult.filePath
        coverBuffer = artResult.buffer
      }
    }

    try {
      if (ext === '.mp3') {
        await this.tagMp3(filePath, trackInfo, coverBuffer, coverJpgPath)
      } else if (ext === '.flac') {
        await this.tagFlac(filePath, trackInfo, coverJpgPath)
      } else if (ext === '.m4a' || ext === '.aac') {
        await this.tagM4a(filePath, trackInfo, coverJpgPath)
      } else if (ext === '.wav') {
        await this.tagWav(filePath, trackInfo)
      } else if (ext === '.opus' || ext === '.ogg') {
        await this.tagAudioGeneric(filePath, trackInfo)
      } else if (ext === '.mp4' || ext === '.mkv') {
        await this.tagVideo(filePath, trackInfo)
      }
    } finally {
      // Clean up temporary cover image file if created
      if (coverJpgPath && fs.existsSync(coverJpgPath)) {
        try {
          fs.unlinkSync(coverJpgPath)
        } catch (e) {
          // ignore
        }
      }
    }
  }

  /**
   * Tags MP3 files with ID3v2.3 tags and JPEG album art
   */
  private static async tagMp3(
    filePath: string,
    trackInfo: TrackInfo,
    coverBuffer: Buffer | null,
    coverJpgPath: string | null
  ): Promise<void> {
    try {
      const tags: NodeID3.Tags = {
        title: trackInfo.title,
        artist: trackInfo.artists.join(', '),
        album: trackInfo.album || trackInfo.title,
        year: trackInfo.releaseYear || undefined,
        trackNumber: trackInfo.trackNumber ? String(trackInfo.trackNumber) : undefined,
        genre: trackInfo.genre || undefined
      }

      if (coverBuffer) {
        tags.image = {
          mime: 'image/jpeg',
          type: { id: 3, name: 'front cover' },
          description: 'Cover Art',
          imageBuffer: coverBuffer
        }
      }

      const success = NodeID3.write(tags, filePath)
      if (!success && coverJpgPath) {
        // Fallback to FFmpeg tagging if NodeID3 reports false
        await this.tagWithFFmpeg(filePath, trackInfo, coverJpgPath, 'mp3')
      }
    } catch (err) {
      console.error(`NodeID3 failed for ${filePath}, falling back to FFmpeg:`, err)
      if (coverJpgPath) {
        await this.tagWithFFmpeg(filePath, trackInfo, coverJpgPath, 'mp3')
      }
    }
  }

  /**
   * Tags FLAC files with Vorbis comments and embedded picture block
   */
  private static async tagFlac(filePath: string, trackInfo: TrackInfo, coverJpgPath: string | null): Promise<void> {
    await this.tagWithFFmpeg(filePath, trackInfo, coverJpgPath, 'flac')
  }

  /**
   * Tags M4A/AAC files with iTunes atoms and cover art
   */
  private static async tagM4a(filePath: string, trackInfo: TrackInfo, coverJpgPath: string | null): Promise<void> {
    await this.tagWithFFmpeg(filePath, trackInfo, coverJpgPath, 'm4a')
  }

  /**
   * Tags WAV files with ID3v2 and RIFF INFO chunks
   */
  private static async tagWav(filePath: string, trackInfo: TrackInfo): Promise<void> {
    await this.tagWithFFmpeg(filePath, trackInfo, null, 'wav')
  }

  /**
   * Generic audio tagger for OPUS/OGG
   */
  private static async tagAudioGeneric(filePath: string, trackInfo: TrackInfo): Promise<void> {
    await this.tagWithFFmpeg(filePath, trackInfo, null, path.extname(filePath).replace('.', ''))
  }

  /**
   * Video metadata tagger
   */
  private static async tagVideo(filePath: string, trackInfo: TrackInfo): Promise<void> {
    await this.tagWithFFmpeg(filePath, trackInfo, null, path.extname(filePath).replace('.', ''))
  }

  /**
   * Universal FFmpeg metadata & cover art embedder
   */
  private static async tagWithFFmpeg(
    filePath: string,
    trackInfo: TrackInfo,
    coverJpgPath: string | null,
    formatExt: string
  ): Promise<void> {
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')
    const dir = path.dirname(filePath)
    const ext = path.extname(filePath)
    const tempOutput = path.join(dir, `tagged_${Date.now()}_${Math.random().toString(36).substring(2, 6)}${ext}`)

    const args: string[] = ['-i', filePath]

    if (coverJpgPath && fs.existsSync(coverJpgPath) && (formatExt === 'flac' || formatExt === 'm4a' || formatExt === 'mp3')) {
      args.push('-i', coverJpgPath)
      args.push('-map', '0:a', '-map', '1:v', '-c', 'copy')
      if (formatExt === 'flac' || formatExt === 'm4a') {
        args.push('-disposition:v:0', 'attached_pic')
      } else if (formatExt === 'mp3') {
        args.push('-id3v2_version', '3', '-metadata:s:v', 'title=Album cover', '-metadata:s:v', 'comment=Cover (front)')
      }
    } else {
      args.push('-c', 'copy')
    }

    if (formatExt === 'wav') {
      args.push('-write_id3v2', '1', '-write_bext', '1')
      args.push('-metadata', `INAM=${trackInfo.title}`)
      args.push('-metadata', `IART=${trackInfo.artists.join(', ')}`)
      args.push('-metadata', `IPRD=${trackInfo.album || trackInfo.title}`)
      if (trackInfo.releaseYear) args.push('-metadata', `ICRD=${trackInfo.releaseYear}`)
      if (trackInfo.genre) args.push('-metadata', `IGNR=${trackInfo.genre}`)
      if (trackInfo.trackNumber) args.push('-metadata', `ITRK=${trackInfo.trackNumber}`)
    }

    const artistStr = trackInfo.artists.join(', ')
    args.push('-metadata', `title=${trackInfo.title}`)
    args.push('-metadata', `artist=${artistStr}`)
    args.push('-metadata', `album_artist=${artistStr}`)
    args.push('-metadata', `album=${trackInfo.album || trackInfo.title}`)
    if (trackInfo.releaseYear) {
      args.push('-metadata', `date=${trackInfo.releaseYear}`)
      args.push('-metadata', `year=${trackInfo.releaseYear}`)
    }
    if (trackInfo.genre) {
      args.push('-metadata', `genre=${trackInfo.genre}`)
    }
    if (trackInfo.trackNumber) {
      args.push('-metadata', `track=${trackInfo.trackNumber}`)
    }

    // Also write standard uppercase Vorbis comments for FLAC/OGG
    if (formatExt === 'flac' || formatExt === 'ogg' || formatExt === 'opus') {
      args.push('-metadata', `TITLE=${trackInfo.title}`)
      args.push('-metadata', `ARTIST=${artistStr}`)
      args.push('-metadata', `ALBUM=${trackInfo.album || trackInfo.title}`)
      if (trackInfo.releaseYear) args.push('-metadata', `DATE=${trackInfo.releaseYear}`)
    }

    args.push('-y', tempOutput)

    return new Promise<void>((resolve) => {
      const child = spawn(ffmpegPath, args, { windowsHide: true })
      child.on('close', (code) => {
        if (code === 0 && fs.existsSync(tempOutput)) {
          try {
            fs.unlinkSync(filePath)
            fs.renameSync(tempOutput, filePath)
          } catch (e) {
            console.error('Error replacing file with tagged version:', e)
          }
        } else {
          if (fs.existsSync(tempOutput)) {
            try { fs.unlinkSync(tempOutput) } catch (err) {}
          }
        }
        resolve()
      })
      child.on('error', (err) => {
        console.error('FFmpeg tagging error:', err)
        resolve()
      })
    })
  }

  /**
   * Downloads thumbnail and converts it into a clean square baseline JPEG using FFmpeg
   */
  private static async prepareArtworkJpeg(url: string): Promise<{ filePath: string; buffer: Buffer } | null> {
    try {
      const response = await axios.get(url, {
        responseType: 'arraybuffer',
        timeout: 10000,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
      })

      if (response.status !== 200 || !response.data) {
        return null
      }

      const rawBuffer = Buffer.from(response.data)
      const tempId = `art_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`
      const rawTempPath = path.join(os.tmpdir(), `${tempId}_raw`)
      const jpgTempPath = path.join(os.tmpdir(), `${tempId}.jpg`)

      fs.writeFileSync(rawTempPath, rawBuffer)

      // Use FFmpeg to convert image (WebP, PNG, etc.) to 600x600 Baseline JPEG
      const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')
      await new Promise<void>((resolve) => {
        const child = spawn(
          ffmpegPath,
          ['-i', rawTempPath, '-vf', 'scale=600:600:force_original_aspect_ratio=increase,crop=600:600', '-q:v', '2', '-y', jpgTempPath],
          { windowsHide: true }
        )
        child.on('close', () => resolve())
        child.on('error', () => resolve())
      })

      // Clean up raw temp file
      try {
        fs.unlinkSync(rawTempPath)
      } catch (e) {}

      if (fs.existsSync(jpgTempPath)) {
        const jpgBuffer = fs.readFileSync(jpgTempPath)
        return {
          filePath: jpgTempPath,
          buffer: jpgBuffer
        }
      }

      return null
    } catch (err) {
      console.warn(`Failed to prepare artwork from ${url}:`, err)
      return null
    }
  }
}
