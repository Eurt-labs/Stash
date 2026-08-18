import React, { useState, useEffect } from 'react'
import {
  X,
  ShieldCheck,
  ShieldAlert,
  RefreshCw,
  Cpu,
  Film,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ExternalLink,
  Github,
  DownloadCloud
} from 'lucide-react'
import { DependencyStatus, AppUpdateStatus } from '../../../shared/types'

interface DependencyModalProps {
  status: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  isOpen: boolean
  onClose: () => void
  onRefresh: () => Promise<void>
  onUpdateYtDlp: () => Promise<{ success: boolean; message: string }>
  onCheckAppUpdate: () => Promise<AppUpdateStatus>
  onOpenUrl: (url: string) => void
}

export const DependencyModal: React.FC<DependencyModalProps> = ({
  status,
  appUpdate,
  isOpen,
  onClose,
  onRefresh,
  onUpdateYtDlp,
  onCheckAppUpdate,
  onOpenUrl
}) => {
  const [isUpdatingYtDlp, setIsUpdatingYtDlp] = useState(false)
  const [ytDlpMsg, setYtDlpMsg] = useState<string | null>(null)

  const [isCheckingApp, setIsCheckingApp] = useState(false)
  const [appUpdateData, setAppUpdateData] = useState<AppUpdateStatus | null>(appUpdate)

  useEffect(() => {
    setAppUpdateData(appUpdate)
  }, [appUpdate])

  if (!isOpen) return null

  const handleUpdateYtDlp = async () => {
    setIsUpdatingYtDlp(true)
    setYtDlpMsg(null)
    try {
      const res = await onUpdateYtDlp()
      setYtDlpMsg(res.message)
      await onRefresh()
    } catch (e: any) {
      setYtDlpMsg(e.message || 'Update failed')
    } finally {
      setIsUpdatingYtDlp(false)
    }
  }

  const handleCheckAppUpdate = async () => {
    setIsCheckingApp(true)
    try {
      const res = await onCheckAppUpdate()
      setAppUpdateData(res)
    } catch (e) {
      console.error('App update check error:', e)
    } finally {
      setIsCheckingApp(false)
    }
  }

  const repoUrl = 'https://github.com/Eurt-labs/Stash'

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '580px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>System & Updates</h2>
          <button className="btn btn-outline btn-icon-only" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        {/* ── Section 1: Stash Application Version & GitHub Update Check ── */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.5px' }}>
              Stash Application (GitHub)
            </span>
            <button
              className="btn btn-outline"
              style={{ padding: '4px 8px', fontSize: '11px' }}
              onClick={() => onOpenUrl(repoUrl)}
              title="Open repository on GitHub"
            >
              <Github size={12} /> Eurt-labs/Stash <ExternalLink size={10} />
            </button>
          </div>

          <div className="dep-item" style={{ background: 'rgba(99, 102, 241, 0.06)', borderColor: 'rgba(99, 102, 241, 0.2)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: 'var(--radius-sm)',
                  background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white'
                }}
              >
                <Sparkles size={18} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  Stash Media Downloader
                  <span className="brand-badge" style={{ fontSize: '10px' }}>
                    v{appUpdateData?.currentVersion || '2.0.0'}
                  </span>
                </div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  {appUpdateData?.latestVersion
                    ? `Latest GitHub release: v${appUpdateData.latestVersion}`
                    : 'Checking latest version from GitHub...'}
                </div>
              </div>
            </div>

            {appUpdateData?.hasUpdate ? (
              <span className="status-badge" style={{ background: 'rgba(99, 102, 241, 0.25)', color: '#c7d2fe', border: '1px solid rgba(99, 102, 241, 0.4)' }}>
                <DownloadCloud size={14} /> Update v{appUpdateData.latestVersion}
              </span>
            ) : (
              <span className="status-badge completed">
                <CheckCircle2 size={14} /> Up to Date
              </span>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px' }}>
            <button
              className="btn btn-secondary"
              style={{ fontSize: '12px', padding: '6px 12px' }}
              onClick={handleCheckAppUpdate}
              disabled={isCheckingApp}
            >
              {isCheckingApp ? <RefreshCw size={13} className="status-dot pulsing" /> : <RefreshCw size={13} />}
              <span>{isCheckingApp ? 'Checking GitHub...' : 'Check App Version'}</span>
            </button>

            {appUpdateData?.releaseUrl && (
              <button
                className="btn btn-primary"
                style={{ fontSize: '12px', padding: '6px 12px' }}
                onClick={() => onOpenUrl(appUpdateData.releaseUrl || repoUrl)}
              >
                <ExternalLink size={13} /> View on GitHub
              </button>
            )}
          </div>
        </div>

        {/* ── Section 2: External Tool Dependencies (yt-dlp & FFmpeg) ── */}
        <div>
          <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }}>
            Core Engine & Binaries
          </span>

          {/* yt-dlp Status */}
          <div className="dep-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Cpu size={22} color="#6366f1" />
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px' }}>yt-dlp Engine</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  {status?.ytDlpInstalled ? (status.ytDlpVersion || 'Installed') : 'Not Found'}
                </div>
              </div>
            </div>
            {status?.ytDlpInstalled ? (
              <span className="status-badge completed">
                <CheckCircle2 size={14} /> Ready
              </span>
            ) : (
              <span className="status-badge failed">
                <AlertCircle size={14} /> Missing
              </span>
            )}
          </div>

          {/* FFmpeg Status */}
          <div className="dep-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Film size={22} color="#10b981" />
              <div>
                <div style={{ fontWeight: 700, fontSize: '14px' }}>FFmpeg Transcoder</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  {status?.ffmpegInstalled ? (status.ffmpegVersion?.slice(0, 40) || 'Installed') : 'Not Found'}
                </div>
              </div>
            </div>
            {status?.ffmpegInstalled ? (
              <span className="status-badge completed">
                <CheckCircle2 size={14} /> Ready
              </span>
            ) : (
              <span className="status-badge failed">
                <AlertCircle size={14} /> Missing
              </span>
            )}
          </div>

          {ytDlpMsg && (
            <div
              style={{
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(255, 255, 255, 0.05)',
                fontSize: '12px',
                marginTop: '12px',
                fontFamily: 'var(--font-mono)',
                whiteSpace: 'pre-wrap'
              }}
            >
              {ytDlpMsg}
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '14px' }}>
            <button className="btn btn-secondary" style={{ fontSize: '12px', padding: '6px 12px' }} onClick={() => onRefresh()} title="Check binary paths">
              <RefreshCw size={13} /> Refresh Tools
            </button>
            <button className="btn btn-primary" style={{ fontSize: '12px', padding: '6px 12px' }} onClick={handleUpdateYtDlp} disabled={isUpdatingYtDlp}>
              {isUpdatingYtDlp ? <RefreshCw size={13} className="status-dot pulsing" /> : null}
              <span>{isUpdatingYtDlp ? 'Updating yt-dlp...' : 'Update yt-dlp'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
