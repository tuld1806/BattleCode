"""Validate an immutable student checkout before compiling it in the CI sandbox."""

import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1]).resolve()
language = sys.argv[2]
entry = {"python": "bot.py", "cpp": "src/main.cpp", "java": "src/main/java/Bot.java"}[language]
allowed = {".py", ".cpp", ".h", ".hpp", ".java", ".json", ".md", ".yml"}
forbidden = {
    "python": ("eval(", "exec(", "subprocess", "socket"),
    "cpp": ("system(", "popen(", "fork(", "exec("),
    "java": ("Runtime.exec", "ProcessBuilder", "java.net"),
}[language]

manifest = json.loads((root / "bot.json").read_text(encoding="utf-8"))
if manifest.get("language") != language or manifest.get("entry") != entry:
    raise SystemExit("bot.json does not match the selected language and entry")
if not (root / "bot_meta.json").is_file() or not (root / entry).is_file():
    raise SystemExit("Missing bot_meta.json or entry file")

total = 0
for path in root.rglob("*"):
    if ".git" in path.parts:
        continue
    if path.is_symlink():
        raise SystemExit("Symbolic links are not allowed")
    if not path.is_file():
        continue
    if path.suffix not in allowed:
        raise SystemExit(f"Forbidden extension: {path.relative_to(root)}")
    total += path.stat().st_size
    if total > 1_024 * 1_024:
        raise SystemExit("Source package exceeds 1 MiB")
    if path.suffix in {".py", ".cpp", ".h", ".hpp", ".java"}:
        content = path.read_text(encoding="utf-8")
        if any(token in content for token in forbidden):
            raise SystemExit(f"Forbidden identifier in {path.relative_to(root)}")

print(f"Validated {language} package: {total} bytes")
