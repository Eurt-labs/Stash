import { app, BrowserWindow, ipcMain, dialog, shell } from 'electron'
import path from 'path'
import { StashOrchestrator } from './services/StashOrchestrator'
import { DependencyResolver } from './services/DependencyResolver'
import { FileManager } from './services/FileManager'
import { AppUpdateChecker } from './services/AppUpdateChecker'
import { DownloadQuality, DownloadFormat, TrackInfo } from '../shared/types'

process.env.DIST = path.join(__dirname, '../dist')
process.env.VITE_PUBLIC = app.isPackaged ? process.env.DIST : path.join(process.env.DIST, '../public')

let mainWindow: BrowserWindow | null = null
const orchestrator = new StashOrchestrator()

function createWindow() {
  mainWindow = new BrowserWindow({
    title: 'Stash Downloader',
    width: 1100,
    height: 800,
    minWidth: 840,
    minHeight: 620,
    backgroundColor: '#090d16',
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      sandbox: false,
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  // Wire orchestrator events to renderer
  orchestrator.onBatchesChanged = (batches) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('stash:batchesUpdated', batches)
    }
  }

  orchestrator.onFetchingStatusChanged = (status) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('stash:fetchingStatus', status)
    }
  }

  orchestrator.onToastMessage = (toast) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('stash:toast', toast)
    }
  }

  // Graceful show on ready-to-show
  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
  })

  // Handle external link clicks
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  const devServerUrl = process.env.VITE_DEV_SERVER_URL
  if (devServerUrl) {
    mainWindow.loadURL(devServerUrl)
  } else {
    const distPath = process.env.DIST || path.join(__dirname, '../dist')
    mainWindow.loadFile(path.join(distPath, 'index.html'))
  }
}

app.whenReady().then(() => {
  setupIpcHandlers()
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

function setupIpcHandlers() {
  // Settings
  ipcMain.handle('stash:getSettings', () => orchestrator.getSettings())
  ipcMain.handle('stash:setOutputDir', (_e, dir: string) => orchestrator.setOutputDirectory(dir))
  ipcMain.handle('stash:setQuality', (_e, quality: DownloadQuality) => orchestrator.setQuality(quality))
  ipcMain.handle('stash:setFormat', (_e, format: DownloadFormat) => orchestrator.setFormat(format))

  // System & Files
  ipcMain.handle('stash:selectDirectory', async (_e, defaultPath?: string) => {
    if (!mainWindow) return null
    const result = await dialog.showOpenDialog(mainWindow, {
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

  ipcMain.handle('stash:enqueueBatch', async (_e, data: { name: string; tracks: TrackInfo[]; quality?: DownloadQuality; format?: DownloadFormat; outputDir?: string }) => {
    return orchestrator.enqueueBatch(data.name, data.tracks, data.quality, data.format, data.outputDir)
  })

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
