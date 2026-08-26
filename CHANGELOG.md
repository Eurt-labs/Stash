# Changelog

All notable changes to **Stash Media Downloader** are documented in this file.

For detailed historical root-cause analyses and code diffs across all 24 major iterations, refer to [`docs/CHANGELOG_AND_FIXES.md`](docs/CHANGELOG_AND_FIXES.md).

---

## [2.1.0] - 2026-08-26


### Fixes & Enhancements
- **Download Engine (PC)**: Restored `youtube:player_client=ios,android,tv` extractor arguments to bypass YouTube web client throttling, unlocking 1080p, 2K, and 4K video downloads.
- **Metadata (PC)**: Forced `yt-dlp` to natively embed ID3 tags and high-resolution JPEG thumbnails directly during the download phase (`--embed-metadata`, `--embed-thumbnail`).
- **File Organization (PC)**: Implemented dynamic subfolder generation; downloading a playlist or artist library now automatically routes tracks into a dedicated subfolder within the chosen download directory.

### Highlights
- **Cross-Platform Parity**: Unified versioning (v2.1.0) across the Electron PC app and Native Android app.
- **UI Architecture**: Resolved critical JSX Fragment parsing errors in `SettingsBar.tsx` for stable rendering.
- **Build Integrity**: Fixed Electron taskbar icon inheritance by hardlinking `app-resources/icon.png` to the `BrowserWindow` lifecycle.
- **Documentation**: Overhauled `README.md` to map the new Domain-Driven Design (DDD) architecture and updated feature capabilities for Native ID3 tagging and SAF storage hooks.


## [2.0.0] - 2026-08-19

### Highlights
- **Architecture**: Complete transition from legacy Kotlin codebase to modular Electron, React 18, and TypeScript architecture.
- **Packaging**: Unbundled ~200MB binaries from installer, shrinking package footprint to **85.9 MB** with on-demand background bootstrapping.
- **Video Formats**: Added 4K Ultra-HD (2160p) and 2K Quad-HD (1440p) presets with smart UI visibility toggles.
- **Audiophile Audio**: Bit-perfect lossless FLAC and WAV transcoding with RIFF INFO and Vorbis ID3 chunk embedding.
- **Anti-Throttling**: Switched to modern VisionOS extraction engine, resolving YouTube SABR and HTTP 403 Forbidden errors.
- **Resilience**: Added automated alternative search fallback (`ytsearch1:`) for hidden or region-blocked playlist tracks.
- **UI & Animations**: Monochromatic vector SVG identity, spring-loaded playlist accordions, and chromatic theme bloom transitions.
- **Self-Healing**: Added 1-click in-app `yt-dlp` nightly updater directly inside Settings.
