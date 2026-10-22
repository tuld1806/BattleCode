# MULTI-RUNTIME SANDBOX & PROCESS ISOLATION ARCHITECTURE

This document specifies the OS-level **Process Isolation** architecture replacing in-memory VM approaches. The platform backend (API, Simulation Engine, Queue Worker, and Match Runner) runs strictly on Java 21; student bots execute in **Python, C++, or Java**. Inter-process communication operates via **Standard I/O (stdin/stdout)**.

---

## 1. COMMUNICATION PROTOCOL (NDJSON OVER STDIN/STDOUT)

Instead of in-memory function calls, the Java Match Runner and student bot processes communicate via standard streams using **Newline-Delimited JSON (NDJSON)**:

1. **Engine $\rightarrow$ Bot (`stdin`):**
   - At startup, the Bot prints `READY\n` to `stdout`.
   - On each subsequent tick, the Java Match Runner serializes the observation payload into a **single line of JSON** terminated by `\n` and flushes it to the Bot's `stdin`.
2. **Bot $\rightarrow$ Engine (`stdout`):**
   - The Bot reads a single line from `stdin`, processes the observation, and writes an action JSON array on a single line terminated by `\n` to `stdout`.
   - **Mandatory Buffer Flush:** The Bot must immediately flush stdout (`sys.stdout.flush()` in Python, `std::cout << std::endl` in C++, `System.out.flush()` in Java).
3. **Debug Stream (`stderr`):**
   - Student debug print statements are emitted to `stderr`.
   - The Match Runner consumes `stderr` asynchronously on a background thread to prevent buffer deadlocks, truncates lines exceeding 200 characters per team per tick, and stores the logs for replay debugging without corrupting `stdout` protocol frames.

---

## 2. SANDBOX ARCHITECTURE & HYBRID WORKERS

- **Baseline Worker:** Runs as a background service on Render Free Tier for round-the-clock 24/7 Practice matches against built-in bots (`/[gameSlug]/practice`) and peer Scrimmage games (`/[gameSlug]/scrimmage`).
- **Burst Docker Workers:** Deployed on laboratory workstations or private cluster nodes for high-concurrency official tournaments (`/tournaments/[tournamentId]/config`), pulling match jobs from Supabase PostgreSQL via Supavisor.
- **Isolation Tools:** Utilizes lightweight Linux namespaces via **Nsjail** or **Bubblewrap (`bwrap`)**.

### 2.1. Backend / Burst Worker `Dockerfile`
```dockerfile
FROM eclipse-temurin:21-jdk-bookworm

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    g++ \
    python3 \
    bubblewrap \
    git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/battlecode-backend-1.0.0-SNAPSHOT.jar ./backend.jar

CMD ["java", "-Xms64m", "-Xmx160m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=48m", "-jar", "backend.jar"]
```

### 2.2. OS Boundary Enforcement via `bubblewrap` (`bwrap`)
When `MatchRunner` invokes `ProcessBuilder`, it applies container and kernel limits:
- **Network Isolation (`--unshare-net`):** Completely severs network access.
- **Filesystem Confinement (`--ro-bind`):** Read-only mount of the runtime and student bot folder; temporary writable storage is isolated and quota-limited.
- **Resource Constraints:** Max **96 MB RAM**, CPU quota, and tick output capped at **16 KB**.

---

## 3. COMPILATION & RUNTIME MATRIX

| Language | Build Command (CI Phase - Max 15s) | Execution Command (Match Phase) | Technical Notes |
| :--- | :--- | :--- | :--- |
| **Python** | `python3 -m py_compile bot.py` | `python3 -u bot.py` | `-u` forces unbuffered binary stdin/stdout. |
| **C++** | `g++ -O3 -std=c++20 bot.cpp -o bot` | `./bot` | `-O3` optimizes compute speed. |
| **Java** | `javac -encoding UTF-8 Bot.java` | `java -Xmx64m -Xss256k -XX:MaxMetaspaceSize=32m -XX:+UseSerialGC Bot` | JVM parameters keep footprint strictly within 96 MB cgroup. |

### Artifact Storage on Cloudflare R2
Compilation and smoke tests run within GitHub Actions. Validated binaries (`bot` or `*.class`) are uploaded to **Cloudflare R2** via Presigned PUT URLs issued by the Java API. When a match is claimed, the Match Worker downloads the artifact to local cache before spinning up the sandbox.

---

## 4. TIME BUDGET & WARMUP ALGORITHM

1. **Persistent Process:**
   - Bot processes remain alive throughout all match ticks to eliminate repeated JVM / Python interpreter startup latency.
   - **Tick 0 (Warmup):** Up to **1,000ms** allowed for process initialization until `READY\n` is received.
   - **Tick 1+ (Turns):** Base budget $T_{\text{base}} = 50\text{ms}$ + reserve **Time Bank = 2000ms**.
2. **Measurement via Java Monotonic Clock:**
   - Record `long start = System.nanoTime()` immediately before flushing to `process.getOutputStream()`.
   - Read line from `process.getInputStream()`. Calculate elapsed duration $\Delta t = (\text{System.nanoTime()} - start) / 1_000_000.0\text{ ms}$.
   - If $\Delta t > 50\text{ms}$, deduct $(\Delta t - 50\text{ms})$ from `time_bank_ms`.
   - If `time_bank_ms <= 0` or timeout exceeded, terminate the process tree (`process.descendants()`) and issue `DISQUALIFIED_TIMEOUT`.