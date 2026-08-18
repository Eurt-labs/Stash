import React from 'react'
import {
  Play,
  RotateCcw,
  XCircle,
  Trash2,
  FolderCheck,
  Music,
  CheckCircle2,
  AlertCircle,
  Clock
} from 'lucide-react'
import { DownloadItem } from '../../../shared/types'

interface TrackCardProps {
  item: DownloadItem
  onDownload: () => void
  onCancel: () => void
  onRemove: () => void
  onOpenFile: () => void
}

export const TrackCard: React.FC<TrackCardProps> = ({
  item,
  onDownload,
  onCancel,
  onRemove,
  onOpenFile
}) => {
  const { trackInfo, state, progress, speed, eta, statusMessage, format, quality } = item

  const formatDuration = (ms: number) => {
    if (!ms) return '0:00'
    const totalSec = Math.floor(ms / 1000)
    const minutes = Math.floor(totalSec / 60)
    const seconds = totalSec % 60
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`
  }

  const renderStatusBadge = () => {
    switch (state) {
      case 'QUEUED':
        return (
          <span className="status-badge queued">
            <Clock size={12} /> Queued
          </span>
        )
      case 'DOWNLOADING':
        return (
          <span className="status-badge downloading">
            <span className="status-dot pulsing" /> {statusMessage || 'Downloading'}
          </span>
        )
      case 'CONVERTING':
        return (
          <span className="status-badge converting">
            <span className="status-dot pulsing" /> {statusMessage || 'Converting'}
          </span>
        )
      case 'TAGGING':
        return (
          <span className="status-badge tagging">
            <span className="status-dot pulsing" /> Tagging
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="status-badge completed">
            <CheckCircle2 size={12} /> Completed
          </span>
        )
      case 'FAILED':
        return (
          <span className="status-badge failed" title={item.errorMessage || 'Error'}>
            <AlertCircle size={12} /> Failed
          </span>
        )
      case 'CANCELLED':
        return (
          <span className="status-badge cancelled">
            <XCircle size={12} /> Cancelled
          </span>
        )
      default:
        return null
    }
  }

  const isWorking = state === 'DOWNLOADING' || state === 'CONVERTING' || state === 'TAGGING'

  return (
    <div className="track-card">
      {/* Artwork Thumbnail */}
      {trackInfo.albumArtUrl ? (
        <img
          src={trackInfo.albumArtUrl}
          alt={trackInfo.title}
          className="track-art"
          onError={(e) => {
            ;(e.target as HTMLElement).style.display = 'none'
          }}
        />
      ) : (
        <div className="track-art" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Music size={20} color="#94a3b8" />
        </div>
      )}

      {/* Metadata */}
      <div className="track-info">
        <span className="track-title" title={trackInfo.title}>
          {trackInfo.title}
        </span>
        <div className="track-artist-line">
          <span>{trackInfo.artists.join(', ')}</span>
          {trackInfo.album && <span>• {trackInfo.album}</span>}
          <span>• {formatDuration(trackInfo.durationMs)}</span>
        </div>
        {state === 'FAILED' && item.errorMessage && (
          <div
            style={{
              fontSize: '11px',
              color: '#f87171',
              marginTop: '2px',
              fontWeight: 500,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              maxWidth: '340px'
            }}
            title={item.errorMessage}
          >
            ⚠️ {item.errorMessage}
          </div>
        )}
      </div>

      {/* Format & Quality Badges */}
      <span className="tag-pill">{format}</span>
      <span className="tag-pill">{quality}</span>

      {/* Real-time Progress Bar */}
      {isWorking ? (
        <div className="track-progress-container">
          <div className="progress-bar-bg">
            <div className="progress-bar-fill" style={{ width: `${Math.min(Math.max(progress, 0), 100)}%` }} />
          </div>
          <div className="progress-stats">
            <span>{speed || (state === 'CONVERTING' ? 'Transcoding' : 'Processing')}</span>
            <span>{eta ? `ETA ${eta}` : `${Math.round(progress)}%`}</span>
          </div>
        </div>
      ) : null}

      {/* Status Badge */}
      {renderStatusBadge()}

      {/* Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
        {state === 'COMPLETED' && (
          <button
            className="btn btn-secondary btn-icon-only"
            onClick={onOpenFile}
            title="Locate file in File Explorer"
          >
            <FolderCheck size={16} color="#10b981" />
          </button>
        )}

        {(state === 'FAILED' || state === 'CANCELLED') && (
          <button className="btn btn-secondary btn-icon-only" onClick={onDownload} title="Retry Download">
            <RotateCcw size={16} />
          </button>
        )}

        {isWorking && (
          <button className="btn btn-danger btn-icon-only" onClick={onCancel} title="Cancel Download">
            <XCircle size={16} />
          </button>
        )}

        <button className="btn btn-outline btn-icon-only" onClick={onRemove} title="Remove from list">
          <Trash2 size={16} />
        </button>
      </div>
    </div>
  )
}
