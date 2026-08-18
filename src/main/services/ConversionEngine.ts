import { spawn, execFile, ChildProcess } from 'child_process'
import path from 'path'
import fs from 'fs'
import readline from 'readline'
import { DownloadFormat, DownloadQuality } from '../../shared/types'
import { DependencyResolver } from './DependencyResolver'

export class ConversionEngine {
  private activeProcesses: Map<string, ChildProcess> = new Map()

  /**
   * Converts a downloaded audio/video file to the desired output format & bitrate.
   */
  public async convert(
    inputPath: string,
    format: DownloadFormat,
    quality: DownloadQuality,
    conversionId: string,
    onProgress?: (progressFraction: number) => void
  ): Promise<string> {
    if (!fs.existsSync(inputPath)) {
      throw new Error(`Input file not found: ${inputPath}`)
    }

    const ext = this.getFormatExtension(format)
    const inputParsed = path.parse(inputPath)
    const outputPath = path.join(inputParsed.dir, `${inputParsed.name}.${ext}`)

    // If input already has target format and doesn't need re-encoding
    if (inputParsed.ext.replace('.', '').toLowerCase() === ext.toLowerCase()) {
      return inputPath
    }

    const durationSeconds = await this.getDuration(inputPath)
    const ffmpegPath = DependencyResolver.resolveExecutable('ffmpeg')

    const args: string[] = ['-i', inputPath, '-y']

    if (format === 'MP4' || format === 'OTHER_VIDEO') {
      args.push('-c:v', 'copy', '-c:a', 'aac', '-b:a', '192k')
    } else {
      args.push('-vn') // No video
      const { codec, bitrate } = this.getAudioCodecAndBitrate(format, quality)
      args.push('-codec:a', codec)
      if (bitrate) {
        args.push('-b:a', bitrate)
      }
      args.push('-ar', '44100', '-ac', '2')
    }

    args.push('-progress', 'pipe:1', '-loglevel', 'error', outputPath)

    return new Promise<string>((resolve, reject) => {
      const child = spawn(ffmpegPath, args, { windowsHide: true })
      this.activeProcesses.set(conversionId, child)

      const rl = readline.createInterface({ input: child.stdout })

      rl.on('line', (line) => {
        if (line.startsWith('out_time_us=')) {
          const timeUs = parseInt(line.replace('out_time_us=', '').trim(), 10)
          if (!isNaN(timeUs) && durationSeconds > 0) {
            const currentSeconds = timeUs / 1000000.0
            const fraction = Math.min(Math.max(currentSeconds / durationSeconds, 0), 1)
            if (onProgress) {
              onProgress(fraction)
            }
          }
        }
      })

      let stderrOutput = ''
      child.stderr.on('data', (data) => {
        stderrOutput += data.toString()
      })

      child.on('close', (code) => {
        this.activeProcesses.delete(conversionId)
        if (code === 0) {
          if (fs.existsSync(outputPath)) {
            // Delete raw input file if different
            if (inputPath !== outputPath) {
              try {
                fs.unlinkSync(inputPath)
              } catch (e) {
                // ignore
              }
            }
            if (onProgress) onProgress(1)
            resolve(outputPath)
          } else {
            reject(new Error('Conversion finished but target file missing'))
          }
        } else {
          reject(new Error(`FFmpeg error (code ${code}): ${stderrOutput.slice(-300)}`))
        }
      })

      child.on('error', (err) => {
        this.activeProcesses.delete(conversionId)
        reject(new Error(`FFmpeg failed to start: ${err.message}`))
      })
    })
  }

  /**
   * Cancels an active conversion
   */
  public cancelConversion(conversionId: string): void {
    const proc = this.activeProcesses.get(conversionId)
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
      this.activeProcesses.delete(conversionId)
    }
  }

  private getFormatExtension(format: DownloadFormat): string {
    switch (format) {
      case 'MP3': return 'mp3'
      case 'AAC': return 'm4a'
      case 'FLAC': return 'flac'
      case 'OPUS': return 'opus'
      case 'WAV': return 'wav'
      case 'MP4':
      case 'OTHER_VIDEO': return 'mp4'
      case 'AUTO':
      default: return 'mp3'
    }
  }

  private getAudioCodecAndBitrate(format: DownloadFormat, quality: DownloadQuality): { codec: string; bitrate?: string } {
    const isHighOrAbove = quality === '4K' || quality === '2K' || quality === 'HIGH'
    switch (format) {
      case 'MP3': {
        const kbps = isHighOrAbove ? '320k' : (quality === 'MID' ? '192k' : '128k')
        return { codec: 'libmp3lame', bitrate: kbps }
      }
      case 'AAC': {
        const kbps = isHighOrAbove ? '256k' : (quality === 'MID' ? '192k' : '128k')
        return { codec: 'aac', bitrate: kbps }
      }
      case 'FLAC':
        return { codec: 'flac' }
      case 'OPUS': {
        const kbps = isHighOrAbove ? '256k' : (quality === 'MID' ? '160k' : '96k')
        return { codec: 'libopus', bitrate: kbps }
      }
      case 'WAV':
        return { codec: 'pcm_s16le' }
      case 'AUTO':
      default: {
        const kbps = isHighOrAbove ? '320k' : (quality === 'MID' ? '192k' : '128k')
        return { codec: 'libmp3lame', bitrate: kbps }
      }
    }
  }

  private getDuration(filePath: string): Promise<number> {
    return new Promise((resolve) => {
      const ffprobePath = DependencyResolver.resolveExecutable('ffprobe')
      const args = [
        '-v', 'quiet',
        '-show_entries', 'format=duration',
        '-of', 'default=noprint_wrappers=1:nokey=1',
        filePath
      ]
      execFile(ffprobePath, args, { timeout: 10000 }, (err, stdout) => {
        if (!err && stdout) {
          const duration = parseFloat(stdout.trim())
          resolve(!isNaN(duration) ? duration : 0)
        } else {
          resolve(0)
        }
      })
    })
  }
}
