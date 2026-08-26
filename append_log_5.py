
import datetime

main_changelog = """
### Fixes & Enhancements
- **Download Engine (PC)**: Restored `youtube:player_client=ios,android,tv` extractor arguments to bypass YouTube web client throttling, unlocking 1080p, 2K, and 4K video downloads.
- **Metadata (PC)**: Forced `yt-dlp` to natively embed ID3 tags and high-resolution JPEG thumbnails directly during the download phase (`--embed-metadata`, `--embed-thumbnail`).
- **File Organization (PC)**: Implemented dynamic subfolder generation; downloading a playlist or artist library now automatically routes tracks into a dedicated subfolder within the chosen download directory.
"""

dev_changelog = """
### [August 26, 2026] Download Engine Refinements & Quality Unlocking

We tackled three major logic bugs in the PC `DownloadEngine` today:
1. **The 1080p Blockade**: YouTube recently started heavily punishing generic web traffic, capping video downloads at 720p. Since we removed the `tv` client earlier to speed up extraction, we inadvertently triggered this throttle. I restored the `youtube:player_client=ios,android,tv` spoofing to bypass the resolution restriction and bring back 4K downloads!
2. **Missing Metadata**: The PC app was relying on the post-download Node.js tagger, which sometimes failed on complex covers. I injected the exact FFmpeg native embedding flags (`--embed-metadata`, `--embed-thumbnail`) directly into `yt-dlp` to ensure bulletproof tagging during the download stream, just like we did on Android.
3. **Subfolder Routing**: Instead of vomiting 100 playlist tracks directly into the root download folder, the `DownloadEngine` and `StashOrchestrator` now dynamically read the `playlistName` (or `artist` name) and generate a clean subfolder for the batch!
"""

with open("CHANGELOG.md", "r", encoding="utf-8") as f:
    content = f.read()

# Insert before the first ### Highlights if we are appending to [2.1.0] section
parts = content.split("### Highlights", 1)
if len(parts) == 2:
    new_content = parts[0] + main_changelog + "\n### Highlights" + parts[1]
    with open("CHANGELOG.md", "w", encoding="utf-8") as f:
        f.write(new_content)

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + dev_changelog)

