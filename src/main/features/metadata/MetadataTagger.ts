import path from 'path'
import fs from 'fs'
import axios from 'axios'
import NodeID3 from 'node-id3'
import { spawn } from 'child_process'
import { TrackInfo } from '../../../shared/types'
import { DependencyResolver } from '../updater/DependencyResolver'

export class MetadataTagger {
  /**
   * Embeds ID3 / RIFF / Vorbis tags and cover artwork into media file
   */
  public static async tagFile(filePath: string, trackInfo: TrackInfo): Promise<string> {
    const ext = path.extname(filePath).toLowerCase()

    let imageBuffer: Buffer | undefined
    if (trackInfo.albumArtUrl) {
      try {
        const response = await axios.get(trackInfo.albumArtUrl, {
          responseType: 'arraybuffer',
          timeout: 10000
        })
        if (response.status === 200) {
          imageBuffer = Buffer.from(response.data)
        }
      } catch (err) {
        console.warn(`[MetadataTagger] Could not download cover artwork:`, err)
      }
    }

    if (ext === '.mp3') {
      return this.tagMp3(filePath, trackInfo, imageBuffer)
    } else if (ext === '.wav') {
      return this.tagWav(filePath, trackInfo, imageBuffer)
    } else if (ext === '.flac') {
      return this.tagFlac(filePath, trackInfo, imageBuffer)
    } else if (ext === '.m4a' || ext === '.mp4' || ext === '.opus') {
      return this.tagViaFfmpeg(filePath, trackInfo, imageBuffer)
    }

    return filePath
  }

  private static tagMp3(filePath: string, trackInfo: TrackInfo, imageBuffer?: Buffer): Promise<string> {
    return new Promise((resolve) => {
      const tags: NodeID3.Tags = {
        title: trackInfo.title,
        artist: trackInfo.artists.join(', '),
        album: trackInfo.album || trackInfo.title,
        trackNumber: trackInfo.trackNumber ? String(trackInfo.trackNumber) : undefined,
        genre: trackInfo.genre || undefined
      }

      if (imageBuffer) {
        tags.image = {
          mime: 'image/jpeg',
          type: { id: 3, name: 'front cover' },
          description: 'Front Cover',
          imageBuffer
        }
      }

      NodeID3.write(tags, filePath, (err) => {
        if (err) {
          console.warn('[MetadataTagger] node-id3 write error:', err)
        }
        resolve(filePath)
      })
    })
  }

  private static tagWav(filePath: string, trackInfo: TrackInfo, imageBuffer?: Buffer): Promise<string> {
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')
    const dir = path.dirname(filePath)
    const baseName = path.basename(filePath, '.wav')
    const tempOut = path.join(dir, `${baseName}_tagged.wav`)

    const artistStr = trackInfo.artists.join(', ')
    const albumStr = trackInfo.album || trackInfo.title

    const args = [
      '-y',
      '-i', filePath,
      '-c', 'copy',
      '-metadata', `title=${trackInfo.title}`,
      '-metadata', `artist=${artistStr}`,
      '-metadata', `album_artist=${artistStr}`,
      '-metadata', `album=${albumStr}`,
      '-metadata', `INAM=${trackInfo.title}`,
      '-metadata', `IART=${artistStr}`,
      '-metadata', `IPRD=${albumStr}`,
      '-metadata', `ICRD=${new Date().getFullYear()}`,
      '-metadata', `IGNR=${trackInfo.genre || 'Music'}`,
      '-write_id3v2', '1',
      '-write_bext', '1'
    ]

    if (trackInfo.trackNumber) {
      args.push('-metadata', `track=${trackInfo.trackNumber}`)
      args.push('-metadata', `ITRK=${trackInfo.trackNumber}`)
    }

    args.push(tempOut)

    return new Promise((resolve) => {
      const child = spawn(ffmpegPath, args, { windowsHide: true })
      child.on('close', (code) => {
        if (code === 0 && fs.existsSync(tempOut)) {
          try {
            fs.unlinkSync(filePath)
            fs.renameSync(tempOut, filePath)
          } catch (e) {
            console.warn('[MetadataTagger] WAV file swap failed:', e)
          }
        } else {
          if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        }
        resolve(filePath)
      })
      child.on('error', () => {
        if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        resolve(filePath)
      })
    })
  }

