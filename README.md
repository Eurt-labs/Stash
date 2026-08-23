<div align="center">

![Stash Banner](app-resources/hero.svg?raw=true&v=2.0.3)

# Stash Media Downloader

**A fast, lightweight media downloader for downloading, transcoding, and tagging audio and video from YouTube and YouTube Music.**

Built with Electron, Vite, React 18, TypeScript, and Hardware-Accelerated Canvas & SVG Shaders.

[![GitHub Release](https://img.shields.io/github/v/release/Eurt-labs/Stash?style=flat-square&color=6366f1)](https://github.com/Eurt-labs/Stash/releases)
[![License](https://img.shields.io/badge/license-MIT-emerald?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android-3b82f6?style=flat-square)](#prerequisites--dependencies)

</div>

---

## ⚡ What is Stash?

Stash is an open-source media downloader that lets you pull tracks, full albums, playlists, or individual videos from YouTube and YouTube Music directly to your local storage. It automatically handles format transcoding (up to 320kbps MP3, AAC, FLAC, OPUS, WAV, MP4 video up to 4K Ultra HD) and embeds high-resolution cover artwork and metadata tags into your files.

Stash is available as both a **Desktop Application** (Electron + React) and a **Native Android Application** (Kotlin + Jetpack Compose). Both versions share a sleek design language, reliable extraction engines, and cross-platform quality-of-life features like automatic playlist folder organization.

---

## 🔄 How It Works

Stash processes downloads through a deterministic **5-phase sequential pipeline** to prevent system lockups during large playlist conversions:

<div align="center">

![Stash Pipeline](app-resources/pipeline.svg?raw=true&v=2.0.2)

</div>

1. **Input & Parse**: Accepts direct URLs (video, playlist, album, artist) or plain-text artist searches.
2. **Fetch Metadata**: Scrapes track titles, artists, album names, durations, and high-res thumbnail URLs via `yt-dlp` with automatic fallback search.
3. **Download Stream**: Downloads the best available raw audio or video stream into a temporary working cache with live speed and ETA reporting (4K/2K/1080p/720p/Audio).
4. **Transcode (FFmpeg)**: Converts the raw stream into your chosen target format and bitrate preset (`4K`, `2K`, `High 320k`, `Mid 192k`, `Low 128k`, or bit-perfect `Lossless`).
5. **Tag & Move**: Embeds ID3v2/RIFF/Vorbis metadata and Baseline JPEG album artwork, then moves the final file to your selected output directory and purges the temp cache.

---

## 🛠️ Prerequisites & Dependencies

Stash relies on two core command-line tools under the hood:
- **`yt-dlp`** — Stream extraction and metadata parser (Auto-managed nightly VisionOS engine).
- **`ffmpeg` / `ffprobe`** — Audio and video transcoding engine.

### Automatic Bootstrap
Stash includes an automatic on-demand binary bootstrap engine. If `yt-dlp` is not found on your system PATH, Stash automatically downloads the latest nightly executable directly into `~/.stash/bin/` on your first search or download with zero manual setup required!

### Manual Installation by Platform (Optional)

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

## 🚀 Building & Running the Application

### 1. Clone the repository
```bash
git clone https://github.com/Eurt-labs/Stash.git
cd Stash
```

### 2. Install Node dependencies
```bash
npm install
```

### 3. Run in development mode (Live Reload)
```bash
npm run dev
```

### 4. Compile TypeScript & Vite production bundle
```bash
npm run build
```

### 5. Package Windows Standalone Installer (`.exe`)
To package the clean, lightweight standalone Windows installer (**~85 MB**):
```bash
npm run package:win
```
The compiled installer will be generated in the **`release/`** folder:
- **`release/Stash Setup 2.0.0.exe`** — Standalone NSIS installer with desktop and start menu shortcuts.

### 6. Alternative Build Targets
- **Build Portable `.exe` (No installation required)**:
  ```bash
  npm run build
  npx electron-builder --win portable
  ```
- **Build Unpacked Directory (Fast preview without installer)**:
  ```bash
  npm run build
  npx electron-builder --win --dir
  ```

---

## ✨ Features

- **Broad Platform & URL Support**: Works with YouTube videos, Shorts, playlists, YouTube Music tracks, albums, artist channels, and plain-text search queries.
- **Transcoding Options**:
  - **Formats**: MP3, AAC, FLAC (lossless), OPUS, WAV, MP4 video.
  - **Video Quality Presets**: 4K Ultra HD (2160p), 2K Quad HD (1440p), 1080p Full HD, 720p HD, 360p Compact.
  - **Audio Quality Presets**: High (320kbps), Mid (192kbps), Low (128kbps), Lossless (Bit-Perfect / Maximum).
- **Automated Metadata & Cover Art**: Embeds ID3v2, RIFF INFO, and Vorbis comments with normalized 600x600 Baseline JPEG artwork across all formats.
- **Full Playlist Extraction & Auto-Fallback**: Instantly extracts 100% of playlist items and uses automatic search fallback for hidden or region-restricted songs.
- **Cross-Platform Playlist Organization**: Downloads of multiple tracks (like playlists or artist pages) are automatically grouped into cleanly named subfolders within your main download directory.
- **Micro-Animations & Liquid Glass UI**: Spring-loaded chevrons, CSS Grid playlist accordions, staggered cascading track cards, and 120Hz-optimized Liquid Glass UI on Android.
- **13 Built-in Color & Artist Signature Themes**: 7 Core Palettes + 6 Artist Styles (The Weeknd, Taylor Swift, Billie Eilish, Daft Punk, Travis Scott, Lana Del Rey) with slow-breathing animated backdrops.
- **In-App Self-Healing Update Engine**: 1-click update tool in Settings to automatically pull the newest upstream nightly patches without reinstalling the app.

## 🛡️ Android Security Architecture (Anti-Ban)

Stash Android implements state-of-the-art security patterns to ensure your Google Account is **100% immune** to scraping bans from YouTube:

1. **Silent Guest Sessions**: `yt-dlp` is strictly air-gapped from your Google ID. On every app launch, Stash silently spins up an invisible background browser to fetch brand new, organic anonymous "Guest" cookies from YouTube. `yt-dlp` only ever sees these randomized guest tokens.
2. **Native Library Fetcher**: When you log in to sync your private "Watch History" or "Liked Videos", Stash fetches this data natively in Kotlin using your real cookies. However, when you tap download, Stash passes *only the public video URL* to `yt-dlp`, which downloads it using the anonymous Guest token. Your account data never touches the downloader!

---

## 📁 Project Structure

```text
Stash/
├── app-resources/          # Application logo and animated vector SVG assets
├── src/
│   ├── main/               # Electron main process
│   │   ├── services/       # Orchestrator, LinkParser, DownloadEngine, ConversionEngine, MetadataTagger, DependencyResolver
│   │   ├── main.ts         # Main process window and IPC handlers
│   │   └── preload.ts      # Context bridge exposing safe IPC API
│   ├── renderer/           # React frontend UI
│   │   ├── src/
│   │   │   ├── components/ # Header, SettingsBar, LinkInputBar, BatchItem, TrackCard, SettingsModal
│   │   │   ├── index.css   # Clean handcrafted dark design system & micro-animations
│   │   │   └── App.tsx     # Root application component
│   │   └── index.html      # Application HTML entry point
│   └── shared/             # Shared TypeScript types and interfaces
├── electron-builder.json   # Windows NSIS installer packaging configuration
├── vite.config.ts          # Vite + Electron plugin configuration
└── package.json
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
