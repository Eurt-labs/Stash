import React from 'react'
import { Settings, DownloadCloud } from 'lucide-react'
import { DependencyStatus, AppUpdateStatus } from '../../../shared/types'

interface HeaderProps {
  depStatus: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  onOpenDepModal: () => void
}

export const Header: React.FC<HeaderProps> = ({ appUpdate, onOpenDepModal }) => {
  return (
    <header className="header-container">
      <div className="brand-section">
        <img
          src="/stash_logo.png"
          alt="Stash Logo"
          className="brand-logo"
          style={{ width: '40px', height: '40px', borderRadius: '10px', objectFit: 'cover' }}
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
            <DownloadCloud size={15} />
            <span>Update v{appUpdate.latestVersion} Available!</span>
          </button>
        )}

        <button
          className="btn btn-secondary"
          onClick={onOpenDepModal}
          title="Settings, Themes & System Updates"
        >
          <Settings size={15} />
          <span>Settings</span>
        </button>
      </div>
    </header>
  )
}
