import { spawn, execFile } from 'child_process'
import path from 'path'
import fs from 'fs'
import os from 'os'
import axios from 'axios'
import { app } from 'electron'
import { DependencyStatus } from '../../../shared/types'
import { APP_CONSTANTS } from '../../core/constants'

export class DependencyResolver {
  private static cachedStatus: DependencyStatus | null = null

  /**
   * Resolves the executable path for a given binary (yt-dlp, ffmpeg, ffprobe)
   */
  public static resolveExecutable(binaryName: string): string {
    const isWindows = process.platform === 'win32'
    const ext = isWindows ? '.exe' : ''
    const exeName = `${binaryName}${ext}`

    // 1. User auto-managed ~/.stash/bin/<binary>.exe
    const userStashBin = path.join(APP_CONSTANTS.DEFAULT_STASH_BIN_DIR, exeName)
    if (fs.existsSync(userStashBin)) {
      return userStashBin
    }

    // 2. Packaged extraResources path: <resources>/bin/yt-dlp.exe
    if (app && app.isPackaged) {
      const packagedBin = path.join(process.resourcesPath, 'bin', exeName)
      if (fs.existsSync(packagedBin)) {
        return packagedBin
      }
      const packagedAppResources = path.join(process.resourcesPath, 'app-resources', 'windows', exeName)
      if (fs.existsSync(packagedAppResources)) {
        return packagedAppResources
      }
    }

    // 3. Dev mode: app-resources/windows/yt-dlp.exe in current working directory
    const devPath = path.join(process.cwd(), 'app-resources', 'windows', exeName)
    if (fs.existsSync(devPath)) {
      return devPath
    }

    // 4. Application directory / root
    const appDir = app ? app.getAppPath() : process.cwd()
    const appDirPath = path.join(appDir, 'app-resources', 'windows', exeName)
    if (fs.existsSync(appDirPath)) {
      return appDirPath
    }

    // 5. Fallback to system PATH
    return binaryName
  }

  /**
   * Runs an executable with arguments and returns stdout if exit code is 0
   */
  public static checkBinary(binaryName: string, args: string[] = ['--version']): Promise<{ installed: boolean; version?: string; path?: string }> {
    return new Promise((resolve) => {
      const resolvedPath = this.resolveExecutable(binaryName)
      execFile(resolvedPath, args, { timeout: 10000 }, (error, stdout, stderr) => {
        if (!error && (stdout || stderr)) {
          const raw = (stdout || stderr).toString().trim()
          const firstLine = raw.split('\n')[0].trim()
          resolve({
            installed: true,
            version: firstLine,
            path: resolvedPath
          })
        } else {
          // If custom path failed, try system PATH
          if (resolvedPath !== binaryName) {
            execFile(binaryName, args, { timeout: 10000 }, (err2, out2, errOut2) => {
              if (!err2 && (out2 || errOut2)) {
                const raw2 = (out2 || errOut2).toString().trim()
                const firstLine2 = raw2.split('\n')[0].trim()
                resolve({
                  installed: true,
                  version: firstLine2,
                  path: binaryName
                })
              } else {
                resolve({ installed: false })
              }
            })
          } else {
            resolve({ installed: false })
          }
        }
      })
    })
  }

  /**
   * Retrieves full dependency status for yt-dlp, ffmpeg, ffprobe
   */
  public static async getDependencyStatus(forceRefresh = false): Promise<DependencyStatus> {
    if (this.cachedStatus && !forceRefresh) {
      return this.cachedStatus
    }

    const [ytDlp, ffmpeg, ffprobe] = await Promise.all([
      this.checkBinary('yt-dlp', ['--version']),
      this.checkBinary('ffmpeg', ['-version']),
      this.checkBinary('ffprobe', ['-version'])
    ])

    const status: DependencyStatus = {
      ytDlpInstalled: ytDlp.installed,
      ytDlpVersion: ytDlp.version,
      ytDlpPath: ytDlp.path,
      ffmpegInstalled: ffmpeg.installed,
      ffmpegVersion: ffmpeg.version,
      ffmpegPath: ffmpeg.path,
      ffprobeInstalled: ffprobe.installed,
      ffprobePath: ffprobe.path
    }

    this.cachedStatus = status
    return status
  }

  /**
   * Downloads and initializes yt-dlp nightly binary directly into ~/.stash/bin/yt-dlp.exe
   */
  public static async installYtDlpDirect(): Promise<{ success: boolean; message: string }> {
    const userBinDir = APP_CONSTANTS.DEFAULT_STASH_BIN_DIR
    if (!fs.existsSync(userBinDir)) {
      try {
        fs.mkdirSync(userBinDir, { recursive: true })
      } catch (err) {
        console.error('Failed to create ~/.stash/bin directory:', err)
      }
    }

    const destPath = path.join(userBinDir, 'yt-dlp.exe')
    const url = APP_CONSTANTS.YT_DLP_NIGHTLY_URL

    try {
      const response = await axios({
        method: 'GET',
        url,
        responseType: 'stream',
        timeout: 60000,
        headers: {
          'User-Agent': APP_CONSTANTS.USER_AGENT
        }
      })

      const writer = fs.createWriteStream(destPath)
      response.data.pipe(writer)

      await new Promise<void>((resolve, reject) => {
        writer.on('finish', () => resolve())
        writer.on('error', (err) => reject(err))
      })

      this.cachedStatus = null
      return { success: true, message: 'yt-dlp nightly has been successfully downloaded and initialized!' }
    } catch (err: any) {
      return { success: false, message: `Failed to download yt-dlp: ${err.message}` }
    }
  }

  /**
   * Updates yt-dlp by running `yt-dlp --update-to nightly` or downloading it directly if missing
   */
  public static async updateYtDlp(): Promise<{ success: boolean; message: string }> {
    const status = await this.getDependencyStatus(true)

    // If yt-dlp is not installed at all, perform a direct download bootstrap
    if (!status.ytDlpInstalled) {
      return this.installYtDlpDirect()
    }

    return new Promise((resolve) => {
      const ytDlpPath = this.resolveExecutable('yt-dlp')
      execFile(ytDlpPath, ['--update-to', 'nightly'], { timeout: 60000 }, async (error, stdout, stderr) => {
        this.cachedStatus = null

        const output = `${stdout || ''} ${stderr || ''}`.trim()
        if (!error) {
          const isUpToDate = output.toLowerCase().includes('up to date')
          const isUpdated = output.toLowerCase().includes('updated') || output.toLowerCase().includes('downloading')
          
          if (isUpToDate) {
            resolve({
              success: true,
              message: output || 'yt-dlp is already up to date with the latest release.'
            })
          } else if (isUpdated) {
            resolve({
              success: true,
              message: output || 'yt-dlp has been updated to the latest nightly version!'
            })
          } else {
            resolve({
              success: true,
              message: output || 'Update check completed successfully.'
            })
          }
        } else {
          // If update failed, fallback to direct download bootstrap
          const directResult = await this.installYtDlpDirect()
          resolve(directResult)
        }
      })
    })
  }
}
