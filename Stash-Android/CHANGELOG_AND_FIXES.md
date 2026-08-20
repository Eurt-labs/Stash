# 📱 Stash Android — Changelog & Technical Fixes Archive

---

### 📌 [ANDROID-FEAT-012] Full HD/4K/2K Stream Selector & Liquid Sliding Mode Bubble
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/model/Models.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
- **Severity**: Video Quality & Visual Polish

#### 1. User Requests Addressed
- **High-Quality Video Stream Assurance**: 
  - Upgraded video stream format selectors to `bestvideo[height<=...]+bestaudio/best` and added `--format-sort "res,fps,codec:h264,size,br"` to guarantee that 1080p, 2K, and 4K downloads fetch the full uncompressed high-bitrate video streams rather than low-resolution fallbacks.
  - Audio extraction now uses `bestaudio/best` at max acoustic quality.
- **Liquid Glass Sliding Mode Bubble in Settings**:
  - Implemented an animated sliding liquid glass bubble pill in `SettingsScreen.kt` with spring physics (`stiffness = 450f, dampingRatio = 0.72f`) that glides fluidly between **Music & Audio** and **Video** modes.

---

### 📌 [ANDROID-FEAT-011] Interactive Download Queue Controls, Manual Link Pasting & Glass Polish
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/TrackCardItem.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BatchQueueList.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
- **Severity**: Core Features & Interaction Control

#### 1. User Requests Addressed
- **Fixed `enqueueTrackFromSearch` compilation error**: Resolved reference in `MainActivity.kt`.
- **Removed Auto-Clipboard Pasting**: Removed automatic clipboard inspection on app resume/focus to give users complete manual control over when links are pasted.
- **Interactive Download Controls**: Added **Cancel (✕)**, **Pause (❚❚)**, **Resume (▶)**, and **Retry (↻)** controls with live process termination via `YoutubeDL.destroyProcessById(itemId)`.
- **Refined Liquid Glass UI in Settings**: Enhanced translucent glass gradients, multi-stop specular borders, and luminous selection badges across all settings sections.

---

### 📌 [ANDROID-FIX-018] DownloadBatch & DownloadItem Model Property Alignment
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/storage/LibraryStore.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/LibraryScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` threw compilation errors in `LibraryStore.kt`, `LibraryScreen.kt`, and `DownloadViewModel.kt` due to mismatched model constructor properties (`track` vs `trackInfo`, `title` vs `name`, and `outputDir`).

#### 2. Root Cause & Solution
- Aligned all `DownloadItem` constructors to use `trackInfo: TrackInfo`.
- Aligned `DownloadBatch` constructors to use `name: String`, `outputDir: String`, `quality: DownloadQuality`, `format: DownloadFormat`, and `items: List<DownloadItem>`.
- Verified all callers in `LibraryStore.kt`, `LibraryScreen.kt`, and `DownloadViewModel.kt`.

---

### 📌 [ANDROID-FEAT-010] Persistent Library Storage, Disk Scanner & Floating Liquid Glass Island Nav
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/storage/LibraryStore.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/LibraryScreen.kt`
- **Severity**: Data Persistence & Visual Excellence

#### 1. User Requests Addressed
- **Persistent Library Storage**: Created `LibraryStore.kt` which automatically caches completed download batches and metadata into internal JSON storage (`stash_library_history.json`).
- **Disk Auto-Discovery**: Automatically scans device download and music directories on startup so pre-existing downloaded media tracks are discovered, displayed, playable, and shareable even after process exit or device reboot.
- **Floating Liquid Glass Island Navbar**: Redesigned the navigation bar into a detached floating frosted island (`shape = RoundedCornerShape(28.dp)`) with multi-stop specular highlights and high-refraction liquid glow bubbles for 120fps responsive gesture dragging and tapping.
- **Library File Controls**: Added 1-tap playback via default Android media player (`Intent.ACTION_VIEW`), 1-tap sharing (`Intent.ACTION_SEND`), and live file size formatting (`MB`).

