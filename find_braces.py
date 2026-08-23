
import re

with open("Stash-Android/app/src/main/java/com/eurtlabs/stash/ui/screens/SettingsScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

stack = []
for i, line in enumerate(lines):
    line = re.sub(r"\".*?\"", "\"\"", line)
    line = re.sub(r"//.*", "", line)
    
    for char in line:
        if char == "{":
            stack.append(i+1)
        elif char == "}":
            if stack:
                stack.pop()
            else:
                print(f"Extra close brace at line {i+1}")

for num in stack:
    print(f"Unclosed block started at line {num}")

