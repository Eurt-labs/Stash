# ⚡ Stash Downloader v1.3.0

![Stash Banner](app-resources/stash_app_banner.png)

![Rainbow Separator](https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139b6daec5c5.gif)

Hey there! Welcome to **Stash**—a high-performance, elegant media downloader built with Jetpack Compose Desktop for JVM. Whether you want to format-shift your favorite music tracks, playlists, albums, or videos from YouTube and YouTube Music into clean, tagged local files, Stash has got your back. 

It's fast, it's pretty, and it actually works. Let's get you set up!

---

## 🚀 How It works: The sequential pipeline

Stash runs a super stable **5-Phase Sequential Batch Pipeline** that downloads and converts everything cleanly without choking your system or getting your IP banned. 

![Stash Flow](app-resources/stash_download_flow.png)

```mermaid
flowchart TD
    A["User pastes link in UI"] --> B["StashOrchestrator.processLink()"]
    B --> C["Phase 1: FETCH\n(Metadata Query via yt-dlp)"]
    C --> D["ManifestManager\n(Save metadata to temp JSON manifest)"]
    D --> E["DownloadQueueManager\n(Sequential execution)"]
    
    E --> F["Phase 2: DOWNLOAD\n(Downloads track-by-track via yt-dlp)"]
    F --> G["Phase 3: CONVERT\n(Transcodes one-by-one via FFmpeg)"]
    G --> H["Phase 4: TAG & MOVE\n(ID3v2.4 tags + embed artwork)"]
    H --> I["Phase 5: CLEANUP\n(Deletes manifest & temp cache files)"]
```

### The 5 Phases:
1. **FETCH**: Stash parses your link (using regex in `LinkParser`) and queries metadata using `yt-dlp`. It saves this metadata to a temporary JSON manifest file.
2. **DOWNLOAD**: Tracks are downloaded sequentially (one-by-one) using `yt-dlp` to extract the best audio streams, saving them to a temporary cache.
3. **CONVERT**: Transcoding is handled one-by-one using `ffmpeg` to target your selected format (MP3/AAC) and quality (Low/Mid/High).
4. **TAG & MOVE**: Converted files are tagged with ID3v2.4 metadata (including high-resolution album artwork) using `mp3agic` and moved to your chosen output folder.
5. **CLEANUP**: All cache and manifest files are deleted, leaving a clean workspace.

---

## 🛠️ Instalation & Dependecies

Installing Stash is super easy. We've got a couple of ways for you to get up and running:

### Method 1: The Provided Standalone Installer (Easiest!)
We have already compiled and provided a standalone Windows MSI installer directly in this repository!
* You can grab the installer from the [build-installer/](file:///c:/Projects/Stash/build-installer/) folder.
* Simply run the [Stash-1.3.0.msi](file:///c:/Projects/Stash/build-installer/Stash-1.3.0.msi) file to install the application instantly.
* **All dependecies are bundled!** The installer packages `yt-dlp.exe`, `ffmpeg.exe`, and `ffprobe.exe` out of the box and sets them up inside the application's resources folder. No manual environment PATH configuration needed!
* **Post-Install Launch:** Once the installation finishes, check the "Launch Stash" box, and it will open the app automaticaly.

If you ever want to rebuild the installer yourself:
```cmd
.\gradlew.bat :app:packageMsi
```
The compiled MSI will be written to: `app/build/compose/binaries/main/msi/Stash-1.3.0.msi`

### Method 2: Running Locally from Source
If you are running the project from source, you'll need:
- **Java Development Kit (JDK) 17 or higher**
- **FFmpeg & yt-dlp**: You can place `yt-dlp.exe`, `ffmpeg.exe`, and `ffprobe.exe` in `app-resources/windows/` before building, or Stash will ask to install them automatically for you inside the app when it starts up!

To start the app in development mode:
```cmd
.\gradlew.bat :app:run
```

---

## 📦 Manual Dependency Installation Commands
If you prefer installing the dependencies globally on your machine instead of using our automatic bundled/in-app downloaders:

### Windows
* **Using winget (Command Prompt / PowerShell):**
  ```cmd
  winget install yt-dlp
  winget install Gyan.FFmpeg
  ```
* **Using scoop:**
  ```cmd
  scoop install yt-dlp ffmpeg
  ```

### macOS (using Homebrew)
```bash
brew install yt-dlp ffmpeg
```

### Linux (Debian/Ubuntu)
```bash
sudo apt update
sudo apt install ffmpeg

# Download and install the latest yt-dlp binary
sudo wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
sudo chmod a+rx /usr/local/bin/yt-dlp
```

---

## ✨ Cool Features

- ⏸ **Pause & Resume**: You can pause any track in the queue to save bandwidth and resume it later. The download engine resumes from partial files seamlessly.
- 📂 **Smart Folder Organization**: Downloads automatically group into subfolders named after the Album/Playlist. Individual tracks download directly without creating subdirectories.
- 🔍 **YouTube Playlist Parsing**: Full support for playlist URLs (including watch-playlists with `&list=` parameters).
- 🔄 **Self-Updating Dependency**: Check and update `yt-dlp` directly within the app UI.

---

## ⚖️ Legal & Fair Use Status

Stash is developed strictly as an educational project and personal archiving utility.

- **No DRM Circumvention**: Stash **does not** bypass, disable, decrypt, or otherwise crack any Digital Rights Management (DRM) protection layers (such as Widevine, FairPlay, or PlayReady). 
- **Fair Use Compliant**: Stash facilitates offline format-shifting for personal use. Users are responsible for complying with the Terms of Service of the platforms and local copyright regulations.

---

## 📝 License

This project is open-source and released under the [MIT License](LICENSE).