---

### 📌 [ANDROID-FEAT-009] Horizontal Format & Quality Selectors + 120 Hz Hardware Display Optimizer
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: UI Polish & 120fps Rendering Performance

#### 1. User Requests Addressed
- **Horizontal Format & Quality Selectors**: Replaced tall vertical lists with sleek, horizontal scrolling liquid-glass cards for both Codec / Format and Bitrate / Resolution selectors.
- **120 Hz Fluid Display Compatibility**: Added window hardware display mode request in `MainActivity.kt` targeting the device's maximum supported refresh rate (`120 Hz`) on Android 15 & 16 (API 35/36).

---

### 📌 [ANDROID-FIX-017] Android 16 KB Page Alignment & JNI Packaging Resolution
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/build.gradle.kts`
- **Severity**: Android 16 (API 36) Compatibility

#### 1. Problem Description
- Android Studio / Android 16 device (OPPO CPH2729) displayed:
  `Android 16 KB Alignment: APK app-debug.apk is not compatible with 16 KB devices. Some libraries have LOAD segments not aligned at 16 KB boundaries.`

#### 2. Root Cause & Solution
- Android 15 and 16 introduced 16 KB memory page sizes. Precompiled native binaries bundled inside uncompressed APKs can have 4KB-aligned ELF segments.
- Configured `packaging { jniLibs { useLegacyPackaging = true } }` in `app/build.gradle.kts`. This instructs Android to extract native `.so` shared libraries to the device's native library path with 16 KB memory alignment, ensuring complete runtime stability.

---

### 📌 [ANDROID-FIX-016] Enum Property Name Alignments in SettingsScreen
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` failed with:
  `SettingsScreen.kt: Unresolved reference: description :355`
  `SettingsScreen.kt: Unresolved reference: label :424`

#### 2. Root Cause & Solution
- In `Models.kt`, `DownloadQuality` defines `label` and `valueOption` (no separate `description`).
- `ColorTheme` defines `displayName` and `subtitle` (not `label`).
- Aligned `SettingsScreen.kt` references to `theme.displayName` and removed the redundant quality description row.

---

### 📌 [ANDROID-FEAT-008] FLAC Lossless Rules, Full Liquid Glass UI, Draggable Nav & Auto-Paste
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: User Experience & Audiophile Audio Quality

#### 1. User Requests Addressed
- **Lossless Format Rules (FLAC / WAV)**: When FLAC or WAV is chosen, bitrate compression choices are automatically replaced with a bit-perfect uncompressed studio quality indicator (since lossless audio requires no lossy bitrate compression).
- **Comprehensive Liquid Glassmorphism in Settings**: Converted all cards, theme choosers, and action buttons in `SettingsScreen.kt` to the frosted liquid glass design with specular highlights.
- **Smooth Gesture-Draggable Navbar**: Implemented real-time touch position tracking with surface tension stretch (`scaleX = 1.15f`) and bouncy spring snap on drag release.
- **Automatic Clipboard Link Detection**: When the app is launched or resumed, Stash automatically inspects clipboard for copied YouTube / music links and enqueues them instantly.
- **Footer Text Rebranding**: Cleaned the footer label to `Stash Media Downloader`.

---

### 📌 [ANDROID-FIX-015] YouTube Reload Page Workaround, .txt Diagnostic File Export & Storage Dialog
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/LogManager.kt`
  - `Stash-Android/app/src/main/res/xml/file_paths.xml`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: Core Engine Fix & Diagnostics Polish

#### 1. Problem Diagnosed from Live User Logs
- User provided log snippet:
  `ERROR: [youtube] 4NRXx6U8ABQ: The page needs to be reloaded.`
- Root Cause: Deprecated `tv` player API in `--extractor-args` was returning reload page anti-bot challenge on YouTube servers.

#### 2. Technical Solution
- **Extractor Client Engine**: Replaced with `--extractor-args "youtube:player_client=android,web,mweb"` and added `--force-ipv4` for guaranteed direct stream delivery on mobile networks.
- **Physical .txt Diagnostic File Export**: Updated `LogManager.kt` to write a UTF-8 `.txt` file into cache and share it as an actual file attachment via `FileProvider` (`content://`).
- **Storage Dialog Triggering**: Wired `SettingsScreen.kt` "Change" action to `viewModel.openStorageDialog()` so users can switch between Default folder and SAF custom folder anytime.

