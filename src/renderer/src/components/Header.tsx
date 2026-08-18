import React from 'react'
import { Settings, DownloadCloud, Sun, Moon } from 'lucide-react'
import { DependencyStatus, AppUpdateStatus, ThemeMode } from '../../../shared/types'
import { Logo } from './Logo'

interface HeaderProps {
  depStatus: DependencyStatus | null
  appUpdate: AppUpdateStatus | null
  mode: ThemeMode
  onToggleMode: () => void
  onOpenDepModal: () => void
}

export const Header: React.FC<HeaderProps> = ({
  appUpdate,
  mode,
  onToggleMode,
  onOpenDepModal
}) => {
  return (
    <header className="header-container">
      <div className="brand-section">
        <Logo size={38} />
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

        {/* Light / Dark Mode Toggle Button */}
        <button
          className="btn btn-secondary btn-icon-only"
          onClick={onToggleMode}
          title={mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
        >
          {mode === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
        </button>

        {/* Settings Button */}
        <button
          className="btn btn-secondary"
          onClick={onOpenDepModal}
          title="Settings, Appearance & System Updates"
        >
          <Settings size={15} />
          <span>Settings</span>
        </button>
      </div>
    </header>
  )
}
