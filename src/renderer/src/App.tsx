import React, { useState, useEffect } from 'react'
import {
  DownloadBatch,
  DownloadQuality,
  DownloadFormat,
  ColorTheme,
  DependencyStatus,
  AppUpdateStatus,
  TrackInfo
} from '../../shared/types'
import { Header } from './components/Header'
import { SettingsBar } from './components/SettingsBar'
import { LinkInputBar } from './components/LinkInputBar'
import { BatchItem } from './components/BatchItem'
import { DependencyModal } from './components/DependencyModal'
import { Toast, ToastItem } from './components/Toast'
import { ListMusic, Trash2 } from 'lucide-react'

export const App: React.FC = () => {
  const [batches, setBatches] = useState<Record<string, DownloadBatch>>({})
  const [outputDir, setOutputDir] = useState('')
  const [quality, setQuality] = useState<DownloadQuality>('HIGH')
  const [format, setFormat] = useState<DownloadFormat>('AUTO')
  const [theme, setTheme] = useState<ColorTheme>(() => {
    return (localStorage.getItem('stash_theme') as ColorTheme) || 'indigo'
  })

  const [isFetching, setIsFetching] = useState(false)
  const [fetchingMessage, setFetchingMessage] = useState('')
  const [depStatus, setDepStatus] = useState<DependencyStatus | null>(null)
  const [appUpdate, setAppUpdate] = useState<AppUpdateStatus | null>(null)
  const [isDepModalOpen, setIsDepModalOpen] = useState(false)
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const addToast = (type: 'success' | 'error' | 'info', message: string) => {
    const id = Math.random().toString(36).substring(2, 9)
    setToasts((prev) => [...prev, { id, type, message }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }

  useEffect(() => {
    if (!window.stashAPI) return

    // 1. Load initial settings
    window.stashAPI.getSettings().then((settings) => {
      setOutputDir(settings.outputDir)
      setQuality(settings.quality)
      setFormat(settings.format)
    })

    // 2. Check dependencies & app updates
    window.stashAPI.checkDependencies().then((status) => {
      setDepStatus(status)
      if (!status.ytDlpInstalled || !status.ffmpegInstalled) {
        setIsDepModalOpen(true)
      }
    })

    window.stashAPI.checkAppUpdate().then((update) => {
      setAppUpdate(update)
      if (update.hasUpdate) {
        addToast('info', `New version v${update.latestVersion} available on GitHub!`)
      }
    })

    // 3. Load initial batches
    window.stashAPI.getAllBatches().then((b) => setBatches(b))

    // 4. Subscriptions
    const unsubBatches = window.stashAPI.onBatchUpdated((updatedBatches) => {
      setBatches(updatedBatches)
    })

    const unsubFetching = window.stashAPI.onFetchingStatus((status) => {
      setIsFetching(status.isFetching)
      setFetchingMessage(status.message)
    })

    const unsubToast = window.stashAPI.onToast((toast) => {
      addToast(toast.type, toast.message)
    })

    return () => {
      unsubBatches()
      unsubFetching()
      unsubToast()
    }
  }, [])

  // Sync theme with DOM attribute and localStorage
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('stash_theme', theme)
  }, [theme])

  const handleFetch = async (url: string) => {
    try {
      const result = await window.stashAPI.parseAndFetchMetadata(url)
      const { parsedLink, tracks } = result

      let batchName = tracks[0]?.title || 'Download Batch'
      if (tracks.length > 1) {
        if (parsedLink.id && !parsedLink.id.startsWith('http')) {
          batchName = `${parsedLink.id} (${tracks.length} tracks)`
        } else {
          batchName = `${tracks[0].artists[0]} Collection (${tracks.length} tracks)`
        }
      }

      await window.stashAPI.enqueueBatch(batchName, tracks, quality, format, outputDir)
      addToast('success', `Enqueued ${tracks.length} track(s) to download queue!`)
    } catch (err: any) {
      addToast('error', err?.message || 'Failed to fetch media metadata')
    }
  }

  const handleSelectDir = async () => {
    const selected = await window.stashAPI.selectDirectory(outputDir)
    if (selected) {
      setOutputDir(selected)
      addToast('info', `Output directory set to: ${selected}`)
    }
  }

  const handleOpenDir = async () => {
    if (outputDir) {
      await window.stashAPI.openDirectory(outputDir)
    }
  }

  const handleChangeQuality = async (q: DownloadQuality) => {
    setQuality(q)
    await window.stashAPI.setQuality(q)
  }

  const handleChangeFormat = async (f: DownloadFormat) => {
    setFormat(f)
    await window.stashAPI.setFormat(f)
  }

  const handleRefreshDeps = async () => {
    const status = await window.stashAPI.checkDependencies()
    setDepStatus(status)
  }

  const handleClearCompleted = async () => {
    await window.stashAPI.clearCompletedBatches()
    addToast('info', 'Cleared completed batches from list')
  }

  const handleCheckAppUpdate = async () => {
    const update = await window.stashAPI.checkAppUpdate()
    setAppUpdate(update)
    return update
  }

  const handleOpenUrl = async (url: string) => {
    await window.stashAPI.openExternalUrl(url)
  }

  const batchList = Object.values(batches)

  return (
    <div className="app-container">
      <Header
        depStatus={depStatus}
        appUpdate={appUpdate}
        onOpenDepModal={() => setIsDepModalOpen(true)}
      />

      <div className="content-scrollable">
        <SettingsBar
          outputDir={outputDir}
          quality={quality}
          format={format}
          theme={theme}
          onSelectDir={handleSelectDir}
          onOpenDir={handleOpenDir}
          onChangeQuality={handleChangeQuality}
          onChangeFormat={handleChangeFormat}
          onChangeTheme={(t) => setTheme(t)}
        />

        <LinkInputBar isFetching={isFetching} fetchingMessage={fetchingMessage} onFetch={handleFetch} />

        {/* Batches Queue Section */}
        <div>
          <div className="batch-section-header">
            <h2 className="section-title">
              <ListMusic size={18} color="#6366f1" />
              <span>Download Queue</span>
              <span className="count-badge">{batchList.length} Batches</span>
            </h2>

            {batchList.length > 0 && (
              <button className="btn btn-outline" style={{ fontSize: '12px', padding: '6px 12px' }} onClick={handleClearCompleted}>
                <Trash2 size={14} /> Clear Completed
              </button>
            )}
          </div>

          <div style={{ marginTop: '14px' }}>
            {batchList.length === 0 ? (
              <div className="card empty-state">
                <div className="empty-icon">
                  <ListMusic size={32} />
                </div>
                <h3>Your Download Queue is Empty</h3>
                <p style={{ fontSize: '13px', maxWidth: '420px' }}>
                  Paste any YouTube or YouTube Music link, playlist, album, or artist name in the box above to start downloading.
                </p>
              </div>
            ) : (
              batchList.map((batch) => (
                <BatchItem
                  key={batch.id}
                  batch={batch}
                  onStartBatch={(bId) => window.stashAPI.startBatchDownload(bId)}
                  onCancelBatch={(bId) => window.stashAPI.cancelBatch(bId)}
                  onRemoveBatch={(bId) => window.stashAPI.removeBatch(bId)}
                  onStartTrack={(bId, tId) => window.stashAPI.startTrackDownload(bId, tId)}
                  onCancelTrack={(bId, tId) => window.stashAPI.cancelTrack(bId, tId)}
                  onRemoveTrack={(bId, tId) => window.stashAPI.removeTrack(bId, tId)}
                  onOpenFile={(filePath) => window.stashAPI.openFile(filePath)}
                  onOpenFolder={(folderPath) => window.stashAPI.openDirectory(folderPath)}
                />
              ))
            )}
          </div>
        </div>
      </div>

      <DependencyModal
        isOpen={isDepModalOpen}
        status={depStatus}
        appUpdate={appUpdate}
        onClose={() => setIsDepModalOpen(false)}
        onRefresh={handleRefreshDeps}
        onUpdateYtDlp={() => window.stashAPI.updateYtDlp()}
        onCheckAppUpdate={handleCheckAppUpdate}
        onOpenUrl={handleOpenUrl}
      />

      <Toast toasts={toasts} onDismiss={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))} />
    </div>
  )
}
export default App
