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
  ExternalLink,
  Github,
  DownloadCloud,
  Palette,
  Sliders,
  Sun,
  Moon
} from 'lucide-react'
import { DependencyStatus, AppUpdateStatus, ColorTheme, ThemeMode } from '../../../shared/types'
import { Logo } from './Logo'

interface SettingsModalProps {
  status: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  theme: ColorTheme
  mode: ThemeMode
  isOpen: boolean
  onClose: () => void
  onChangeTheme: (t: ColorTheme) => void
  onChangeMode: (m: ThemeMode) => void
  onRefresh: () => Promise<void>
  onUpdateYtDlp: () => Promise<{ success: boolean; message: string }>
  onCheckAppUpdate: () => Promise<AppUpdateStatus>
  onOpenUrl: (url: string) => void
}

const THEME_OPTIONS: Array<{ id: ColorTheme; label: string; dotGradient: string }> = [
  { id: 'indigo', label: 'Indigo', dotGradient: '#6366f1' },
  { id: 'emerald', label: 'Green', dotGradient: '#10b981' },
  { id: 'sunset', label: 'Pink', dotGradient: '#f43f5e' },
  { id: 'sapphire', label: 'Blue', dotGradient: '#3b82f6' },
  { id: 'amber', label: 'Yellow', dotGradient: '#f59e0b' },
  { id: 'crimson', label: 'Red', dotGradient: '#ef4444' },
  { id: 'oled', label: 'Monochrome', dotGradient: '#ffffff' }
]

export const DependencyModal: React.FC<SettingsModalProps> = ({
  status,
  appUpdate,
  theme,
  mode,
  isOpen,
  onClose,
  onChangeTheme,
  onChangeMode,
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
      <div className="modal-card" style={{ maxWidth: '620px', maxHeight: '90vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Sliders size={20} color="var(--primary)" />
            <h2>Settings & Preferences</h2>
          </div>
          <button className="btn btn-outline btn-icon-only" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        {/* ── Section 1: Appearance (Dark / Light Mode & Color Palettes) ── */}
        <div style={{ marginBottom: '22px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Palette size={14} color="var(--primary)" />
              <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.05em' }}>
                Appearance & Liquid Glass
              </span>
            </div>

            {/* Mode Pills (Dark / Light) */}
            <div style={{ display: 'flex', gap: '4px', background: 'var(--glass-input)', padding: '3px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
              <button
                type="button"
                className="btn btn-icon-only"
                style={{
                  padding: '4px 12px',
                  fontSize: '11.5px',
                  borderRadius: 'var(--radius-xs)',
                  background: mode === 'dark' ? 'var(--primary)' : 'transparent',
                  color: mode === 'dark' ? '#ffffff' : 'var(--text-secondary)',
                  border: 'none'
                }}
                onClick={() => onChangeMode('dark')}
              >
                <Moon size={12} /> Dark
              </button>
              <button
                type="button"
                className="btn btn-icon-only"
                style={{
                  padding: '4px 12px',
                  fontSize: '11.5px',
                  borderRadius: 'var(--radius-xs)',
                  background: mode === 'light' ? 'var(--primary)' : 'transparent',
                  color: mode === 'light' ? '#ffffff' : 'var(--text-secondary)',
                  border: 'none'
                }}
                onClick={() => onChangeMode('light')}
              >
                <Sun size={12} /> Light
              </button>
            </div>
          </div>

          <div className="theme-swatches-grid">
            {THEME_OPTIONS.map((t) => (
              <div
                key={t.id}
                className={`theme-swatch-card ${theme === t.id ? 'active' : ''}`}
                onClick={() => onChangeTheme(t.id)}
              >
                <div className="theme-preview-dot" style={{ background: t.dotGradient }} />
                <span className="theme-swatch-label">{t.label}</span>
                {theme === t.id && (
                  <span style={{ position: 'absolute', top: '6px', right: '6px', fontSize: '10px', color: 'var(--primary)' }}>
                    ✓
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* ── Section 2: Stash Application Version & GitHub Update Check ── */}
        <div style={{ marginBottom: '22px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Github size={14} color="var(--primary)" />
              <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.05em' }}>
                Stash Application (GitHub)
              </span>
            </div>
            <button
              className="btn btn-outline"
              style={{ padding: '4px 8px', fontSize: '11px' }}
              onClick={() => onOpenUrl(repoUrl)}
              title="Open repository on GitHub"
            >
              <Github size={12} /> Eurt-labs/Stash <ExternalLink size={10} />
            </button>
          </div>

          <div className="dep-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Logo size={36} />
              <div>
                <div style={{ fontWeight: 700, fontSize: '13.5px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  Stash Media Downloader
                  <span className="tag-pill" style={{ fontSize: '10px' }}>
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
              <span className="status-badge" style={{ background: 'var(--primary-muted)', color: 'var(--primary-light)', border: '1px solid var(--border-medium)' }}>
                <DownloadCloud size={14} /> Update v{appUpdateData.latestVersion}
              </span>
            ) : (
              <span className="status-badge completed">
                <CheckCircle2 size={14} /> Up to Date
              </span>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
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

        {/* ── Section 3: Core Engine Tools (yt-dlp & FFmpeg) ── */}
        <div>
          <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.05em', display: 'block', marginBottom: '8px' }}>
            Core Engine & Binaries
          </span>

          {/* yt-dlp Status */}
          <div className="dep-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Cpu size={20} color="var(--primary)" />
              <div>
                <div style={{ fontWeight: 700, fontSize: '13.5px' }}>yt-dlp Engine</div>
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
              <Film size={20} color="var(--secondary)" />
              <div>
                <div style={{ fontWeight: 700, fontSize: '13.5px' }}>FFmpeg Transcoder</div>
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
                borderRadius: 'var(--radius-sm)',
                background: 'var(--glass-card-subtle)',
                border: '1px solid var(--border-subtle)',
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
