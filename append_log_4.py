
import datetime

main_changelog = """
## [2.1.0] - """ + datetime.date.today().strftime("%Y-%m-%d") + """

### Highlights
- **Cross-Platform Parity**: Unified versioning (v2.1.0) across the Electron PC app and Native Android app.
- **UI Architecture**: Resolved critical JSX Fragment parsing errors in `SettingsBar.tsx` for stable rendering.
- **Build Integrity**: Fixed Electron taskbar icon inheritance by hardlinking `app-resources/icon.png` to the `BrowserWindow` lifecycle.
- **Documentation**: Overhauled `README.md` to map the new Domain-Driven Design (DDD) architecture and updated feature capabilities for Native ID3 tagging and SAF storage hooks.
"""

dev_changelog = """
### [August 26, 2026] v2.1.0 Architecture Finalization & PC Build Polish

The push for total cross-platform parity! We officially bumped the version to **v2.1.0** across both the Android app (`build.gradle.kts`) and the PC Electron app (`package.json`).

**1. PC Build Polish:**
- The Electron `BrowserWindow` was silently dropping our custom icon and falling back to the default Atom logo because it wasn\'t explicitly bound during initialization. Hardlinked it to `app-resources/icon.png` in `app.ts` so the taskbar and system tray display perfectly now.
- Caught and resolved a nasty little React syntax error in `SettingsBar.tsx` caused by returning adjacent root elements. Wrapped them in a Fragment `<> ... </>` and the compiler is happy again.

**2. Documentation Push:**
- Gave the `README.md` a massive facelift to reflect the new Domain-Driven Design (DDD) modular structure.
- Made sure to highlight all the cool stuff we built over the last few days (Native ID3 FFmpeg tagging, Liquid Glass UI extracted kit, and the SAF duplicate file fix).
"""

with open("CHANGELOG.md", "r", encoding="utf-8") as f:
    content = f.read()

# Insert after the first horizontal rule
parts = content.split("---", 1)
if len(parts) == 2:
    new_content = parts[0] + "---\n" + main_changelog + parts[1]
    with open("CHANGELOG.md", "w", encoding="utf-8") as f:
        f.write(new_content)

with open("Stash-Android/CHANGELOG_AND_FIXES.md", "a", encoding="utf-8") as f:
    f.write("\n" + dev_changelog)

