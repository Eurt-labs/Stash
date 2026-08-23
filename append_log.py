
import datetime

log_content = """
### [August 23, 2026] WebView CAPTCHA Bypass & 4K Streaming Unlock

Hey team, just pushing a few major updates to how we handle the downloading pipeline!

**1. CAPTCHA Evasion & Cookie Syncing:**
If we get hit by an age-restriction or CAPTCHA, it sucks. I built a native `WebView` directly into the `SettingsScreen` where users can legitimately log into their YouTube account. We extract the Netscape cookies directly from the WebView and feed them seamlessly into `yt-dlp`. 
*(Note: To keep it 100% secure, we sanitize the cookies first so the downloader never gets any sensitive Google account tokens!)*

**2. Speed & 1080p/4K Quality Unlock:**
We had a bug where downloading videos would frustratingly cap out at 360p or 720p, no matter what quality you selected. Turns out, the YouTube API restricts the `android` and `tv` endpoints for unauthenticated requests. I stripped out the `--extractor-args` forcing those clients, moving us back to the `default` web client. This fixed two things at once:
- **Speed:** Fetches are blazing fast again since we use standard guest tokens without needing to spoof PO Tokens.
- **Quality:** `yt-dlp` can now see the raw 1080p, 1440p, and 4K streams again, so the quality selector actually works!

**3. Settings Screen Syntax Polish:**
Fixed a nasty missing brace issue in `SettingsScreen.kt` that was causing 14+ compiler errors. Also bumped some properties like `setJavaScriptEnabled(true)` to use direct Java setters just to keep the Compose compiler happy. All braces are perfectly balanced now.
"""

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + log_content)