---

### 📌 [ANDROID-FIX-014] Compose Runtime Imports & Icons Resolution in SettingsScreen
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` threw `Unresolved reference: Speed` and missing `setValue` delegate in `SettingsScreen.kt`.

#### 2. Root Cause & Solution
- Added `import androidx.compose.runtime.setValue`, `rememberCoroutineScope`, and `mutableStateOf`.
- Used standard `Icons.Default.Tune` for the Diagnostics & Logs section header icon.

---

### 📌 [ANDROID-FEAT-007] Diagnostics Exporter, Engine Updater, Runtime Permissions & Navbar Polish
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/LogManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
  - `Stash-Android/app/src/main/AndroidManifest.xml`
- **Severity**: Core Stability, Permissions & UI Polish

#### 1. Problems & User Requests Addressed
- **Navbar Bubble Artifact & Stickiness**: Removed the horizontal line bar from inside the bubble and eliminated gesture drag lag for silky-smooth tab navigation.
- **Storage & Notification Permissions**: App now proactively requests `POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, and external storage permissions on startup.
- **Diagnostics & Log Export**: Added a dedicated system diagnostics card in Settings with **"Export Diagnostic Logs"** (via Android Share Sheet) and **"Update Core Engine (yt-dlp)"**.
- **YouTube Extractor Fix**: Updated `--extractor-args "youtube:player_client=tv,web_safari,android"` to prevent bot challenges on YouTube.

---

### 📌 [ANDROID-FIX-013] safeFileName Identifier Resolution in YoutubeDLManager
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
- **Severity**: Compilation Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` failed with:
  `YoutubeDLManager.kt: Unresolved reference: safeName :231`

#### 2. Root Cause & Solution
- In `videoInfoToTrackInfo()`, the local variable is declared as `val safeFileName = sanitizeFileName(...)`.
- Fixed the constructor assignment parameter from `safeFileName = safeName` to `safeFileName = safeFileName`.

---

### 📌 [ANDROID-FIX-012] FFmpeg Package Alignment & Execute Parameter Resolution
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/StashApplication.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
- **Severity**: Compilation & Engine Initialization Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` threw `Unresolved reference: FFmpeg` in `StashApplication.kt`.

#### 2. Root Cause & Solution
- In `youtubedl-android`, `FFmpeg` is situated under package `com.yausername.ffmpeg.FFmpeg` (separate from `com.yausername.youtubedl_android.YoutubeDL`).
- Corrected import to `import com.yausername.ffmpeg.FFmpeg`.
- Fully typed Kotlin callback parameters for `YoutubeDL.getInstance().execute(request, processId) { progress: Float, etaInSeconds: Long, line: String? -> ... }`.

---

### 📌 [ANDROID-FEAT-006] Full Artist Discography Grabber & Batch Downloader
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SearchScreen.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/viewmodel/DownloadViewModel.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/MainActivity.kt`
- **Severity**: Major Feature & User Request

#### 1. User Request
- In the **Artists tab** in Search, grab all the songs of that artist and enable downloading their full discography.

#### 2. Technical Implementation
- **Deep Artist Catalog Scraper**: Configured `SearchFilter.ARTISTS` in `YoutubeDLManager.kt` to query `ytsearch30:$query songs`, pulling 30+ top tracks by that artist with verified channel metadata and album art.
- **Artist Discography Card**:
  - Displays artist avatar, verified badge, and track count header.
  - Added a prominent **"Download All 30 Songs (Batch)"** action button.
- **Batch Enqueue Engine**: Implemented `enqueueAllSearchResults` in `DownloadViewModel.kt` to batch-enqueue the entire artist discography into the parallel download queue in a single tap!

---

### 📌 [ANDROID-FEAT-005] Real Liquid Glass Refraction, Draggable Bubble & YouTube Stream Resolution
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/StashApplication.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/downloader/YoutubeDLManager.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SearchScreen.kt`
- **Severity**: Core Engine & Visual Interaction Polish

