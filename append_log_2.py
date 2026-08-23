
import datetime

log_content = """
### [August 23, 2026] Metadata Engine Overhaul & WebView Polish

Another quick set of highly requested features and UI tweaks!

**1. Bulletproof Native ID3 / Metadata Tagging:**
Previously, downloaded files were sometimes missing metadata or thumbnails. We fixed this by plugging directly into `yt-dlp`\'s native metadata engine. The downloader now injects the Title, Artist, Album, and Cover Art (Thumbnail) straight into the MP4/MP3 headers during the download itself. This means files will now look beautifully organized in any local music or video player.

**2. Filename Quality & "null" Bug Fix:**
- No more weird `"null"` file names! Added a strict parser that intercepts missing or `"null"` artist/title strings from YouTube\'s API and defaults them cleanly to `"Unknown Artist"` or `"YouTube"`.
- Appended the selected quality string directly into the file name (e.g. `Song [Full HD (1080p)].mp4`) so you can instantly distinguish formats in your local file explorer without checking file properties.

**3. WebView Mobile Scaling:**
The hidden YouTube login WebView in the Settings was rendering strangely because Android defaults to desktop viewport scaling. Injected `useWideViewPort`, `loadWithOverviewMode`, and a strict `Mobile Android 13` user agent into the `WebSettings` so the CAPTCHA and login screens now perfectly scale to fit your phone\'s layout.
"""

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + log_content)

