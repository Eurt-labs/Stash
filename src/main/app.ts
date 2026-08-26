import { app, BrowserWindow, shell } from 'electron'
import path from 'path'
import fs from 'fs'
import { StashOrchestrator } from './features/downloader/StashOrchestrator'
import { DownloadEngine } from './features/downloader/DownloadEngine'
import { registerIpcHandlers } from './ipc'

process.env.DIST = path.join(__dirname, '../dist')
process.env.VITE_PUBLIC = app.isPackaged ? process.env.DIST : path.join(process.env.DIST, '../public')

let mainWindow: BrowserWindow | null = null
const orchestrator = new StashOrchestrator()

function createWindow(): void {
  mainWindow = new BrowserWindow({
    title: 'Stash Downloader',
    icon: path.join(__dirname, '../app-resources/icon.png'),
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

  // Register all modular domain IPC handlers
  registerIpcHandlers(orchestrator, () => mainWindow)

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

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

// Single instance lock
const gotTheLock = app.requestSingleInstanceLock()

if (!gotTheLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.focus()
    }
  })

  app.whenReady().then(() => {
    // Inject the native machine's User-Agent into the DownloadEngine
    DownloadEngine.deviceUserAgent = app.userAgentFallback || `Mozilla/5.0 (${process.platform}; ${process.arch}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36`
    
    // Load previously saved cookies if they exist
    const cookiesPath = path.join(app.getPath('userData'), 'youtube_cookies.txt')
    if (fs.existsSync(cookiesPath)) {
      DownloadEngine.cookiesFile = cookiesPath
    }

    createWindow()

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) {
        createWindow()
      }
    })
  })

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
      app.quit()
    }
  })
}
