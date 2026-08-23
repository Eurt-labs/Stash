
import datetime

log_content = """
### [August 23, 2026] SAF Storage Duplication Fix & UI Neutrality

Just a quick patch to fix some weird behavior when saving downloads to custom folders!

**1. Custom Storage (SAF) Fixes:**
If a user selected a custom download folder (like their SD Card), the app was properly copying the file there, but it was leaving the original copy sitting in the internal `Music/Stash` directory! This was confusing because users would see duplicate files, or assume the app just ignored their custom folder choice. I added a strict cleanup pass that deletes the internal ghost copy immediately after a successful move. Also stripped the extension before handing the file to the OS to prevent it from creating weird `.mp4.mp4` double extensions on certain Android versions.

**2. UI Neutrality:**
Removed the "(Recommended)" branding from the default storage option in the `StorageSelectionDialog`. We want to remain completely neutral and let the user decide where their files go without feeling pushed towards the internal storage.
"""

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + log_content)

