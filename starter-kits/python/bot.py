"""BattleCode Cyber-Grid starter bot. Replace choose_actions with your strategy."""

import json
import sys


def choose_actions(observation):
    # One action per visible friendly unit. An empty list means WAIT for all units.
    return []


print("READY", flush=True)
for line in sys.stdin:
    try:
        observation = json.loads(line)
        actions = choose_actions(observation)
        if not isinstance(actions, list):
            actions = []
    except (ValueError, TypeError):
        actions = []
    print(json.dumps({"actions": actions}, separators=(",", ":")), flush=True)
