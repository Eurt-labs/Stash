
import sys

with open("src/main/features/downloader/DownloadEngine.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "if (DownloadEngine.cookiesFile && fs.existsSync(DownloadEngine.cookiesFile)) {" in line:
        skip = True
        continue
    if skip and "}" in line and "args.push" not in line:
        skip = False
        continue
    if skip:
        continue
    new_lines.append(line)

with open("src/main/features/downloader/DownloadEngine.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

