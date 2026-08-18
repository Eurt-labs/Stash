<div align="center">

![Stash Banner](app-resources/hero.svg?raw=true&v=2.0.1)

# Stash Media Downloader

**A fast, lightweight desktop application for downloading, transcoding, and tagging audio and video from YouTube and YouTube Music.**

Built with Electron, Vite, React 18, TypeScript, and Hardware-Accelerated Canvas &amp; SVG Shaders.

[![GitHub Release](https://img.shields.io/github/v/release/Eurt-labs/Stash?style=flat-square&color=6366f1)](https://github.com/Eurt-labs/Stash/releases)
[![License](https://img.shields.io/badge/license-MIT-emerald?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-3b82f6?style=flat-square)](#prerequisites--dependencies)

</div>

---

## ⚡ What is Stash?

Stash is an open-source desktop tool that lets you pull tracks, full albums, playlists, or individual videos from YouTube and YouTube Music directly to your local drive. It automatically handles format transcoding (up to 320kbps MP3, AAC, FLAC, OPUS, WAV, MP4) and embeds high-resolution cover artwork and ID3 tags into your files.

---

## 🔄 How It Works

Stash processes downloads through a deterministic **5-phase sequential pipeline** to prevent system lockups during large playlist conversions:

<div align="center">

![Stash Pipeline](app-resources/pipeline.svg?raw=true&v=2.0.1)

</div>

1. **Input & Parse**: Accepts direct URLs (video, playlist, album, artist) or plain-text artist searches.
2. **Fetch Metadata**: Scrapes track titles, artists, album names, durations, and high-res thumbnail URLs via `yt-dlp`.
3. **Download Stream**: Downloads the best available raw audio or video stream into a temporary working cache with live speed and ETA reporting.
4. **Transcode (FFmpeg)**: Converts the raw stream into your chosen target format and bitrate preset (`High 320k`, `Mid 192k`, `Low 128k`).
5. **Tag & Move**: Embeds ID3v2.4 metadata and cover artwork using `node-id3`, then moves the final file to your selected output directory and purges the temp cache.

---

## 🛠️ Prerequisites & Dependencies

Stash relies on two core command-line tools under the hood:
- **`yt-dlp`** — Stream extraction and metadata parser.
- **`ffmpeg` / `ffprobe`** — Audio and video transcoding engine.

### Automatic Windows Setup
If you are running on Windows, Stash includes a helper script that downloads the latest official binaries directly into `app-resources/windows/`:
```powershell
powershell -ExecutionPolicy Bypass -File .\download_dependencies.ps1
```

### Manual Installation by Platform

#### Windows
```powershell
# Using winget (Recommended)
winget install yt-dlp.yt-dlp Gyan.FFmpeg

# Or using Chocolatey
choco install yt-dlp ffmpeg
```

#### macOS
```bash
# Using Homebrew
brew install yt-dlp ffmpeg
```

#### Linux (Debian / Ubuntu / Arch / Fedora)
```bash
# Ubuntu / Debian
sudo apt update && sudo apt install ffmpeg yt-dlp

# Arch Linux
sudo pacman -S ffmpeg yt-dlp

# Fedora
sudo dnf install ffmpeg yt-dlp
```

---

## 🚀 Getting Started Locally

### 1. Clone the repository
```bash
git clone https://github.com/Eurt-labs/Stash.git
cd Stash
```

### 2. Install Node dependencies
```bash
npm install
```

### 3. Run in development mode
```bash
npm run dev
```

### 4. Build for production (Windows `.exe` / `.msi`)
```bash
npm run package:win
```
The compiled installer will be generated in the `release/` folder.

---

## ✨ Features

- **Broad Platform Support**: Works with YouTube videos, Shorts, playlists, YouTube Music tracks, albums, artist channels, and plain-text search queries.
- **Transcoding Options**:
  - **Formats**: Auto-Detect, MP3, AAC, FLAC (lossless), OPUS, WAV, MP4 video.
  - **Quality Presets**: High (320kbps / 1080p), Mid (192kbps / 720p), Low (128kbps / 360p).
- **Automated Metadata & Cover Art**: Embeds ID3v2.4 tags (Title, Artist, Album, Year, Genre) and full-quality album artwork.
- **7 Built-in Color Themes**: Linear Indigo, Emerald Mint, Sunset Rose, Ocean Sapphire, Amber Gold, Crimson Red, and OLED Monochrome with instant live switching.
- **In-App Tool & Release Checker**: Checks for new `yt-dlp` binary updates and alerts you if a newer version of Stash is published on GitHub.
- **Fluid Desktop UI**: Ambient animated SVG background paths powered by Framer Motion, with clean keyboard shortcuts and responsive scaling.

---

## 📁 Project Structure

```text
Stash/
├── app-resources/          # Application logo, animated SVGs, and bundled binaries
├── src/
│   ├── main/               # Electron main process
│   │   ├── services/       # Orchestrator, LinkParser, DownloadEngine, ConversionEngine, MetadataTagger
│   │   ├── main.ts         # Main process window and IPC handlers
│   │   └── preload.ts      # Context bridge exposing safe IPC API
│   ├── renderer/           # React frontend UI
│   │   ├── src/
│   │   │   ├── components/ # Header, SettingsBar, LinkInputBar, BatchItem, TrackCard, SettingsModal
│   │   │   ├── index.css   # Clean handcrafted dark design system
│   │   │   └── App.tsx     # Root application component
│   └── shared/             # Shared TypeScript types and interfaces
├── electron-builder.json   # Windows NSIS / MSI installer packaging configuration
├── vite.config.ts          # Vite + Electron plugin configuration
└── package.json
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
