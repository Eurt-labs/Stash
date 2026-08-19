# 🏛️ Stash Architecture & System Design

Stash is an Electron-based desktop media downloader designed for deterministic, non-blocking media extraction, transcoding, and metadata tagging.

---

## 🔄 5-Phase Sequential Processing Pipeline

To guarantee system stability, prevent CPU spikes, and prevent out-of-memory lockups during large playlist conversions (e.g. 100+ tracks), downloads are processed sequentially:

```
[Phase 1: Input & Parse]
        │
        ▼
[Phase 2: Metadata Extraction] ──► (Flat playlist mode + fallback search)
        │
        ▼
[Phase 3: Stream Download] ─────► (yt-dlp nightly VisionOS / anti-403)
        │
        ▼
[Phase 4: FFmpeg Transcode] ────► (320k MP3, FLAC, AAC, WAV, 4K MP4)
        │
        ▼
[Phase 5: Tag & Move] ──────────► (ID3v2.4 / RIFF INFO / 600x600 Baseline JPEG)
```

---

## 📂 Source Code Domain Modules

```
src/
├── main/                           # Electron Main Process (Node.js runtime)
│   ├── app.ts                      # Window lifecycle & initialization
│   ├── preload.ts                  # Secure context bridge IPC
│   ├── core/                       # Core abstractions & error types
│   │   ├── constants/              # Global engine constants
│   │   ├── errors/                 # Domain-specific typed error classes
│   │   └── utils/                  # Safe file system utilities
│   ├── features/                   # Business domain modules
│   │   ├── downloader/             # Stream downloading & batch queue orchestrator
│   │   ├── transcoder/             # Audio & video transcoding (FFmpeg)
│   │   ├── metadata/               # Audio tagging & album art normalization
│   │   ├── updater/                # In-app binary resolver & auto-bootstrap
│   │   └── parser/                 # Link & platform detector
│   └── ipc/                        # Modular IPC router & handlers
├── renderer/                       # React 18 Frontend UI (Chromium runtime)
│   ├── src/
│   │   ├── components/             # Reusable UI components
│   │   ├── index.css               # Design system & GPU micro-animations
│   │   ├── App.tsx                 # Root application state
│   │   └── main.tsx                # React DOM entry
│   └── index.html                  # HTML entry point
└── shared/                         # Cross-boundary types & contracts
    ├── types/                      # TypeScript definitions
    └── constants/                  # Themes, format & quality definitions
```
