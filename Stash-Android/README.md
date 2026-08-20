# 📱 Stash for Android (Kotlin + Jetpack Compose)

High-performance native Android media downloader powered by **Jetpack Compose (Material 3)**, **`youtubedl-android`** (NDK precompiled `yt-dlp`), and native **FFmpeg**.

---

## 🏗️ Architecture Overview

```text
Stash-Android/
├── app/
│   ├── build.gradle.kts                        # Gradle build config (Compose, youtubedl-android, Coil)
│   ├── proguard-rules.pro                      # JNI & NDK reflection rules
│   └── src/main/
│       ├── AndroidManifest.xml                 # Foreground Service, Post Notifications, SEND intent
│       ├── res/                                # XML resources & Material theme tokens
│       └── java/com/eurtlabs/stash/
│           ├── StashApplication.kt             # Native YoutubeDL / FFmpeg engine bootstrap
│           ├── MainActivity.kt                 # Single Activity with incoming link share handler
│           ├── data/
│           │   ├── model/Models.kt             # TrackInfo, DownloadItem, DownloadBatch, Enums
│           │   ├── parser/LinkParser.kt        # URL regex parser for YouTube, Music & Search
│           │   ├── downloader/YoutubeDLManager.kt # youtubedl-android process execution
│           │   └── transcoder/MediaTagger.kt   # ID3 tagging & MediaStore notification
│           ├── service/
│           │   └── DownloadForegroundService.kt# Android Foreground Service for persistent downloads
│           ├── viewmodel/
│           │   └── DownloadViewModel.kt        # StateFlow UI state management
│           └── ui/
│               ├── theme/                      # Color palettes (Weeknd, OLED, Sunset, etc.)
│               └── components/                 # Jetpack Compose UI (TopBar, SearchBar, TrackCards)
```

---

## 🚀 Getting Started

### 1. Open in Android Studio
1. Launch **Android Studio (Hedgehog or newer)**.
2. Select **Open** and select the `Stash-Android` directory.
3. Allow Gradle to sync dependencies from Google, MavenCentral, and JitPack.

### 2. Run on Device / Emulator
* Connect an Android device (Android 8.0 / API 26+) with USB Debugging enabled.
* Click **Run ▶** in Android Studio.

### 3. Share Sheet Integration
You can share any YouTube link directly from the YouTube app or browser by clicking **Share ➔ Stash**; the app automatically opens, scrapes metadata, and enqueues the download.
