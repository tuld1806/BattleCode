# GAME PLUG-IN ARCHITECTURE, SANDBOX & REPLAY PROTOCOL
## Simulation Core & Plug-in SDK Specification

This document defines the architectural standards allowing instructors and system administrators to develop new games and plug them into the platform as standalone **Plug-ins** without modifying the core system codebase.

---

## 1. GAME PLUG-IN ARCHITECTURE

Each Game Plug-in is an isolated modular package. The simulation engine and backend services run on Java; the graphical visualizer runs in client browsers. First-year student bots run exclusively on Python, C++, or Java as declared in the manifest.

```
my-game-plugin/
├── plugin.manifest.json       # Metadata identity, submission rules & configuration schema
├── engine/
│   ├── src/main/java/...      # Game Engine implementing Java IGameEngine interface
│   └── resources/             # Default maps, assets, and game configs
├── starter-kits/              # Git template repositories for students (downloadable at /[gameSlug]/overview)
│   ├── python/
│   ├── cpp/
│   └── java/
├── bots/                      # Built-in reference bots (DummyBot, EasyBot, HardBot sparred 24/7 at /[gameSlug]/practice)
└── visualizer/
    └── renderer.js            # ES module implementing ReplayPlayer rendering interface for /replay/*
```

### 1.1. `plugin.manifest.json` Specification
This manifest serves as the formal interface contract between the Plug-in and Core Platform services:
- **Admin Management:** Uploaded and managed via `/admin/plugins`; game parameters defined in `config_schema` are dynamically configured at `/admin/plugins/[gameSlug]/config`.
- **Student Portal:** Consumed by `/[gameSlug]/overview` (rules & specs), `/[gameSlug]/submission` (file structure validation), and `/[gameSlug]/practice` (bot sparring limits).
- **Match Runner:** Defines CPU/RAM quotas and banned system calls enforced inside `bwrap` sandboxes.

```json
{
  "slug": "cyber-grid",
  "name": "Cyber-Grid: Neon Dominion",
  "version": "1.0.0",
  "description": "Turn-based grid tactical game exercising pathfinding algorithms and resource management.",
  "supported_player_counts": [2, 4],
  "visualizer": {
    "module": "visualizer/renderer.js",
    "api_version": "1.0"
  },
  "sandbox_limits": {
    "tick_timeout_ms": 50,
    "time_bank_ms": 2000,
    "memory_limit_mb": 96,
    "max_output_bytes_per_tick": 16384
  },
  "submission_requirements": {
    "supported_languages": ["python", "cpp", "java"],
    "entry_file_by_language": {
      "python": "bot.py",
      "cpp": "src/main.cpp",
      "java": "src/main/java/Bot.java"
    },
    "required_files": [
      "bot.json",
      "bot_meta.json"
    ],
    "allowed_extensions": [".py", ".cpp", ".h", ".hpp", ".java", ".json"],
    "max_total_size_kb": 1024,
    "forbidden_identifiers_by_language": {
      "python": ["eval", "exec", "subprocess", "socket"],
      "cpp": ["system", "popen", "fork", "exec"],
      "java": ["Runtime.exec", "ProcessBuilder", "java.net"]
    }
  },
  "config_schema": {
    "type": "object",
    "properties": {
      "map_size": {
        "type": "integer",
        "enum": [16, 32, 48, 64],
        "default": 32,
        "title": "Map Size (N x N)"
      },
      "max_ticks": {
        "type": "integer",
        "minimum": 100,
        "maximum": 2000,
        "default": 1000,
        "title": "Maximum Match Ticks"
      },
      "fog_of_war": {
        "type": "boolean",
        "default": true,
        "title": "Enable Fog of War"
      },
      "core_passive_income": {
        "type": "integer",
        "default": 10,
        "title": "Passive Data Bytes generated per tick at Core"
      },
      "node_income": {
        "type": "integer",
        "default": 5,
        "title": "Data Bytes harvested per Node per tick"
      }
    }
  }
}
```

---

### 1.2. Java Engine Interface (`IGameEngine`)
Every Game Plug-in must implement the canonical Java interface executed by the Hybrid Java Match Worker:

```java
public interface IGameEngine {
    MatchState initMatch(GameConfig config, List<TeamInfo> teams, long seed);
    BotObservation getBotObservation(MatchState state, long teamId);
    TickStepResult stepTick(
        MatchState state,
        Map<Long, BotAction> botActions,
        Map<Long, BotMetrics> botMetrics
    );
    boolean isTerminal(MatchState state);
    List<MatchFinalResult> getFinalResults(MatchState state);
}
```

---

## 2. SANDBOX ISOLATION & TIMING BUDGET

Backend Java on the Hybrid Match Worker cluster (Render Free Tier baseline + Burst Docker Workers) runs the engine and coordinates process sandboxes. Student bots execute as isolated OS processes; no shared memory or in-process interpreters are used as security boundaries.

### 2.1. Bot Process Protocol & Isolation
1. **Persistent Process:** Java Match Runner spawns one persistent OS process per Bot using `ProcessBuilder` and keeps it alive throughout the match, eliminating startup overhead per tick.
2. **NDJSON over Stdin/Stdout:** At tick 0, the Bot outputs `READY\n`. Each subsequent tick, the Runner writes a single-line observation JSON to `stdin`. The Bot responds with exactly one line of JSON on `stdout` and flushes immediately. Debug logs are emitted strictly to `stderr` and collected out-of-band into Replay metadata.
3. **OS-Level Sandbox:** Linux execution is wrapped with `bubblewrap` (`bwrap`) or `nsjail`: `--unshare-net`, `--ro-bind`, 96MB RAM, 16KB max output per tick. Static code analysis is never treated as the sole security boundary.
4. **Determinism:** The Java Engine controls the random seed and state progression. Bots must rely solely on deterministic game inputs.
5. **Pre-match Compilation:** GitHub Actions CI builds student C++ / Java artifacts. Match workers execute pre-built binaries without counting build duration against match tick budgets.

