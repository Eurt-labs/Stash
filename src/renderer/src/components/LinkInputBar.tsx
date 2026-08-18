import React, { useState } from 'react'
import { Search, Loader2, Download, Clipboard } from 'lucide-react'

interface LinkInputBarProps {
  isFetching: boolean
  fetchingMessage: string
  onFetch: (url: string) => void
}

export const LinkInputBar: React.FC<LinkInputBarProps> = ({ isFetching, fetchingMessage, onFetch }) => {
  const [inputValue, setInputValue] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (inputValue.trim() && !isFetching) {
      onFetch(inputValue.trim())
      setInputValue('')
    }
  }

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText()
      if (text && text.trim()) {
        setInputValue(text.trim())
      }
    } catch (e) {
      console.warn('Could not read clipboard:', e)
    }
  }

  return (
    <div className="search-bar-card">
      <Search size={20} color="#6366f1" />
      <form onSubmit={handleSubmit} style={{ display: 'flex', flex: 1, alignItems: 'center', gap: '10px' }}>
        <input
          type="text"
          className="search-input"
          placeholder="Paste YouTube / YouTube Music URL, Playlist, or Artist Name (e.g. Taylor Swift)..."
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          disabled={isFetching}
        />

        {inputValue ? (
          <button
            type="button"
            className="btn btn-outline"
            style={{ padding: '6px 10px', fontSize: '12px' }}
            onClick={() => setInputValue('')}
            disabled={isFetching}
          >
            Clear
          </button>
        ) : (
          <button
            type="button"
            className="btn btn-outline"
            style={{ padding: '6px 10px', fontSize: '12px' }}
            onClick={handlePaste}
            title="Paste from clipboard"
            disabled={isFetching}
          >
            <Clipboard size={14} /> Paste
          </button>
        )}

        <button
          type="submit"
          className="btn btn-primary"
          disabled={!inputValue.trim() || isFetching}
          style={{ minWidth: '130px' }}
        >
          {isFetching ? (
            <>
              <Loader2 size={16} className="status-dot pulsing" />
              <span>Fetching...</span>
            </>
          ) : (
            <>
              <Download size={16} />
              <span>Fetch & Add</span>
            </>
          )}
        </button>
      </form>
    </div>
  )
}
