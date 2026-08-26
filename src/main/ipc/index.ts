import { ipcMain, dialog, shell, BrowserWindow } from 'electron'
import { StashOrchestrator } from '../features/downloader/StashOrchestrator'
import { DependencyResolver } from '../features/updater/DependencyResolver'
import { AppUpdateChecker } from '../features/updater/AppUpdateChecker'
import { FileManager } from '../core/utils/FileManager'
import { DownloadQuality, DownloadFormat, TrackInfo } from '../../shared/types'

export function registerIpcHandlers(orchestrator: StashOrchestrator, getMainWindow: () => BrowserWindow | null): void {
  // Wire orchestrator events to renderer
  orchestrator.onBatchesChanged = (batches) => {
    const win = getMainWindow()
    if (win && !win.isDestroyed()) {
      win.webContents.send('stash:batchesUpdated', batches)
    }
  }

  orchestrator.onFetchingStatusChanged = (status) => {
    const win = getMainWindow()
    if (win && !win.isDestroyed()) {
      win.webContents.send('stash:fetchingStatus', status)
    }
  }

  orchestrator.onToastMessage = (toast) => {
    const win = getMainWindow()
    if (win && !win.isDestroyed()) {
      win.webContents.send('stash:toast', toast)
    }
  }

  // Settings
  ipcMain.handle('stash:getSettings', () => orchestrator.getSettings())
  ipcMain.handle('stash:setOutputDir', (_e, dir: string) => orchestrator.setOutputDirectory(dir))
  ipcMain.handle('stash:setQuality', (_e, quality: DownloadQuality) => orchestrator.setQuality(quality))
  ipcMain.handle('stash:setFormat', (_e, format: DownloadFormat) => orchestrator.setFormat(format))


  // System & Files
  ipcMain.handle('stash:selectDirectory', async (_e, defaultPath?: string) => {
    const win = getMainWindow()
    if (!win) return null
    const result = await dialog.showOpenDialog(win, {
      title: 'Select Output Folder for Downloads',
      defaultPath: defaultPath || orchestrator.getSettings().outputDir,
      properties: ['openDirectory', 'createDirectory']
    })
    if (!result.canceled && result.filePaths.length > 0) {
      const selected = result.filePaths[0]
      orchestrator.setOutputDirectory(selected)
      return selected
    }
    return null
  })

  ipcMain.handle('stash:openDirectory', async (_e, dirPath: string) => {
    await FileManager.openDirectory(dirPath)
  })

  ipcMain.handle('stash:openFile', async (_e, filePath: string) => {
    await FileManager.showItemInFolder(filePath)
  })

  ipcMain.handle('stash:openExternalUrl', async (_e, url: string) => {
    if (url) {
      await shell.openExternal(url)
    }
  })

  // Dependencies & Updates
  ipcMain.handle('stash:checkDependencies', async () => {
    return await DependencyResolver.getDependencyStatus(true)
  })

  ipcMain.handle('stash:updateYtDlp', async () => {
    return await DependencyResolver.updateYtDlp()
  })

  ipcMain.handle('stash:checkAppUpdate', async () => {
    return await AppUpdateChecker.checkForUpdates()
  })

  // Metadata & Queue
  ipcMain.handle('stash:parseAndFetchMetadata', async (_e, url: string) => {
    return await orchestrator.fetchMetadata(url)
  })

  ipcMain.handle(
    'stash:enqueueBatch',
    async (
      _e,
      data: {
        name: string
        tracks: TrackInfo[]
        quality?: DownloadQuality
        format?: DownloadFormat
        outputDir?: string
      }
    ) => {
      return orchestrator.enqueueBatch(data.name, data.tracks, data.quality, data.format, data.outputDir)
    }
  )

  ipcMain.handle('stash:startBatchDownload', async (_e, batchId: string) => {
    await orchestrator.startBatchDownload(batchId)
  })

  ipcMain.handle('stash:startTrackDownload', async (_e, batchId: string, trackId: string) => {
    await orchestrator.startTrackDownload(batchId, trackId)
  })

  ipcMain.handle('stash:cancelTrack', async (_e, batchId: string, trackId: string) => {
    await orchestrator.cancelTrack(batchId, trackId)
  })

  ipcMain.handle('stash:cancelBatch', async (_e, batchId: string) => {
    await orchestrator.cancelBatch(batchId)
  })

  ipcMain.handle('stash:removeTrack', async (_e, batchId: string, trackId: string) => {
    await orchestrator.removeTrack(batchId, trackId)
  })

  ipcMain.handle('stash:removeBatch', async (_e, batchId: string) => {
    await orchestrator.removeBatch(batchId)
  })

  ipcMain.handle('stash:clearCompletedBatches', async () => {
    await orchestrator.clearCompletedBatches()
  })

  ipcMain.handle('stash:getAllBatches', async () => {
    return orchestrator.getAllBatches()
  })
}
