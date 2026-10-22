# BATTLECODE-UET SYSTEM STATE & CAPABILITY AUDIT
**Last Updated:** 2026-10-23T02:58:00+07:00  
**Repository Branch:** `master` (Commit `c692060`)  
**Active Phase:** `PHASE 5: FRONTEND COCKPIT & REPLAY VISUALIZER`  
**Automated Tests Passing:** 35 / 35 (100% Pass, 0 Failures, 0 Errors)

---

## 1. Executive Summary & Progress Audit ("Hệ thống đã làm đến đâu?")

The backend core, deterministic simulation engine, bot runtime sandbox, replay pipeline, database persistence layer, and student starter kits are **fully implemented and verified by 35 automated tests**. The project is currently transitioning from backend infrastructure to **Phase 5 (Next.js Frontend & Canvas Replay Theater)**.

### Subsystem Completion Matrix

| Subsystem / Module | Status | Verified Deliverables | Tests |
| :--- | :--- | :--- | :--- |
| **Cyber-Grid Simulation Engine** (`backend/game`) | **COMPLETE** | 16x16 symmetric procedural map generator, Fog of War Manhattan union, simultaneous combat resolution, collision bounce with damage, economy (core + worker mining), 200-tick cap, deterministic tie-breakers. | 11 / 11 PASS |
| **Bot Protocol & Sandboxing** (`backend/runner`) | **COMPLETE** | Persistent process runner over `stdin/stdout` using NDJSON, 1000ms `READY` warmup, 50ms turn budget + 2000ms monotonic time bank, 16KB stdout limit, 200-char stderr drain, Linux `bwrap` fail-closed preflight. | 6 / 6 PASS |
| **Match Execution Engine** (`backend/runner`) | **COMPLETE** | `MatchExecutor` orchestrates 2-bot matches, enforces tick progression, records ticks and scores, generates `MatchOutcome`. | 2 / 2 PASS |
| **Replay Compression & Serialization** (`backend/replay`) | **COMPLETE** | Hybrid keyframe (every 50 ticks) + delta compression, coordinate rounding to 2 decimals, GZIP compressed format `replay.json.gz`, deterministic reconstruction. | 1 / 1 PASS |
| **Database Schema & Migrations** (`backend/resources/db`) | **COMPLETE** | Flyway `V1__init.sql` schema: 10 tables (`profiles`, `tournaments`, `teams`, `team_members`, `submissions`, `ci_jobs`, `tournament_entries`, `rounds`, `series`, `matches`), immutable commit SHA trigger, atomic match claims via `SELECT ... FOR UPDATE SKIP LOCKED`. | DB Tests PASS |
| **Backend REST API** (`backend/api`, `backend/db`) | **COMPLETE** | Supabase JWT AuthContext, `PublicController` (tournaments, standings, bracket, replay query), `TeamController` (registration, join, submit), `AdminController` (freeze, generate bracket, retry, publish), `WorkerController` (match claim, heartbeat, spec, complete, fail), `CiController` (presigned upload, CI webhook). | 9 / 9 PASS |
| **Student Starter Kits** (`starter-kits/`) | **COMPLETE** | Python 3 (`bot.py`), C++20 (`main.cpp`), Java 21 (`Bot.java`), metadata manifests (`bot.json`, `bot_meta.json`), validation script (`validate_submission.py`), smoke test runner (`smoke_test.py`). | 3 / 3 PASS |
| **Worker Match Runner Daemon** (`worker/`) | **COMPLETE** | Standalone Java daemon `WorkerMain` for Linux, lease heartbeat thread (30s), R2 artifact download & extraction (`ArtifactExtractor`), replay upload to R2 (`WorkerStorage`), atomic match finalization. | 2 / 2 PASS |
| **Frontend Cockpit & Replay** (`frontend/`) | **NOT STARTED** | Next.js 14 App Router, Kinetic UI Primitives, 60fps Canvas Replay Renderer, static export for Cloudflare Pages. | Pending Phase 5 |
| **Production Cloud Deployment** (`ops/`) | **NOT STARTED** | Multi-stage Dockerfiles (`backend`, `worker`), `render.yaml`, Cloudflare Pages config, deployment rehearsal. | Pending Phase 6 |

---

## 2. Established Contracts & Architectural Invariants

### A. Engine & Bot Protocol Contract
- **Protocol:** Persistent process communicating via single-line NDJSON over `stdin/stdout`.
- **Warmup:** Wait up to `1000ms` for `READY\n`.
- **Tick Budget:** Base $T_{\text{base}} = 50\text{ms}$, reserve time bank = $2000\text{ms}$. Exceeding time bank triggers disqualification.
- **Safety Limits:** Max `16,384 bytes` per stdout line. Max `200 chars/tick` on stderr (max `30 KB` per match). Malformed actions fallback safely to `WAIT` without engine crash.

### B. Database & Concurrency Contract
- **Queueing:** PostgreSQL-backed atomic match claim using:
  ```sql
  SELECT * FROM matches 
  WHERE status = 'QUEUED' OR (status = 'RUNNING' AND lease_expires_at < CURRENT_TIMESTAMP)
  ORDER BY created_at ASC 
  FOR UPDATE SKIP LOCKED LIMIT 1;
  ```
- **Connection Efficiency:** Hikari pool size strictly set to `2` for backend and worker to accommodate Supabase/Supavisor Free tier (port `6543`, `?pgbouncer=true&prepareThreshold=0`).
- **Zero DB Bloat:** No blobs or replays in PostgreSQL. Artifacts and replays stored on Cloudflare R2; DB stores only URL pointers and `error_summary VARCHAR(255)`.

### C. Replay Serialization Contract
- **Format:** `replay.json.gz` (v1).
- **Structure:** `Keyframe` stored at tick 0 and every 50 ticks; delta frames (`TickDelta`) stored in between.
- **Precision:** Internal physics calculated in IEEE 754 doubles; output coordinates rounded to at most 2 decimal places.

### D. Security & Identity Contract
- **Authentication:** Supabase JWT verified via JWKS endpoint.
- **Role Enforcement:** `profiles.role` ('STUDENT' or 'ADMIN'). Student mutations restricted to owned teams.
- **Worker Auth:** Constant-time `MessageDigest.isEqual` comparison on `X-Worker-Key` (minimum 32 characters).
- **CI Webhook:** Scoped one-time token hash with expiration in `ci_jobs`.

---

## 3. Current Test Evidence

Full project build run on 2026-10-23:
```bash
mvn test
```
**Results:**
- `edu.uet:battlecode-backend`: 33 tests, 0 failures, 0 errors, 0 skipped (Time: ~12.4s)
- `edu.uet:battlecode-worker`: 2 tests, 0 failures, 0 errors, 0 skipped (Time: ~2.0s)
- `edu.uet:battlecode-parent`: Reactor Build SUCCESS (Total Time: ~14.8s)

---

## 4. Immediate Next Step

Proceed to **Phase 5: Frontend Cockpit & Replay Visualizer** as outlined in [02_PHASE_ROADMAP.md](file:///c:/Users/admin/Downloads/BattleCode/.agent/02_PHASE_ROADMAP.md).
