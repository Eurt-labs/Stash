import { contextBridge, ipcRenderer } from 'electron'
import { StashAPI, DownloadQuality, DownloadFormat, TrackInfo } from '../shared/types'

const api: StashAPI = {
  getSettings: () => ipcRenderer.invoke('stash:getSettings'),
  setOutputDir: (dir: string) => ipcRenderer.invoke('stash:setOutputDir', dir),
  setQuality: (quality: DownloadQuality) => ipcRenderer.invoke('stash:setQuality', quality),
  setFormat: (format: DownloadFormat) => ipcRenderer.invoke('stash:setFormat', format),
  selectDirectory: (defaultPath?: string) => ipcRenderer.invoke('stash:selectDirectory', defaultPath),
  openDirectory: (dirPath: string) => ipcRenderer.invoke('stash:openDirectory', dirPath),
  openFile: (filePath: string) => ipcRenderer.invoke('stash:openFile', filePath),
  openExternalUrl: (url: string) => ipcRenderer.invoke('stash:openExternalUrl', url),
  checkDependencies: () => ipcRenderer.invoke('stash:checkDependencies'),
  updateYtDlp: () => ipcRenderer.invoke('stash:updateYtDlp'),
  checkAppUpdate: () => ipcRenderer.invoke('stash:checkAppUpdate'),
  loginYouTube: () => ipcRenderer.invoke('stash:loginYouTube'),

  parseAndFetchMetadata: (url: string) => ipcRenderer.invoke('stash:parseAndFetchMetadata', url),
  enqueueBatch: (name: string, tracks: TrackInfo[], quality?: DownloadQuality, format?: DownloadFormat, outputDir?: string) =>
    ipcRenderer.invoke('stash:enqueueBatch', { name, tracks, quality, format, outputDir }),
  startBatchDownload: (batchId: string) => ipcRenderer.invoke('stash:startBatchDownload', batchId),
  startTrackDownload: (batchId: string, trackId: string) => ipcRenderer.invoke('stash:startTrackDownload', batchId, trackId),
  cancelTrack: (batchId: string, trackId: string) => ipcRenderer.invoke('stash:cancelTrack', batchId, trackId),
  cancelBatch: (batchId: string) => ipcRenderer.invoke('stash:cancelBatch', batchId),
  removeTrack: (batchId: string, trackId: string) => ipcRenderer.invoke('stash:removeTrack', batchId, trackId),
  removeBatch: (batchId: string) => ipcRenderer.invoke('stash:removeBatch', batchId),
  clearCompletedBatches: () => ipcRenderer.invoke('stash:clearCompletedBatches'),
  getAllBatches: () => ipcRenderer.invoke('stash:getAllBatches'),

  onBatchUpdated: (callback) => {
    const handler = (_event: any, batches: any) => callback(batches)
    ipcRenderer.on('stash:batchesUpdated', handler)
    return () => {
      ipcRenderer.removeListener('stash:batchesUpdated', handler)
    }
  },
  onFetchingStatus: (callback) => {
    const handler = (_event: any, status: any) => callback(status)
    ipcRenderer.on('stash:fetchingStatus', handler)
    return () => {
      ipcRenderer.removeListener('stash:fetchingStatus', handler)
    }
  },
  onToast: (callback) => {
    const handler = (_event: any, toast: any) => callback(toast)
    ipcRenderer.on('stash:toast', handler)
    return () => {
      ipcRenderer.removeListener('stash:toast', handler)
    }
  }
}

contextBridge.exposeInMainWorld('stashAPI', api)
