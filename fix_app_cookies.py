
import sys

with open("src/main/app.ts", "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "const cookiesPath = path.join(app.getPath('userData'), 'youtube_cookies.txt')" in line:
        skip = True
        continue
    if skip and "}" in line and "createWindow" not in line and "app.on" not in line:
        skip = False
        continue
    if skip:
        continue
    new_lines.append(line)

with open("src/main/app.ts", "w", encoding="utf-8") as f:
    f.writelines(new_lines)

