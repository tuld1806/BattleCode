# Cyber-Grid starter kits

Each bot prints `READY` within one second, then reads one JSON observation per line and writes one JSON action object per line. Flush standard output after every line. Use standard error for debug messages. The process persists through the entire match.

The starter code intentionally sends `{"actions":[]}`, which waits for every unit. Change the decision function or loop to emit records such as `{"unit_id":2,"type":"MOVE","direction":"EAST"}`. Supported actions are `WAIT`, `MOVE`, `ATTACK`, `SPAWN`, and `BUILD`; see the Cyber-Grid rules in `../docs/doc4_games_cybergrid_stellar.md`.

Submit a public GitHub repository URL, the full 40-character lowercase commit SHA, and the language. The platform CI reads only the pinned commit. Keep `bot.json`, `bot_meta.json`, and the entry file at the paths shown in each kit.
