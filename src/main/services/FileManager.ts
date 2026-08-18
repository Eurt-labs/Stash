import fs from 'fs'
import path from 'path'
import os from 'os'
import { shell } from 'electron'

export class FileManager {
  /**
   * Returns the default download directory: ~/Downloads/Stash
   */
  public static getDefaultDownloadDir(): string {
    const downloadsDir = path.join(os.homedir(), 'Downloads')
    const stashDir = path.join(downloadsDir, 'Stash')
    if (!fs.existsSync(stashDir)) {
      try {
        fs.mkdirSync(stashDir, { recursive: true })
      } catch (err) {
        console.error('Failed to create default stash directory:', err)
        return downloadsDir
      }
    }
    return stashDir
  }

  /**
   * Returns temporary cache directory: ~/.stash_cache
   */
  public static getCacheDir(): string {
    const cacheDir = path.join(os.homedir(), '.stash_cache')
    if (!fs.existsSync(cacheDir)) {
      fs.mkdirSync(cacheDir, { recursive: true })
    }
    return cacheDir
  }

  /**
   * Cleans and sanitizes a string to be a safe filesystem file name
   */
  public static sanitizeFileName(name: string): string {
    if (!name) return 'media_file'
    return name
      .replace(/[\\/:*?"<>|]/g, '_') // Replace forbidden characters
      .replace(/\s+/g, ' ')           // Normalize multiple spaces
      .trim()
      .substring(0, 150)              // Prevent excessively long file names
  }

  /**
   * Finds the most recently modified output file in a directory matching a base name
   */
  public static findOutputFile(dir: string, baseName: string): string | null {
    if (!fs.existsSync(dir)) return null
    try {
      const files = fs.readdirSync(dir)
      const matchingFiles = files
        .filter((file) => {
          const nameWithoutExt = path.parse(file).name
          return nameWithoutExt === baseName
        })
        .map((file) => {
          const fullPath = path.join(dir, file)
          const stats = fs.statSync(fullPath)
          return { fullPath, mtime: stats.mtimeMs }
        })
        .sort((a, b) => b.mtime - a.mtime)

      return matchingFiles.length > 0 ? matchingFiles[0].fullPath : null
    } catch (err) {
      console.error('Error finding output file:', err)
      return null
    }
  }

  /**
   * Safely moves or copies a file from source to target
   */
  public static moveFile(srcPath: string, destDir: string, destFileName?: string): string {
    if (!fs.existsSync(destDir)) {
      fs.mkdirSync(destDir, { recursive: true })
    }

    const fileName = destFileName || path.basename(srcPath)
    const targetPath = path.join(destDir, fileName)

    try {
      if (fs.existsSync(targetPath)) {
        fs.unlinkSync(targetPath)
      }
      fs.renameSync(srcPath, targetPath)
      return targetPath
    } catch (err) {
      // Fallback copy + delete if cross-device
      fs.copyFileSync(srcPath, targetPath)
      try {
        fs.unlinkSync(srcPath)
      } catch (e) {
        // ignore delete error
      }
      return targetPath
    }
  }

  /**
   * Deletes a temporary file safely
   */
  public static deleteFileSafe(filePath: string): void {
    try {
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath)
      }
    } catch (err) {
      console.warn(`Could not delete temp file ${filePath}:`, err)
    }
  }

  /**
   * Cleans the stash cache directory
   */
  public static cleanCache(): void {
    try {
      const cacheDir = this.getCacheDir()
      if (fs.existsSync(cacheDir)) {
        const files = fs.readdirSync(cacheDir)
        for (const file of files) {
          try {
            fs.unlinkSync(path.join(cacheDir, file))
          } catch (e) {
            // ignore
          }
        }
      }
    } catch (err) {
      console.error('Error cleaning cache:', err)
    }
  }

  /**
   * Opens a directory in Windows Explorer / OS file manager
   */
  public static async openDirectory(dirPath: string): Promise<void> {
    if (fs.existsSync(dirPath)) {
      await shell.openPath(dirPath)
    }
  }

  /**
   * Shows a file in Windows Explorer or opens it
   */
  public static async showItemInFolder(filePath: string): Promise<void> {
    if (fs.existsSync(filePath)) {
      shell.showItemInFolder(filePath)
    }
  }
}
