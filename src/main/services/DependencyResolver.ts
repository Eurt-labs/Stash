import { spawn, execFile } from 'child_process'
import path from 'path'
import fs from 'fs'
import os from 'os'
import { app } from 'electron'
import { DependencyStatus } from '../../shared/types'

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
    const userStashBin = path.join(os.homedir(), '.stash', 'bin', exeName)
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
   * Updates yt-dlp by running `yt-dlp -U` or refreshing the executable
   */
  public static updateYtDlp(): Promise<{ success: boolean; message: string }> {
    return new Promise((resolve) => {
      const ytDlpPath = this.resolveExecutable('yt-dlp')
      execFile(ytDlpPath, ['--update-to', 'nightly'], { timeout: 60000 }, async (error, stdout, stderr) => {
        // Invalidate cache so subsequent checks fetch the latest version string
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
              message: output || 'yt-dlp has been updated to the latest version!'
            })
          } else {
            resolve({
              success: true,
              message: output || 'Update check completed successfully.'
            })
          }
        } else {
          resolve({
            success: false,
            message: output || error.message || 'Update check encountered an error. Please verify internet connection.'
          })
        }
      })
    })
  }
}
