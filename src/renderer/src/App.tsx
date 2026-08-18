import React, { useState, useEffect } from 'react'
import {
  DownloadBatch,
  DownloadQuality,
  DownloadFormat,
  ColorTheme,
  ThemeMode,
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
import { FloatingPaths } from './components/BackgroundPaths'
import { ListMusic, Trash2 } from 'lucide-react'

export const App: React.FC = () => {
  const [batches, setBatches] = useState<Record<string, DownloadBatch>>({})
  const [outputDir, setOutputDir] = useState('')
  const [quality, setQuality] = useState<DownloadQuality>('HIGH')
  const [format, setFormat] = useState<DownloadFormat>('MP3')
  const [theme, setTheme] = useState<ColorTheme>(() => {
    return (localStorage.getItem('stash_theme') as ColorTheme) || 'indigo'
  })
  const [mode, setMode] = useState<ThemeMode>(() => {
    return (localStorage.getItem('stash_mode') as ThemeMode) || 'dark'
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

  // Sync theme palette
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    document.body.setAttribute('data-theme', theme)
    localStorage.setItem('stash_theme', theme)
  }, [theme])

  // Sync dark/light mode
  useEffect(() => {
    document.documentElement.setAttribute('data-mode', mode)
    document.body.setAttribute('data-mode', mode)
    localStorage.setItem('stash_mode', mode)
  }, [mode])

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
      addToast('success', `Added ${tracks.length} track${tracks.length > 1 ? 's' : ''} to download queue!`)
    } catch (err: any) {
      addToast('error', err.message || 'Failed to fetch tracks')
    }
  }

  const handleSelectDir = async () => {
    const dir = await window.stashAPI.selectDirectory()
    if (dir) {
      setOutputDir(dir)
      await window.stashAPI.setOutputDir(dir)
      addToast('info', `Output folder updated: ${dir}`)
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

    if (f === 'FLAC' || f === 'WAV') {
      if (quality !== 'HIGH') {
        setQuality('HIGH')
        await window.stashAPI.setQuality('HIGH')
      }
    } else if (f === 'MP3' || f === 'AAC' || f === 'OPUS') {
      if (quality === '4K' || quality === '2K') {
        setQuality('HIGH')
        await window.stashAPI.setQuality('HIGH')
      }
    }
  }

  const handleClearCompleted = async () => {
    await window.stashAPI.clearCompletedBatches()
  }

  const handleRefreshDeps = async () => {
    const status = await window.stashAPI.checkDependencies()
    setDepStatus(status)
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
  const isArtistTheme = ['weeknd', 'taylor', 'billie', 'daftpunk', 'travis', 'lana'].includes(theme)

  return (
    <div className="app-container">
      {/* 1. Dedicated Artist Artwork Backdrop (Clean Artwork with Zero Line Clutter) */}
      {isArtistTheme ? (
        <div
          className="artist-backdrop"
          style={{
            backgroundImage: `url('./artists/${theme}.svg')`
          }}
        />
      ) : (
        /* 2. Animated Liquid Glass Shader & Flowing Ribbon Paths for Core Color Palettes */
        <FloatingPaths theme={theme} mode={mode} />
      )}

      <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', height: '100%', gap: '16px' }}>
        <Header
          appUpdate={appUpdate}
          mode={mode}
          onToggleMode={() => setMode((m) => (m === 'dark' ? 'light' : 'dark'))}
          onOpenDepModal={() => setIsDepModalOpen(true)}
        />

        <div className="content-scrollable">
          <SettingsBar
            outputDir={outputDir}
            quality={quality}
            format={format}
            onSelectDir={handleSelectDir}
            onOpenDir={handleOpenDir}
            onChangeQuality={handleChangeQuality}
            onChangeFormat={handleChangeFormat}
          />

          <LinkInputBar isFetching={isFetching} fetchingMessage={fetchingMessage} onFetch={handleFetch} />

          {/* Batches Queue Section */}
          <div>
            <div className="batch-section-header">
              <h2 className="section-title">
                <ListMusic size={18} color="var(--primary)" />
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
                  <h3 style={{ fontSize: '15px', fontWeight: 700 }}>Your Download Queue is Empty</h3>
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
          theme={theme}
          mode={mode}
          onClose={() => setIsDepModalOpen(false)}
          onChangeTheme={(t) => setTheme(t)}
          onChangeMode={(m) => setMode(m)}
          onRefresh={handleRefreshDeps}
          onUpdateYtDlp={() => window.stashAPI.updateYtDlp()}
          onCheckAppUpdate={handleCheckAppUpdate}
          onOpenUrl={handleOpenUrl}
        />

        <Toast toasts={toasts} onDismiss={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))} />
      </div>
    </div>
  )
}
export default App
