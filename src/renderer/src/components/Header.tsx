import React from 'react'
import { Sparkles, ShieldCheck, ShieldAlert, RefreshCw } from 'lucide-react'
import { DependencyStatus, AppUpdateStatus } from '../../../shared/types'
import { DownloadCloud } from 'lucide-react'

interface HeaderProps {
  depStatus: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  onOpenDepModal: () => void
}

export const Header: React.FC<HeaderProps> = ({ depStatus, appUpdate, onOpenDepModal }) => {
  const allDepsOk = depStatus && depStatus.ytDlpInstalled && depStatus.ffmpegInstalled

  return (
    <header className="header-container">
      <div className="brand-section">
        <img
          src="/stash_logo.png"
          alt="Stash Logo"
          className="brand-logo"
          style={{ width: '44px', height: '44px', borderRadius: '12px', objectFit: 'cover' }}
        />
        <div className="brand-info">
          <h1>Stash Downloader</h1>
          <p>Download, convert, and tag media from YouTube, YouTube Music, and more ~By eurt-labs</p>
        </div>
      </div>

      <div className="header-actions">
        {appUpdate?.hasUpdate && (
          <button
            className="btn btn-primary"
            onClick={onOpenDepModal}
            title="A newer version of Stash is available on GitHub"
            style={{ animation: 'pulse-dot 2s infinite' }}
          >
            <DownloadCloud size={16} />
            <span>Update v{appUpdate.latestVersion} Available!</span>
          </button>
        )}

        <button
          className={`btn ${allDepsOk ? 'btn-secondary' : 'btn-danger'}`}
          onClick={onOpenDepModal}
          title="Check & manage external tool dependencies and updates"
        >
          {allDepsOk ? <ShieldCheck size={16} color="#10b981" /> : <ShieldAlert size={16} color="#ef4444" />}
          <span>{allDepsOk ? 'Tools Ready' : 'Tools Required'}</span>
        </button>
      </div>
    </header>
  )
}
