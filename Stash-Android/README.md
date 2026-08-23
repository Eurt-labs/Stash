# 📱 Stash for Android (Kotlin + Jetpack Compose)

High-performance native Android media downloader powered by **Jetpack Compose (Material 3)**, **`youtubedl-android`** (NDK precompiled `yt-dlp`), and native **FFmpeg**.

---

## 🏗️ Architecture Overview

```text
Stash-Android/
├── settings.gradle.kts                         # JitPack & MavenCentral repository configuration
├── build.gradle.kts                            # Root build configuration
├── gradle.properties                           # JVM args & AndroidX configurations
├── README.md                                   # Getting started & build guide
└── app/
    ├── build.gradle.kts                        # Compose BOM, youtubedl-android, Coil, Coroutines
    ├── proguard-rules.pro                      # JNI & reflection rules for yt-dlp NDK
    └── src/main/
        ├── AndroidManifest.xml                 # Foreground Service, Post Notifications, SEND Intent
        ├── res/
        │   ├── values/strings.xml              # String resources
        │   ├── values/themes.xml               # Window & Status bar themes
        │   └── xml/data_extraction_rules.xml   # Backup configuration
        └── java/com/eurtlabs/stash/
            ├── StashApplication.kt             # Native YoutubeDL / FFmpeg engine bootstrap
            ├── MainActivity.kt                 # Single Activity with incoming link share handler
            ├── data/
            │   ├── model/Models.kt             # TrackInfo, DownloadItem, DownloadBatch, Enums
            │   ├── parser/LinkParser.kt        # URL regex parser for YouTube, Music & Search
            │   ├── downloader/YoutubeDLManager.kt # youtubedl-android process execution
            │   └── transcoder/MediaTagger.kt   # ID3 tagging & MediaStore notification
            ├── service/
            │   └── DownloadForegroundService.kt# Android Foreground Service for persistent downloads
            ├── viewmodel/
            │   └── DownloadViewModel.kt        # StateFlow UI state management
            └── ui/
                ├── theme/
                │   ├── Color.kt                # Stash curated palette (Weeknd, OLED, Sunset, etc.)
                │   ├── Type.kt                 # Typography definitions
                │   └── Theme.kt                # Jetpack Compose Theme wrapper
                └── components/
                    ├── TopBar.kt               # Header with branding and settings trigger
                    ├── SearchInputBar.kt       # Dynamic input bar with paste & analyze trigger
                    ├── TrackCardItem.kt        # Coil image loader, live progress indicator, status chips
                    ├── BatchQueueList.kt       # LazyColumn queue container
                    └── SettingsBottomSheet.kt  # Material3 bottom sheet for theme, format, and bitrate
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

### 4. Bypassing CAPTCHAs & Age Restrictions
If you ever run into CAPTCHAs or age-restriction blocks, don't worry! Just pop open the `SettingsScreen`, click the `Login / Sync Browser Cookies` button, and it opens a hidden WebView. Once you log into YouTube normally, Stash securely pulls your cookies, strips out any sensitive Google ID tokens, and feeds them to the downloader so you never have to deal with manual terminal commands.

### 5. Blazing Fast 4K Downloads
We recently ditched the restricted Android/TV APIs and moved back to the default web clients. What does that mean for you? Blazing fast metadata fetching and full support for downloading raw 1080p, 1440p, and 4K streams! No more getting capped at 720p!
