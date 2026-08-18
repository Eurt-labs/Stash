import fs from 'fs'
import path from 'path'
import axios from 'axios'
import NodeID3 from 'node-id3'
import { TrackInfo } from '../../shared/types'

export class MetadataTagger {
  private static readonly MAX_ART_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB max

  /**
   * Tags an audio file with track metadata and embedded album artwork
   */
  public static async tagFile(filePath: string, trackInfo: TrackInfo): Promise<void> {
    if (!fs.existsSync(filePath)) {
      console.warn(`Cannot tag non-existent file: ${filePath}`)
      return
    }

    const ext = path.extname(filePath).toLowerCase()
    if (ext === '.mp3') {
      await this.tagMp3(filePath, trackInfo)
    }
  }

  private static async tagMp3(filePath: string, trackInfo: TrackInfo): Promise<void> {
    try {
      const tags: NodeID3.Tags = {
        title: trackInfo.title,
        artist: trackInfo.artists.join(', '),
        album: trackInfo.album || '',
        year: trackInfo.releaseYear || '',
        trackNumber: trackInfo.trackNumber ? String(trackInfo.trackNumber) : undefined,
        genre: trackInfo.genre || undefined
      }

      if (trackInfo.albumArtUrl) {
        const imageBuffer = await this.downloadArtwork(trackInfo.albumArtUrl)
        if (imageBuffer) {
          tags.image = {
            mime: 'image/jpeg',
            type: { id: 3, name: 'front cover' },
            description: 'Cover Art',
            imageBuffer
          }
        }
      }

      const success = NodeID3.write(tags, filePath)
      if (!success) {
        console.warn(`NodeID3 returned false while writing tags to ${filePath}`)
      }
    } catch (err) {
      console.error(`Failed to tag MP3 file ${filePath}:`, err)
    }
  }

  private static async downloadArtwork(url: string): Promise<Buffer | null> {
    try {
      const response = await axios.get(url, {
        responseType: 'arraybuffer',
        timeout: 10000,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
      })

      if (response.status === 200 && response.data) {
        const buffer = Buffer.from(response.data)
        if (buffer.length <= this.MAX_ART_SIZE_BYTES) {
          return buffer
        }
      }
      return null
    } catch (err) {
      console.warn(`Failed to download artwork from ${url}:`, err)
      return null
    }
  }
}
