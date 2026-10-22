"""Ten-turn protocol smoke against a waiting DummyBot inside the CI container."""

import json
import os
import select
import subprocess
import sys
import time

language = sys.argv[1]
command = {
    "python": ["python3", "-u", "/bot/bot.py"],
    "cpp": ["/bot/bot"],
    "java": ["java", "-Xmx64m", "-Xss256k", "-XX:MaxMetaspaceSize=32m", "-XX:+UseSerialGC", "-cp", "/bot", "Bot"],
}[language]


def read_line(process, timeout):
    if not select.select([process.stdout], [], [], timeout)[0]:
        raise RuntimeError("Bot timed out")
    line = process.stdout.readline(16_386)
    if not line or len(line) > 16_384 or not line.endswith(b"\n"):
        raise RuntimeError("Bot crashed or exceeded the output limit")
    return line.decode("utf-8").strip()


def turn(process, observation, bank):
    started = time.monotonic()
    process.stdin.write(json.dumps(observation, separators=(",", ":")).encode() + b"\n")
    process.stdin.flush()
    response = read_line(process, 0.05 + bank)
    elapsed = time.monotonic() - started
    bank -= max(0.0, elapsed - 0.05)
    if bank < 0:
        raise RuntimeError("Time bank exhausted")
    payload = json.loads(response)
    if not isinstance(payload, dict) or not isinstance(payload.get("actions"), list):
        raise RuntimeError("Invalid action JSON")
    return bank


with subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                      stderr=subprocess.DEVNULL, env={"PATH": os.environ.get("PATH", "/usr/bin:/bin"),
                                                   "HOME": "/tmp"}) as student, \
     subprocess.Popen(["python3", "-u", "/test/dummy_bot.py"], stdin=subprocess.PIPE,
                      stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                      env={"PATH": "/usr/bin:/bin", "HOME": "/tmp"}) as dummy:
    if read_line(student, 1.0) != "READY" or read_line(dummy, 1.0) != "READY":
        raise RuntimeError("READY handshake failed")
    student_bank = dummy_bank = 2.0
    for tick in range(1, 11):
        base = {"tick": tick, "seed": 41, "size": 16, "resources": 100 + 10 * tick,
                "visible_cells": [], "friendly_units": [], "visible_enemies": []}
        student_bank = turn(student, {**base, "team": 1}, student_bank)
        dummy_bank = turn(dummy, {**base, "team": 2}, dummy_bank)
    student.terminate()
    dummy.terminate()
print("10-turn protocol smoke passed")
