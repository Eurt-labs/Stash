
import sys

with open("src/main/features/downloader/DownloadEngine.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "public static cookiesFile:" in line:
        continue
    # Remove any stray comments about loading cookies
    if "// Load previously saved cookies if they exist" in line:
        continue
    new_lines.append(line)

with open("src/main/features/downloader/DownloadEngine.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

