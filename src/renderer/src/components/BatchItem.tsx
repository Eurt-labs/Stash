import React, { useState } from 'react'
import {
  ChevronDown,
  Play,
  XCircle,
  Trash2,
  ListMusic,
  FolderOpen
} from 'lucide-react'
import { DownloadBatch } from '../../../shared/types'
import { TrackCard } from './TrackCard'

interface BatchItemProps {
  batch: DownloadBatch
  onStartBatch: (batchId: string) => void
  onCancelBatch: (batchId: string) => void
  onRemoveBatch: (batchId: string) => void
  onStartTrack: (batchId: string, trackId: string) => void
  onCancelTrack: (batchId: string, trackId: string) => void
  onRemoveTrack: (batchId: string, trackId: string) => void
  onOpenFile: (filePath: string) => void
  onOpenFolder: (folderPath: string) => void
}

export const BatchItem: React.FC<BatchItemProps> = ({
  batch,
  onStartBatch,
  onCancelBatch,
  onRemoveBatch,
  onStartTrack,
  onCancelTrack,
  onRemoveTrack,
  onOpenFile,
  onOpenFolder
}) => {
  const [isExpanded, setIsExpanded] = useState(true)

  const completedCount = batch.items.filter((i) => i.state === 'COMPLETED').length
  const totalCount = batch.items.length
  const isAllCompleted = completedCount === totalCount && totalCount > 0
  const isWorking = batch.items.some(
    (i) => i.state === 'DOWNLOADING' || i.state === 'CONVERTING' || i.state === 'TAGGING'
  )
  const activeItem = batch.items.find((i) => i.state === 'DOWNLOADING')
  const batchEta = activeItem?.eta ? `ETA ${activeItem.eta}` : null

  return (
    <div className="batch-card">
      <div
        className="batch-header"
        onClick={() => setIsExpanded(!isExpanded)}
        role="button"
        tabIndex={0}
        title={isExpanded ? 'Click to collapse playlist' : 'Click to expand playlist'}
      >
        <div className="batch-info">
          <div className={`chevron-indicator ${isExpanded ? 'is-open' : ''}`}>
            <ChevronDown size={18} />
          </div>
          <ListMusic size={18} color="var(--primary)" />
          <span className="batch-title">{batch.name}</span>
          <div className="batch-meta">
            <span className="count-badge">
              {completedCount} / {totalCount} Done
            </span>
            {isWorking && batchEta && <span className="tag-pill" style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#3b82f6' }}>{batchEta}</span>}
            <span className="tag-pill">{batch.format}</span>
            <span className="tag-pill">{batch.quality}</span>
          </div>
        </div>

        <div className="batch-actions" onClick={(e) => e.stopPropagation()}>
          <button
            className="btn btn-secondary btn-icon-only"
            onClick={() => onOpenFolder(batch.outputDir)}
            title="Open batch download folder"
          >
            <FolderOpen size={16} />
          </button>

          {!isAllCompleted && !isWorking && (
            <button
              className="btn btn-primary"
              style={{ padding: '6px 12px', fontSize: '12px' }}
              onClick={() => onStartBatch(batch.id)}
            >
              <Play size={14} /> Download All
            </button>
          )}

          {isWorking && (
            <button
              className="btn btn-danger"
              style={{ padding: '6px 12px', fontSize: '12px' }}
              onClick={() => onCancelBatch(batch.id)}
            >
              <XCircle size={14} /> Cancel
            </button>
          )}

          <button
            className="btn btn-outline btn-icon-only"
            onClick={() => onRemoveBatch(batch.id)}
            title="Delete batch"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      {/* Smooth CSS Grid Accordion Collapse / Expand with Cascading Card Stagger */}
      <div className={`batch-accordion-wrapper ${isExpanded ? 'is-open' : ''}`}>
        <div className="batch-accordion-inner">
          <div className="track-list">
            {batch.items.map((item, idx) => (
              <div
                key={item.id}
                className="track-card-animated"
                style={{ animationDelay: `${Math.min(idx * 0.03, 0.36)}s` }}
              >
                <TrackCard
                  item={item}
                  onDownload={() => onStartTrack(batch.id, item.id)}
                  onCancel={() => onCancelTrack(batch.id, item.id)}
                  onRemove={() => onRemoveTrack(batch.id, item.id)}
                  onOpenFile={() => item.finalFilePath && onOpenFile(item.finalFilePath)}
                />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
