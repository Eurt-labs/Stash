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
  DownloadCloud,
  Palette,
  Sliders
} from 'lucide-react'
import { DependencyStatus, AppUpdateStatus, ColorTheme } from '../../../shared/types'

interface SettingsModalProps {
  status: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  theme: ColorTheme
  isOpen: boolean
  onClose: () => void
  onChangeTheme: (t: ColorTheme) => void
  onRefresh: () => Promise<void>
  onUpdateYtDlp: () => Promise<{ success: boolean; message: string }>
  onCheckAppUpdate: () => Promise<AppUpdateStatus>
  onOpenUrl: (url: string) => void
}

const THEME_OPTIONS: Array<{ id: ColorTheme; label: string; dotGradient: string }> = [
  { id: 'indigo', label: 'Neon Indigo', dotGradient: 'linear-gradient(135deg, #6366f1, #a855f7)' },
  { id: 'emerald', label: 'Cyber Emerald', dotGradient: 'linear-gradient(135deg, #10b981, #06b6d4)' },
  { id: 'sunset', label: 'Sunset Rose', dotGradient: 'linear-gradient(135deg, #f43f5e, #ec4899)' },
  { id: 'sapphire', label: 'Ocean Sapphire', dotGradient: 'linear-gradient(135deg, #3b82f6, #06b6d4)' },
  { id: 'amber', label: 'Midnight Amber', dotGradient: 'linear-gradient(135deg, #f59e0b, #ef4444)' },
  { id: 'crimson', label: 'Blood Crimson', dotGradient: 'linear-gradient(135deg, #ef4444, #f97316)' },
  { id: 'oled', label: 'OLED Minimal', dotGradient: 'linear-gradient(135deg, #ffffff, #64748b)' }
]

export const DependencyModal: React.FC<SettingsModalProps> = ({
  status,
  appUpdate,
  theme,
  isOpen,
  onClose,
  onChangeTheme,
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
            <h2>Settings & Updates</h2>
          </div>
          <button className="btn btn-outline btn-icon-only" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        {/* ── Section 1: Color Theme Customization ── */}
        <div style={{ marginBottom: '22px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
            <Palette size={14} color="var(--primary)" />
            <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.5px' }}>
              Color Theme Palette
            </span>
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
              <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.5px' }}>
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

          <div className="dep-item" style={{ background: 'rgba(255, 255, 255, 0.02)', borderColor: 'var(--border-subtle)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <img
                src="/stash_logo.png"
                alt="Logo"
                style={{ width: '38px', height: '38px', borderRadius: '10px', objectFit: 'cover' }}
              />
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

        {/* ── Section 3: External Tool Dependencies (yt-dlp & FFmpeg) ── */}
        <div>
          <span style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-secondary)', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }}>
            Core Engine & Binaries
          </span>

          {/* yt-dlp Status */}
          <div className="dep-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Cpu size={22} color="var(--primary)" />
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
              <Film size={22} color="var(--secondary)" />
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
