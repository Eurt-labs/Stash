
import re

with open("Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    clean_line = re.sub(r"\".*?\"", "\"\"", line)
    clean_line = re.sub(r"//.*", "", clean_line)
    
    for char in clean_line:
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
    
    if 750 <= i + 1 <= 900:
        print(f"{i+1:04d} | {depth:02d} | {line.rstrip()}")

