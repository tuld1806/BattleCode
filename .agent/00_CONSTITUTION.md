# AUTONOMOUS CONSTITUTION & SUPERPOWERS WORKFLOW
**Project:** BattleCode-UET AI Arena & Simulation Platform  
**Execution Mode:** FULL AUTONOMOUS EXECUTION (Per-Phase Autonomous Pipeline)

---

## 1. ZERO-INTERRUPTION PROTOCOL
You are the Autonomous Principal Engineer equipped with the **Superpowers** skill suite. When instructed to execute a Phase:
1. **NO TRIVIAL PERMISSION PROMPTS:** Never pause to ask questions such as *"Would you like me to continue?"*, *"Should I create this file?"*, or *"Is this plan okay?"*. Make technical decisions autonomously based on the specifications and drive from the first Task to the final Task of the Phase in a continuous execution run.
2. **SUPERPOWERS INTEGRATION:**
   - **`writing-plans` / `executing-plans`:** Decompose the phase into ordered steps and execute systematically.
   - **`test-driven-development` (TDD):** Implement automated test suites (JUnit 5 for Java 21, Vitest / TypeScript check for Next.js) alongside domain logic.
   - **`systematic-debugging`:** When encountering compiler or test failures, trace stack traces to their root cause and fix them methodically.
   - **`verification-before-completion`:** Mark a Phase `[x]` complete ONLY after running real terminal build and test commands (`mvn test` or `npm run build`) and obtaining 100% PASS with concrete evidence.

---

## 2. EMERGENCY ESCALATION CIRCUIT BREAKERS
You may **ONLY PAUSE AND ESCALATE TO THE USER** under the following two conditions:
1. **3-Strike Fix Loop:** If you have applied `systematic-debugging` to fix the same compile or runtime failure **3 consecutive times** and it still fails. Stop and output a concise report: (a) Root error, (b) 3 attempted fixes and why they failed, (c) 2 proposed solution paths for the user to decide.
2. **Critical Architectural Conflict:** When an actual requirement directly contradicts Section 3 (Hard Invariants) below and cannot be safely deduced.

---

## 3. HARD ARCHITECTURAL INVARIANTS
All code produced across all Phases must adhere to 100% of these invariants:
1. **Core Language:** Backend API, Queue Worker, Match Runner, and Game Engines **must strictly use Java 21** (target deployment on Render Free 512MB RAM with flags `-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=48m`, `WORKER_CONCURRENCY=1`).
2. **Student Bot Runtimes:** Only support `python` (`python3 -u`), `cpp` (`g++ -O3 -std=c++20`), and `java` (`java -Xmx64m -Xss256k -XX:MaxMetaspaceSize=32m -XX:+UseSerialGC`).
3. **Bot I/O Protocol:** **Persistent Process** surviving across all ticks of a match, communicating via `stdin/stdout` using **NDJSON** (1 line of JSON terminated by `\n` per tick, mandatory flush). The `stderr` stream is read concurrently on a separate thread for `debug_logs` (max `200 chars/tick/team`, total match $\le 30\text{ KB}$).
4. **Sandbox Isolation & Timing Budget:** Use `ProcessBuilder` wrapping `bubblewrap` (`bwrap`) or `nsjail` with `--unshare-net`, `--ro-bind`, memory limit `96 MB` RAM/bot, output limit `16 KB/tick`. Tick 0 warmup allows up to `1000ms` waiting for `READY\n`. From Tick 1: base turn budget $T_{\text{base}} = 50\text{ms}$, reserve `Time Bank = 2000ms`. Use **Event-Driven Tick Advancement** (advance immediately upon receiving the `\n` line, do not sleep the full 50ms).
5. **PostgreSQL Queue (Zero Redis):** No Redis. Utilize the `matches` table on Supabase PostgreSQL (connected via Supavisor pooler port `6543` with `?pgbouncer=true&prepareThreshold=0`, Hikari pool size = `2` for Worker) with atomic claim queries using `SELECT ... FOR UPDATE SKIP LOCKED` and a `lease_expires_at` interval (2 minutes).
6. **Supabase 500MB Quota Protection (Zero DB Bloat):** Never store raw logs or replay payloads in PostgreSQL. The columns `validation_log_url`, `compile_log_url`, `log_url`, `artifact_url`, and `replay_data_url` store URLs pointing to compressed artifacts on **Cloudflare R2** (DB retains only `error_summary VARCHAR(255)`).
7. **Git CI Submission Workflow via GitHub Actions:** The Java API does not compile student code directly. The workflow `.github/workflows/battlecode-submit.yml` in the Starter Kit checks file structure, compiles (max `15s`), runs a `10-tick` Smoke Test against `DummyBot`, requests a Presigned PUT URL from the Java API to upload the binary artifact to **Cloudflare R2**, and notifies `/api/webhooks/ci-result`.
8. **Replay Specification (`replay.json.gz`):** Hybrid format combining **Keyframe every 50 Ticks + Delta Frames**. The engine calculates internal physics at 6 decimal digits precision (IEEE 754), but serializes coordinates to `replay.json` rounded to **at most 2 decimal places**.
9. **Frontend Architecture (Next.js App Router on Cloudflare Pages):** Use TanStack Query for server data (CDN cache `s-maxage=5, stale-while-revalidate=10`, invalidated via Supabase Realtime Broadcast) + Zustand for UI state. The browser fetches `replay.json.gz` directly from Cloudflare R2 / CDN (Zero Egress).
10. **Frontend Kinetic Theme & Manifest Compliance (Anti-Boring AI Rule):** When coding or modifying Frontend components (Phase 5 & Phase 6), the Agent **MUST strictly read `docs/ui_theme_manifest.md` and `docs/doc6_ui_design_guide.md`**. The Agent **MUST implement and utilize the unified component suite `frontend/src/components/ui/kinetic-primitives.tsx`** consisting of:
    - `<SpotlightTechCard>` (mouse-following optical gradient spotlight, 3D tilt, chamfered `clip-path`, 4 corner crosshair markers `+`)
    - `<CipherText>` (250–300ms random matrix decipher animation when rendering team names, ELO ratings, and commit hashes)
    - `<AnimatedNumber>` (smooth Odometer Spring rolling numbers with `font-mono tabular-nums`)
    - `<SegmentedBar>` (20-segment gauge skewed at `skew-x-[-18deg]` with pulsating red alert when < 15%)
    - `<LiveCircuitPipeline>` (animated SVG PCB pipeline with moving light beams across the 4 Git CI stages)
    - `<LiveTelemetryTicker>` (live system pulse ribbon, status LEDs, and real-time tournament event marquee).  
    *Strict Prohibition:* AI Agents are strictly forbidden from generating generic, boring AI template layouts (plain `rounded-xl shadow-md bg-white text-gray-800` cards with static text). The interface must feel like a living Aerospace AI Research Lab / Tactical Esports Cockpit (Kinetic Bright Aerospace Lab) that wows computer science students. All colors and chamfer geometries must inherit from CSS variables governed by `docs/ui_theme_manifest.md`.
11. **Canvas Performance & 60fps Guardrails:** `ReplayCanvas` must bind `useRef<HTMLCanvasElement>` and run a `requestAnimationFrame` loop reading from `playbackRef` synchronized with Zustand, **never invoking `useState` on a per-frame basis**. Rendering must adhere to the `createRenderer(canvas): ReplayRenderer` contract with smooth shockwaves, radar sweeps, and particle systems with zero lag.
