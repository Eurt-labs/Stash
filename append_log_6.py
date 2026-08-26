
import datetime

dev_changelog = """
### [August 26, 2026] Icon Build Architecture Note
- **Executable Icon Issue**: Found that the `config/electron-builder.json` had `"signAndEditExecutable": false` hardcoded. Removing this allows `electron-builder` to run `rcedit` to stamp the `icon.ico` directly into the generated Windows `.exe`.
- **Note on Windows Symlinks**: The `rcedit` toolkit (winCodeSign) fails to download/extract on strict Windows environments without Administrator privileges due to restricted Symbolic Link creation. This is a known OS-level restriction, requiring Developer Mode or Admin rights during the first compilation.
"""

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + dev_changelog)