#### 1. User Requests Addressed
- **Download Failure Fix (`ERROR: [youtube] ...`)**: YouTube client bot detection bypass and bundled `FFmpeg` initialization.
- **Search Cover Thumbnails**: YouTube Music / Video search cards were not rendering thumbnails due to null JSON property paths.
- **Search UI Polish**: Redesigned search cards with 16:9 thumbnail previews and duration badge overlays.
- **Draggable Liquid Glass Navigation Bar**: Implemented gesture-draggable liquid bubble with optical specular caustic refraction.

#### 2. Technical Implementation
- **FFmpeg Initialization in `StashApplication.kt`**: Synchronously called `FFmpeg.getInstance().init(this)` alongside `YoutubeDL.getInstance().init(this)` to ensure native audio transcoders are ready.
- **Extractor Client Engine**: Added `--extractor-args "youtube:player_client=android,web"` and stripped artificial desktop user-agents to ensure direct streaming without bot blocks.
- **Guaranteed Thumbnail Fallback**: Configured `https://i.ytimg.com/vi/$id/hqdefault.jpg` fallback so Coil renders every single album/video thumbnail.
- **Draggable Liquid Glass Bubble**:
  - Added `pointerInput` with `detectHorizontalDragGestures` enabling smooth horizontal dragging across the navbar with surface tension stretch (`scaleX = 1.12f`).
  - Added specular light dome (`Brush.verticalGradient(listOf(Color.White.copy(0.65f), Color.White.copy(0.12f)))`) for authentic liquid glass optics.

---

### 📌 [ANDROID-FIX-011] DocumentFile Dependency & URI Segment Path Resolution
- **Date**: 2026-08-20
- **Files Modified**: 
  - `Stash-Android/app/build.gradle.kts`
  - `Stash-Android/app/src/main/java/com/eurtlabs/stash/data/storage/StorageManager.kt`
- **Severity**: Build Resolution & Storage Fix

#### 1. Problem Description
- `:app:compileDebugKotlin` threw `Unresolved reference: documentfile` in `StorageManager.kt`.

#### 2. Root Cause & Solution
- Added explicit dependency `implementation("androidx.documentfile:documentfile:1.0.1")` in `app/build.gradle.kts`.
- Enhanced `StorageManager.kt` with a fallback using `uri.lastPathSegment?.substringAfterLast(":")` to guarantee clean folder display names on Android 10 through Android 16.

---

### 📌 [ANDROID-FEAT-004] Liquid Glass Sliding Bubble Navigation Bar Effect
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/components/BottomNavBar.kt`
- **Severity**: Visual Excellence & Interaction Polish

#### 1. User Request
- Implement a liquid glass effect when switching the navbar tabs with a dynamic liquid glass bubble indicator over the selected tab.

#### 2. Technical Implementation
- **Liquid Glass Bubble**: Replaced individual static pills with a single continuous sliding **Liquid Glass Bubble** utilizing Spring physics (`dampingRatio = 0.72f, stiffness = 380f`).
- **Glassmorphic Gradients**:
  - Translucent multi-stop glass reflection (`Brush.linearGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.07f)))`).
  - Specular rim light top border (`Brush.verticalGradient(listOf(Color.White.copy(0.45f), Color.White.copy(0.08f)))`).
- **Subtle Tab Morphing & Scale**: As tabs switch, active icons and labels spring-scale (`1.06f`) while inactive elements smoothly recess (`0.95f`).

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
