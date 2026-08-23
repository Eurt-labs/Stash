# 🧠 Stash Application Core Concepts

This document serves as a high-level glossary and technical explanation of the core concepts, workarounds, and architectures used in building Stash Media Downloader across both Desktop (Electron/React) and Android (Kotlin/Jetpack Compose).

## 1. The Extractors & Client Cascades (yt-dlp)
YouTube employs extremely aggressive anti-bot mechanisms (like **SABR** - *Secure Audio-Video Bounded Routing* and **PO Tokens** - *Proof of Origin*). When it detects a scraper, it issues `Error 152` or a "Sign in to confirm you're not a bot" block.
To bypass this without user login, Stash relies on **Client Cascades**. 
* **The `tv` Client**: API endpoints for Smart TVs are heavily whitelisted by YouTube because TVs lack browsers to solve captchas. By forcing yt-dlp to request data as a TV (`youtube:player_client=tv`), we punch through IP bans.
* **The Audio Fallback (`ba/18/b`)**: The `tv` client doesn't provide pure audio streams (`ba`). We configured the downloader to fall back to format `18` (360p video + audio) when banned, and use FFmpeg to dynamically rip the audio track out of the video container.

## 2. Platform Differences (Why Android Fails When Desktop Succeeds)
Scraping from an Android device is vastly more difficult than a Desktop PC due to:
* **CGNAT IP Sharing**: Cellular networks share single IP addresses across thousands of phones. If one person scrapes, the entire IP gets banned. Home Desktop IPs are usually residential and highly trusted.
* **JS Challenge Solving**: On Desktop, `yt-dlp` has access to NodeJS/Browsers to silently solve JavaScript Proof-of-Work captchas. Android runs `yt-dlp` inside a Python sandbox (Chaquopy) with no browser to solve these challenges.
* **TLS Fingerprinting (JA3)**: YouTube inspects the cryptographic handshake of incoming requests. A Desktop request looks like Chrome. An Android python request looks suspiciously like a bot.

## 3. Liquid Glass Architecture (UI/UX)
Stash's signature design language across both platforms.
* **Desktop**: Built using HTML Canvas shaders, Framer Motion, and CSS `backdrop-filter: blur()`. It uses absolute positioning and Z-indexes to create floating, hardware-accelerated translucent panels.
* **Android**: Built using Jetpack Compose's `Modifier.blur()` and `RenderEffect`. We use `graphicsLayer` to achieve dynamic translucency on mobile GPUs without draining the battery.

## 4. Dual-Engine Processing
Stash decouples downloading from transcoding to ensure the UI remains perfectly fluid at 60/120Hz.
* **DownloadEngine**: Uses `yt-dlp` purely for retrieving the raw bytes over the network.
* **ConversionEngine / FFmpeg**: Runs in the background (or spawned child process) to handle heavy CPU operations (like ripping AAC to FLAC/MP3) without blocking the main UI thread.

## 5. Storage Access Framework (SAF)
On Android 11+, apps cannot freely write to external storage. Stash implements scoped storage via the `StorageManager`, prompting the user for directory permissions and securely caching download locations via `MediaStore` and `DocumentFile` APIs.
