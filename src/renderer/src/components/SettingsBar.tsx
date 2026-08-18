import React from 'react'
import { Folder, FolderOpen, Sliders, Music2 } from 'lucide-react'
import { DownloadQuality, DownloadFormat } from '../../../shared/types'

interface SettingsBarProps {
  outputDir: string
  quality: DownloadQuality
  format: DownloadFormat
  onSelectDir: () => void
  onOpenDir: () => void
  onChangeQuality: (q: DownloadQuality) => void
  onChangeFormat: (f: DownloadFormat) => void
}

export const SettingsBar: React.FC<SettingsBarProps> = ({
  outputDir,
  quality,
  format,
  onSelectDir,
  onOpenDir,
  onChangeQuality,
  onChangeFormat
}) => {
  return (
    <div className="controls-grid">
      {/* Output Folder Picker */}
      <div className="control-group">
        <label className="control-label">
          <Folder size={14} /> Output Directory
        </label>
        <div className="path-picker-box">
          <span className="path-text" title={outputDir}>
            {outputDir || 'Selecting default folder...'}
          </span>
          <button className="btn btn-secondary btn-icon-only" onClick={onSelectDir} title="Browse Directory">
            <Folder size={16} />
          </button>
          <button className="btn btn-secondary btn-icon-only" onClick={onOpenDir} title="Open in File Explorer">
            <FolderOpen size={16} />
          </button>
        </div>
      </div>

      {/* Quality Selector */}
      <div className="control-group">
        <label className="control-label">
          <Sliders size={14} /> Quality Preset
        </label>
        <select
          className="select-box"
          value={quality}
          onChange={(e) => onChangeQuality(e.target.value as DownloadQuality)}
        >
          <option value="4K">4K Ultra HD (2160p)</option>
          <option value="2K">2K Quad HD (1440p)</option>
          <option value="HIGH">High (1080p / 320kbps)</option>
          <option value="MID">Mid (720p / 192kbps)</option>
          <option value="LOW">Low (360p / 128kbps)</option>
        </select>
      </div>

      {/* Format Selector */}
      <div className="control-group">
        <label className="control-label">
          <Music2 size={14} /> Target Format
        </label>
        <select
          className="select-box"
          value={format}
          onChange={(e) => onChangeFormat(e.target.value as DownloadFormat)}
        >
          <option value="AUTO">Auto-Detect</option>
          <option value="MP3">MP3 Audio (.mp3)</option>
          <option value="AAC">AAC Audio (.m4a)</option>
          <option value="FLAC">FLAC Lossless (.flac)</option>
          <option value="OPUS">Opus Audio (.opus)</option>
          <option value="WAV">WAV Audio (.wav)</option>
          <option value="MP4">MP4 Video (.mp4)</option>
        </select>
      </div>
    </div>
  )
}
