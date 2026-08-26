
import sys

# Fix IPC index.ts
with open("src/main/ipc/index.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "ipcMain.handle('stash:loginYouTube'" in line:
        skip = True
        continue
    if skip and "})" in line:
        skip = False
        continue
    if skip:
        continue
    if "import { CookieManager }" in line:
        continue
    new_lines.append(line)

with open("src/main/ipc/index.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

# Fix preload.ts
with open("src/main/preload.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "loginYouTube:" in line:
        continue
    new_lines.append(line)

with open("src/main/preload.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

# Fix types/index.ts
with open("src/shared/types/index.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "loginYouTube: () => Promise<void>" in line:
        continue
    new_lines.append(line)

with open("src/shared/types/index.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