  private static async tagFlac(filePath: string, trackInfo: TrackInfo, imageBuffer?: Buffer): Promise<string> {
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')
    const dir = path.dirname(filePath)
    const baseName = path.basename(filePath, '.flac')
    const tempOut = path.join(dir, `${baseName}_tagged.flac`)

    let coverFile: string | null = null
    let normalizedCover: string | null = null

    if (imageBuffer) {
      coverFile = path.join(dir, `_temp_cover_${Date.now()}.bin`)
      normalizedCover = path.join(dir, `_temp_cover_${Date.now()}.jpg`)
      fs.writeFileSync(coverFile, imageBuffer)

      await new Promise<void>((done) => {
        const normProc = spawn(
          ffmpegPath,
          ['-y', '-i', coverFile!, '-vf', 'scale=600:600:force_original_aspect_ratio=increase,crop=600:600', '-q:v', '2', normalizedCover!],
          { windowsHide: true }
        )
        normProc.on('close', () => done())
        normProc.on('error', () => done())
      })
    }

    const artistStr = trackInfo.artists.join(', ')
    const albumStr = trackInfo.album || trackInfo.title

    const args = ['-y', '-i', filePath]

    if (normalizedCover && fs.existsSync(normalizedCover)) {
      args.push('-i', normalizedCover)
      args.push('-map', '0:a', '-map', '1:0')
      args.push('-c:v', 'mjpeg')
      args.push('-disposition:v:0', 'attached_pic')
      args.push('-metadata:s:v', 'title=Album cover')
      args.push('-metadata:s:v', 'comment=Cover (front)')
    } else {
      args.push('-map', '0:a')
    }

    args.push('-c:a', 'copy')
    args.push('-metadata', `TITLE=${trackInfo.title}`)
    args.push('-metadata', `title=${trackInfo.title}`)
    args.push('-metadata', `ARTIST=${artistStr}`)
    args.push('-metadata', `artist=${artistStr}`)
    args.push('-metadata', `ALBUM=${albumStr}`)
    args.push('-metadata', `album=${albumStr}`)
    args.push('-metadata', `ALBUMARTIST=${artistStr}`)
    args.push('-metadata', `album_artist=${artistStr}`)
    args.push('-metadata', `DATE=${new Date().getFullYear()}`)
    args.push('-metadata', `GENRE=${trackInfo.genre || 'Music'}`)

    if (trackInfo.trackNumber) {
      args.push('-metadata', `TRACKNUMBER=${trackInfo.trackNumber}`)
      args.push('-metadata', `track=${trackInfo.trackNumber}`)
    }

    args.push(tempOut)

    return new Promise((resolve) => {
      const child = spawn(ffmpegPath, args, { windowsHide: true })
      child.on('close', (code) => {
        if (coverFile && fs.existsSync(coverFile)) fs.unlinkSync(coverFile)
        if (normalizedCover && fs.existsSync(normalizedCover)) fs.unlinkSync(normalizedCover)

        if (code === 0 && fs.existsSync(tempOut)) {
          try {
            fs.unlinkSync(filePath)
            fs.renameSync(tempOut, filePath)
          } catch (e) {
            console.warn('[MetadataTagger] FLAC file swap failed:', e)
          }
        } else {
          if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        }
        resolve(filePath)
      })
      child.on('error', () => {
        if (coverFile && fs.existsSync(coverFile)) fs.unlinkSync(coverFile)
        if (normalizedCover && fs.existsSync(normalizedCover)) fs.unlinkSync(normalizedCover)
        if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        resolve(filePath)
      })
    })
  }

  private static async tagViaFfmpeg(filePath: string, trackInfo: TrackInfo, imageBuffer?: Buffer): Promise<string> {
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')
    const dir = path.dirname(filePath)
    const ext = path.extname(filePath)
    const baseName = path.basename(filePath, ext)
    const tempOut = path.join(dir, `${baseName}_tagged${ext}`)

    let coverFile: string | null = null
    if (imageBuffer) {
      coverFile = path.join(dir, `_temp_cover_${Date.now()}.jpg`)
      fs.writeFileSync(coverFile, imageBuffer)
    }

    const artistStr = trackInfo.artists.join(', ')
    const albumStr = trackInfo.album || trackInfo.title

    const args = ['-y', '-i', filePath]

    if (coverFile && fs.existsSync(coverFile) && (ext === '.m4a' || ext === '.mp4')) {
      args.push('-i', coverFile)
      args.push('-map', '0', '-map', '1')
      args.push('-c', 'copy')
      args.push('-disposition:v:1', 'attached_pic')
    } else {
      args.push('-c', 'copy')
    }

    args.push('-metadata', `title=${trackInfo.title}`)
    args.push('-metadata', `artist=${artistStr}`)
    args.push('-metadata', `album_artist=${artistStr}`)
    args.push('-metadata', `album=${albumStr}`)
    args.push('-metadata', `date=${new Date().getFullYear()}`)

    if (trackInfo.trackNumber) {
      args.push('-metadata', `track=${trackInfo.trackNumber}`)
    }

    args.push(tempOut)

    return new Promise((resolve) => {
      const child = spawn(ffmpegPath, args, { windowsHide: true })
      child.on('close', (code) => {
        if (coverFile && fs.existsSync(coverFile)) fs.unlinkSync(coverFile)

        if (code === 0 && fs.existsSync(tempOut)) {
          try {
            fs.unlinkSync(filePath)
            fs.renameSync(tempOut, filePath)
          } catch (e) {
            console.warn('[MetadataTagger] FFmpeg tag swap failed:', e)
          }
        } else {
          if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        }
        resolve(filePath)
      })
      child.on('error', () => {
        if (coverFile && fs.existsSync(coverFile)) fs.unlinkSync(coverFile)
        if (fs.existsSync(tempOut)) fs.unlinkSync(tempOut)
        resolve(filePath)
      })
    })
  }
}
