# 📱 Stash Android — Changelog & Technical Fixes Archive

---

### 📌 [ANDROID-FEAT-003] Interactive Search Engine & First-Launch Storage Prompt
- **Date**: 2026-08-20
- **Files Modified/Created**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SearchScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/StorageSelectionDialog.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/storage/StorageManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
- **Severity**: Major Feature & UX Enhancement

#### 1. User Requests Addressed
- Implement interactive search for YouTube Music, Artists, and YouTube Videos with a clean minimalist design.
- Prompt the user to select their preferred storage location when starting the application for the first time.

#### 2. Technical Implementation
- **Interactive Search Engine**:
  - Added `searchMedia(query, filter)` in `YoutubeDLManager.kt` leveraging `--flat-playlist --dump-json` to extract titles, artists, thumbnails, and durations in <1.5s.
  - Interactive search tabs in `SearchScreen.kt`: `All`, `Songs & Music`, `Artists`, and `Videos`.
  - Rich result cards with instant single-tap `[ Get 📥 ]` buttons that immediately enqueue downloads in the user's chosen format.
- **First-Launch Storage Onboarding**:
  - Implemented `StorageManager.kt` to persist folder preferences and first-launch state.
  - Designed `StorageSelectionDialog.kt` modal prompting users between **Default Music Folder** (`Music/Stash`) and **Custom Folder** (via Android's native `OpenDocumentTree()` SAF picker).
  - Added **"Change Folder"** capability inside `SettingsScreen.kt`.

---

### 📌 [ANDROID-FIX-010] Synchronous Engine Startup, Stream Selection & Live Status/Retry UI
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/StashApplication.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/parser/LinkParser.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/TrackCardItem.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BatchQueueList.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
- **Severity**: Critical (Engine Initialization Race Condition & Live UI Feedback)

#### 1. Problems Addressed
- **Startup Race Condition**: `YoutubeDL.getInstance().init(this)` was wrapped in a background coroutine in `StashApplication.kt`, causing `YoutubeDLException: not initialized` when users immediately pasted a link after app launch.
- **Heavy Stream Download**: Audio downloads were downloading the entire video stream before running ffmpeg extraction.
- **Search Query Latency**: `LinkParser.kt` was querying `ytsearch150:` causing long timeouts on mobile connections.
- **Missing Loading/Analyzing Feedback**: No visual cues or progress indicators when analyzing links or when downloads fail.

#### 2. Technical Solution & Implementation
- **Synchronous Engine Init in `StashApplication.kt`**: `YoutubeDL.getInstance().init(this)` is invoked directly in `Application.onCreate()`.
- **Audio Stream Direct Extraction**: Added `-f ba/b` and `--audio-quality 0` to immediately stream pure audio without full video overhead.
- **Instant Search Resolution**: Changed query format to `ytsearch1:$query` for instant top match scraping.
- **Animated Fetching Banner**: Added live animated status banner in `MainActivity.kt` when `isFetching == true`.
- **Card Error Details & 1-Tap Retry**: Track cards now display real-time speed/ETA, error descriptions, and an interactive **Retry ↻** button.

---

### 📌 [ANDROID-FIX-009] Type.kt Color Fallbacks & Metadata Extraction Normalization
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/theme/Type.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/theme/Color.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
- **Severity**: Compilation & Robustness Fix

#### 1. Problem Description & Symptoms
- `:app:compileDebugKotlin` failed with unresolved reference:
  `Type.kt: Unresolved reference: TextPrimary`
  `Type.kt: Unresolved reference: TextSecondary`

#### 2. Technical Root Cause Analysis
- `Type.kt` referenced top-level `TextPrimary` / `TextSecondary` which were encapsulated inside `ThemePalette` during the theme engine refactoring.

#### 3. Exact Solution & Code Implementation
- Provided explicit fallback colors in `Type.kt`.
- Exported global `val TextPrimary` and `val TextSecondary` in `Color.kt` for backwards compatibility.
- Cleaned up metadata request execution in `YoutubeDLManager.kt`.

---

### 📌 [ANDROID-FIX-008] Robust Download Execution Algorithm & Dynamic Music/Video Matrix
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/model/Models.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/SettingsBottomSheet.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: Critical (Core Download Engine & Dynamic UX)

#### 1. Problems Addressed
- Downloads were stalling or failing because `getInfo` had redundant `--dump-json` options that conflicted with the native wrapper's internal arguments.
- Android public storage path lacked guaranteed POSIX write access on Android 10-16.
- Removed ambiguous `AUTO` format option.
- Formats and qualities were static and not dynamically responding to whether the user wants Music/Audio or Video.

#### 2. Technical Solution & Implementation
- **Robust Download Engine in `YoutubeDLManager.kt`**:
  - Uses `context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)` for 100% reliable, zero-permission disk I/O on Android 10-16.
  - Simplified `getInfo(url)` and added fallback metadata extraction.
  - Implemented speed/ETA regex parser and file extension fallback detector.
- **Dynamic Music vs Video Mode**:
  - When **Music & Audio** is active:
    - Formats: `MP3`, `AAC`, `FLAC`, `OPUS`, `WAV`
    - Bitrates: `320 kbps (Lossless / Ultra)`, `256 kbps (High Quality)`, `192 kbps (Medium Quality)`, `128 kbps (Standard Quality)`
  - When **Video** is active:
    - Formats: `MP4`, `MKV`, `WEBM`
    - Resolutions: `4K Ultra HD (2160p)`, `2K QHD (1440p)`, `Full HD (1080p)`, `HD (720p)`, `SD (480p)`
- **Dynamic Settings & BottomSheet**:
  - Live animated switching between audio/video format and quality lists.

---

### 📌 [ANDROID-FEAT-002] WhatsApp-Style Bottom Navigation & Monochromatic Design Overhaul
- **Date**: 2026-08-20
- **Files Modified/Created**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/TopBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/StashBrandIcon.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SearchScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/LibraryScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/theme/Color.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/theme/Theme.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/model/Models.kt`
- **Severity**: Major Feature & Architectural UI Transformation

#### 1. User Feedback Addressed
- Top-left app icon showed generic pink "S" letter instead of the real Stash branding.
- Settings format chips (e.g. `OPUS`) were wrapping awkwardly into multi-line letters.
- Settings UI lacked proper Android 16 gesture bar insets padding.
- AI/cyberpunk styles replaced with clean, monochromatic, and architectural themes.
- Replaced flat monolithic layout with WhatsApp-style bottom navigation.

#### 2. Technical Implementation
- **WhatsApp-Style Bottom Navigation**: Integrated pill-indicator navigation bar for `Queue`, `Search`, `Library`, and `Settings` with spring animations and active badge counters.
- **Monochromatic Theme Engine**: Added 7 architectural palettes (`Obsidian OLED`, `Titanium Slate`, `Graphite Carbon`, `Nord Frost`, `Sage Minimal`, `Warm Espresso`, `Midnight Navy`).
- **Vector Brand Icon**: Rendered vector music note + download arrow on glassmorphic badge in `StashBrandIcon.kt`.
- **Responsive Layout**: Replaced rigid rows with `LazyRow` format chips and proper `navigationBarsPadding()`.
- **Integrated FileProvider**: Enabled instant playback in external players and one-tap Android share sheet integration.

---

### 📌 [ANDROID-FIX-007] Function Signature Syntax & VideoInfo Property Resolution
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description & Symptoms
- `:app:compileDebugKotlin` threw 2 errors in `YoutubeDLManager.kt`:
  - `Expecting '->' to specify return type of a function type :56`
  - `Unresolved reference: artist :110`

#### 2. Technical Root Cause Analysis
- Kotlin higher-order function types use `->` syntax (`(Float, String, String) -> Unit`) instead of TypeScript's `=>`.
- `com.yausername.youtubedl_android.mapper.VideoInfo` exposes channel/uploader via `info.uploader`.

#### 3. Exact Solution & Code Implementation
- Changed callback parameter to `onProgress: (progress: Float, speed: String, eta: String) -> Unit`.
- Updated metadata mapping to `val artist = info.uploader ?: "Unknown Artist"`.

---

### 📌 [ANDROID-FIX-006] Native youtubedl-android Engine Initialization Cleanups
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/StashApplication.kt`, `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`, `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/transcoder/MediaTagger.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description & Symptoms
- `:app:compileDebugKotlin` failed with unresolved reference:
  `StashApplication.kt: Unresolved reference: ffmpeg`
  `StashApplication.kt: Unresolved reference: FFmpeg`

#### 2. Technical Root Cause Analysis
- `youtubedl-android` automatically wraps and initializes FFmpeg internally inside `YoutubeDL.getInstance().init(context)` without requiring or exposing a separate public `FFmpeg` class.

#### 3. Exact Solution & Code Implementation
- Removed redundant `FFmpeg` import and initialization from `StashApplication.kt`.
- Cleaned up unused imports across `YoutubeDLManager.kt` and `MediaTagger.kt`.

---

### 📌 [ANDROID-FIX-005] AAPT2 Resource Linking Fix for Vector Mipmap Launcher Icons
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/res/drawable/ic_launcher_background.xml`, `Stash-Android/app/src/main/res/drawable/ic_launcher_foreground.xml`, `Stash-Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `Stash-Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- **Severity**: Critical (AAPT2 Resource Linking)

#### 1. Problem Description & Symptoms
- Build failed during `:app:processDebugResources`:
  ```text
  Aapt2Exception: Android resource linking failed
  ERROR: AndroidManifest.xml:16:5: AAPT: error: resource mipmap/ic_launcher not found
  ```

#### 2. Technical Root Cause Analysis
- `AndroidManifest.xml` referenced `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`, but the vector launcher drawables were missing in the resource tree.

#### 3. Exact Solution & Code Implementation
- Converted Stash's vector branding (`icon.svg`) into:
  - `res/drawable/ic_launcher_background.xml`: Deep dark background (`#0b0d13`).
  - `res/drawable/ic_launcher_foreground.xml`: Vector music note + download arrow matching Stash logo.
  - `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: Adaptive icons for modern Android 8.0 - 16 devices.

---

### 📌 [ANDROID-FIX-004] youtubedl-android Maven Central Coordinates Migration
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/build.gradle.kts`, `Stash-Android/app/src/main/AndroidManifest.xml`, `Stash-Android/app/proguard-rules.pro`
- **Severity**: Critical (Dependency Resolution & AAR Packaging)

#### 1. Problem Description & Symptoms
- Build failed during `:app:checkDebugAarMetadata`:
  ```text
  Could not find com.github.yausername.youtubedl-android:library:0.17.0
  Could not find com.github.yausername.youtubedl-android:ffmpeg:0.17.0
  Searched in: dl.google.com, repo.maven.apache.org, jitpack.io
  ```

#### 2. Technical Root Cause Analysis
- The legacy `yausername/youtubedl-android` group ID on JitPack is deprecated and unmaintained. The active, production-ready build for Android is published directly to Maven Central under `io.github.junkfood02.youtubedl-android`.

#### 3. Exact Solution & Code Implementation
- Migrated dependencies in `app/build.gradle.kts` to:
  ```kotlin
  implementation("io.github.junkfood02.youtubedl-android:library:0.17.4")
  implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.4")
  ```
- Added `android:extractNativeLibs="true"` to `AndroidManifest.xml` as required for native NDK binary extraction.
- Updated Proguard rules for `io.github.junkfood02.youtubedl_android.**`.

---

### 📌 [ANDROID-FIX-003] Android 15 & 16 (API 35/36) Edge-to-Edge & SDK Optimizations
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/build.gradle.kts`, `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: Platform Modernization & UX Enhancement

#### 1. Problem Description & Target
- The user is testing on Android 16 (API 36 preview).
- Android 15 and 16 enforce mandatory Edge-to-Edge window rendering and require runtime notification permission handling.

#### 2. Technical Solution & Implementation
- Upgraded `compileSdk` and `targetSdk` to **35** (with forward runtime compatibility for Android 16 API 36).
- Added `enableEdgeToEdge()` in `MainActivity.kt` with `navigationBarsPadding()` and `statusBarsPadding()` so the UI seamlessly extends underneath translucent gesture bars without letterboxing.
- Implemented automatic runtime `POST_NOTIFICATIONS` permission prompt on Android 13+ (API 33-36).

---

### 📌 [ANDROID-FIX-002] Incompatible Gradle JVM Version & Wrapper Upgrade
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/gradle/wrapper/gradle-wrapper.properties`, `Stash-Android/gradle.properties`, `Stash-Android/.idea/gradle.xml`
- **Severity**: Build Environment Setup

#### 1. Problem Description & Symptoms
- Android Studio showed `Incompatible Gradle JVM version` error when syncing:
  `The project's Gradle version 8.9 is incompatible with the Gradle JVM version 25 currently selected to run Gradle build.`

#### 2. Technical Root Cause Analysis
- The latest Android Studio Canary build bundles OpenJDK 25.0.2 preview, which is too new for Gradle 8.9 (Gradle 8.9 officially supports Java 17 and Java 21 LTS).

#### 3. Exact Solution & Code Implementation
- Upgraded Gradle distribution wrapper to `gradle-8.9-bin.zip` in `gradle-wrapper.properties`.
- Added `org.gradle.java.home=C:\\Users\\Dhruv Saraswat\\.jdks\\jbr-21.0.11` to `gradle.properties` to automatically lock builds to Java 21 LTS.
- Configured `.idea/gradle.xml` to `jbr-21`.

---

### 📌 [ANDROID-FIX-001] Root & App build.gradle.kts Plugin Resolution and Version Catalog Errors
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/build.gradle.kts`, `Stash-Android/app/build.gradle.kts`, `Stash-Android/gradle/wrapper/gradle-wrapper.properties`
- **Severity**: Critical (Build Configuration Resolution)

#### 1. Problem Description & Symptoms
- Opening `Stash-Android` in Android Studio or running Gradle tasks resulted in 13 script compilation errors in `build.gradle.kts`:
  ```text
  Unresolved reference 'libs'
  None of the following candidates is applicable: fun alias(...)
  Unresolved reference 'android'
  Unresolved reference 'compose'
  ```

#### 2. Technical Root Cause Analysis
- `build.gradle.kts` attempted to resolve plugins via `alias(libs.plugins.android.application)` syntax without a configured Gradle Version Catalog (`libs.versions.toml`).
- `app/build.gradle.kts` referenced `org.jetbrains.kotlin.plugin.compose` which is only supported on Kotlin 2.0+, whereas the project uses Kotlin 1.9.23.

#### 3. Exact Solution & Code Implementation
- Replaced catalog alias references in `Stash-Android/build.gradle.kts` with direct, self-contained plugin IDs and explicit AGP/Kotlin versions:
  ```kotlin
  plugins {
      id("com.android.application") version "8.3.2" apply false
      id("org.jetbrains.kotlin.android") version "1.9.23" apply false
  }
  ```
- Configured Jetpack Compose in `Stash-Android/app/build.gradle.kts` with `composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }`.
- Generated `gradle/wrapper/gradle-wrapper.properties` targeting Gradle `8.4`.

---

### 📌 [ANDROID-FEAT-001] Initial Native Android Architecture Scaffolding
- **Date**: 2026-08-20
- **Components Created**:
  - `data/downloader/YoutubeDLManager.kt`: Native NDK wrapper for `yt-dlp` and `ffmpeg`.
  - `data/parser/LinkParser.kt`: Universal URL regex and query fallback parser.
  - `service/DownloadForegroundService.kt`: Resilient foreground service with notification progress bars and partial WakeLock.
  - `viewmodel/DownloadViewModel.kt`: StateFlow queue management.
  - `ui/theme/`: Material 3 theme supporting all 13 Stash artist palettes (*The Weeknd*, *OLED*, *Sunset*, *Emerald*, etc.).
  - `ui/components/`: `TopBar`, `SearchInputBar`, `TrackCardItem`, `BatchQueueList`, `SettingsBottomSheet`.
  - `MainActivity.kt`: Single Activity container with Android Share Sheet (`ACTION_SEND`) receiver.