### 2.2. Timing Budget Algorithm (Tick Budget & Time Bank)
Each Bot $i$ starts with reserve time bank $B_i(0) = 2000\text{ms}$ and turn budget $T_{\text{base}} = 50\text{ms}$.
- **Tick 0 Warmup:** Max `1000ms` allowed for JVM / interpreter startup until `READY\n` is received.
- **Tick 1+ Turns:** High-resolution monotonic clock (`System.nanoTime()`). Turn deadline is $T_{\max, i}(t) = 50\text{ms} + B_i(t-1)$. If execution $\Delta t_i \le 50\text{ms}$, time bank is preserved; excess beyond 50ms is deducted from bank.
- **Event-Driven Tick Advancement:** Runner transitions to the next tick immediately upon receiving all expected responses (`\n`). Runner does not pause for the full 50ms.
- Concurrently reads `stdout` and `stderr` on worker threads to avoid OS pipe deadlock. On crash or time-bank exhaustion, terminates the entire process tree (`process.descendants()`).

---

## 3. REPLAY FILE FORMAT (`replay.json.gz`) & WEB PLAYER

To ensure lightweight downloads and 60fps scrubbing over 1,200 ticks on Cloudflare Pages, `replay.json` combines **Keyframe Snapshots** (every 50 ticks) and **Delta Frames**.

Internal engine calculations use 6 decimal places (IEEE 754); coordinates serialized into `replay.json` are rounded to **at most 2 decimal places** (saving ~60% payload size). Debug logs are capped at `200 chars/team/tick` and total match debug log $\le 30\text{KB}$. Replays are uploaded directly to Cloudflare R2 as `.json.gz`.

### 3.1. Standard `replay.json` Schema
```json
{
  "format_version": "1.0",
  "match_metadata": {
    "match_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "plugin_slug": "cyber-grid",
    "plugin_version": "1.0.0",
    "random_seed": 20261020,
    "config": {
      "map_size": 32,
      "max_ticks": 1000,
      "fog_of_war": true
    },
    "teams": [
      { "slot": 1, "team_id": "t1", "name": "BK-Freshman-01", "color": "#00F5D4" },
      { "slot": 2, "team_id": "t2", "name": "UET-Rookies", "color": "#FF2A85" }
    ]
  },
  "initial_state": {
    "map_grid": "/* Base map obstacles and resource nodes */"
  },
  "ticks": [
    {
      "tick": 1,
      "team_stats": {
        "1": { "resources": 60, "score": 250, "cpu_ms": 4.2, "time_bank_ms": 2000 },
        "2": { "resources": 60, "score": 250, "cpu_ms": 12.8, "time_bank_ms": 2000 }
      },
      "entities": [
        { "id": 101, "type": "WORKER", "team": 1, "x": 5.0, "y": 10.0, "hp": 50 },
        { "id": 201, "type": "WORKER", "team": 2, "x": 26.0, "y": 21.0, "hp": 50 }
      ],
      "events": [
        { "type": "MOVE", "unit_id": 101, "from": [4.0, 10.0], "to": [5.0, 10.0] },
        { "type": "ATTACK", "attacker_id": 102, "target_id": 201, "damage": 35 }
      ],
      "debug_logs": {
        "1": ["[Tick 1] Discovered unharvested node at (10, 10) via BFS, dist = 5"],
        "2": ["[Tick 1] Accumulating data bytes for PAWN production"]
      }
    }
  ],
  "result": {
    "winner_slot": 1,
    "win_reason": "CORE_DESTROYED",
    "total_ticks": 642,
    "final_scores": { "1": 1850.0, "2": 420.0 }
  }
}
```

### 3.2. React `ReplayPlayer` & HTML5 Canvas Contract
```typescript
export interface ReplayRenderInput {
  currentState: ReplayFrameState;
  nextState?: ReplayFrameState;
  tick: number;
  interpolation: number; // 0..1 linear motion interpolation between ticks
  perspective: "GOD" | "TEAM_1" | "TEAM_2";
  pixelRatio: number;
}

export interface ReplayRenderer {
  render(input: ReplayRenderInput): void;
  resize?(width: number, height: number, pixelRatio: number): void;
  destroy?(): void;
}

export function createRenderer(canvas: HTMLCanvasElement): ReplayRenderer;
```
- **Lifecycle & Decoupling:** Canvas lifecycle strictly relies on `useRef<HTMLCanvasElement>` and an autonomous `requestAnimationFrame` render loop synchronized with Zustand. State updates are decoupled from React component re-renders to achieve sustained 60fps.
- **Dual Replay Cockpits Integration:**
  - **Practice & Scrimmage Cockpit (`/replay/[matchId]`):** Renders casual matches with perspective switcher (`GOD VIEW`, `TEAM 1 FOG`, `TEAM 2 FOG`), tick scrub timeline, `TickInspector` drawer for per-tick JSON observations/actions, and quick rematch triggers.
  - **Tournament Esports Cockpit (`/replay/[tournamentId]/[matchId]`):** Renders official tournament Bo3 matches with rapid series navigation (`Game 1` | `Game 2` | `Game 3`) and direct return links to `/tournaments/[tournamentId]/bracket` and `/tournaments/[tournamentId]/tournament-leaderboard`.