# 📱 Stash Android — Changelog & Technical Fixes Archive

---

### 📌 [ANDROID-FIX-002] Incompatible Gradle JVM Version & Wrapper Upgrade
- **Date**: 2026-08-20
- **Files Modified**: `Stash-Android/gradle/wrapper/gradle-wrapper.properties`
- **Severity**: Build Environment Setup

#### 1. Problem Description & Symptoms
- Android Studio showed `Incompatible Gradle JVM version` error when syncing:
  `The project's Gradle version 8.4 is incompatible with the Gradle JVM version currently selected to run Gradle build.`

#### 2. Technical Solution & Implementation
- Upgraded Gradle distribution wrapper to `gradle-8.9-bin.zip` in `gradle-wrapper.properties`.
- Documented steps to select the compatible embedded Android Studio JDK (JDK 17/21).

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
