# BattleCode-UET Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a remotely accessible 16-team Cyber-Grid tournament with real bot execution, standings and replay.

**Architecture:** Java 21 engine/API on Render, isolated Java match worker on Linux, Supabase Auth/PostgreSQL, R2 artifacts, static Next.js frontend on Cloudflare Pages. Trusted GitHub Actions builds public source at pinned SHAs. External deployment occurs after local verification and account connection.

**Tech Stack:** Java 21, Spring Boot 3.3.4, PostgreSQL, JUnit 5, Next.js static export, TypeScript, Linux `bwrap` plus cgroups.

**Spec:** `docs/superpowers/specs/2026-10-23-battlecode-uet-pilot-design.md`

## Global Constraints

- Backend, worker and engine use Java 21; bot languages are Python, C++20 and Java 21.
- Bot protocol is persistent NDJSON; `READY` warmup is 1,000 ms, turn base 50 ms, bank 2,000 ms, output cap 16,384 bytes.
- Official code runs only in a verified Linux OS sandbox; no student program executes inside the API process.
- Queue uses PostgreSQL atomic claims and bounded leases; no Redis. Database holds metadata, not replay or raw logs.
- Frontend follows `docs/ui_theme_manifest.md` and `docs/doc6_ui_design_guide.md`; game visuals use Canvas without React state per frame.
- Do not claim an online tournament is ready until a public deployment and remote two-user match are observed.

## Review Focus

- Malformed bot JSON becomes a recorded `WAIT`, never a runner crash (Task 1 and 2 tests).
- A stale worker cannot overwrite a newer completed match (Task 3 integration test).
- A frozen tournament cannot select a new submission SHA (Task 3 integration test).
- Hidden enemy cells never leak in observation (Task 1 test).
- Bot process timeout or excess output cannot block another match (Task 2 test).

---

### Task 1: Deterministic Cyber-Grid engine

**Files:** Create `backend/pom.xml`, `backend/src/main/java/edu/uet/battlecode/game/*`, `backend/src/test/java/edu/uet/battlecode/game/CyberGridEngineTest.java`.

**Interfaces:** `CyberGridEngine.init(GameConfig,List<TeamInfo>,long) -> MatchState`; `observe(MatchState,int) -> Observation`; `step(MatchState,Map<Integer,List<BotAction>>) -> TickResult`; `isTerminal`, `results`. Immutable DTOs serializable by Jackson.

- [ ] Write tests for symmetric seeded map, initial state, fog, movement collisions, simultaneous attacks, economy, terminal tie break and invalid action to `WAIT`.
- [ ] Run `mvn -f backend/pom.xml -Dtest=CyberGridEngineTest test`; witness failures caused by missing engine behavior.
- [ ] Implement engine and DTOs with deterministic iteration order and no I/O.
- [ ] Run engine tests, then full `mvn -f backend/pom.xml test`.

### Task 2: Bot protocol, sandbox and replay

**Files:** Create `backend/src/main/java/edu/uet/battlecode/runner/*`, `backend/src/main/java/edu/uet/battlecode/replay/*`, `backend/src/test/java/edu/uet/battlecode/runner/*`, `backend/src/test/java/edu/uet/battlecode/replay/*`.

**Interfaces:** `BotSession.start(BotArtifact,SandboxConfig)`, `request(Observation) -> BotTurnResult`, `close()`; `MatchExecutor.execute(MatchSpec) -> MatchOutcome`; `ReplayWriter` emits version 1 gzip replay with keyframes at 50-tick intervals and reconstructable deltas.

- [ ] Write tests using controlled helper bots for READY, valid turn, malformed JSON, timeout, oversized line, stderr drain and replay reconstruction.
- [ ] Run focused tests and witness expected failures.
- [ ] Implement process I/O and monotonic time bank; create Linux-only sandbox preflight that fails closed when namespaces or cgroup limits are unavailable.
- [ ] Run focused tests and full Maven suite. On an actual worker VM, run isolation probes before enabling official matches.

### Task 3: Tournament persistence and API

**Files:** Create `backend/src/main/resources/db/migration/V1__init.sql`, `backend/src/main/java/edu/uet/battlecode/api/*`, `backend/src/main/java/edu/uet/battlecode/db/*`, `backend/src/test/java/edu/uet/battlecode/api/*`, `backend/src/test/java/edu/uet/battlecode/db/*`.

**Interfaces:** Public read endpoints for tournament, bracket, standings and replay metadata; authenticated team registration/submission; admin freeze/generate/retry/publish; worker claim/heartbeat/complete. API verifies Supabase bearer token and DB role/team ownership.

- [ ] Write tests for anonymous access denial, team ownership, commit immutability, freeze, bracket seeding, queue claim and stale-worker completion.
- [ ] Run focused tests and witness failures.
- [ ] Implement SQL constraints, repositories, services and controllers. Keep DB connection pool small for Supavisor transaction mode.
- [ ] Run focused tests and full Maven suite, including PostgreSQL integration tests when database is available.

### Task 4: Student kits and trusted CI

**Files:** Create `starter-kits/python/*`, `starter-kits/cpp/*`, `starter-kits/java/*`, `.github/workflows/battlecode-submit.yml`, `scripts/smoke-test.*`.

**Interfaces:** Each bot consumes one observation and emits one action line; trusted workflow accepts public repo + exact SHA and language, builds Arm artifact, executes 10-turn smoke test, reports digest/status via a scoped callback.

- [ ] Write smoke tests for all three starter bots and malformed/oversized output.
- [ ] Run smoke tests and witness the expected failure before bot implementations.
- [ ] Implement kits, manifest examples and CI workflow with read-only GitHub permissions; no secret reaches student process.
- [ ] Run 10-turn smoke tests in all available local runtimes, plus GitHub runner once connected.

### Task 5: Frontend and replay cockpit

**Files:** Create `frontend/package.json`, `frontend/src/app/*`, `frontend/src/components/ui/kinetic-primitives.tsx`, `frontend/src/components/replay/*`, `frontend/src/lib/api.ts`, frontend tests.

**Interfaces:** Static route shells fetch the Java API using `NEXT_PUBLIC_API_BASE_URL`; authentication uses Supabase Auth; registration, submission, bracket, standings and replay flows use public provider subdomains.

- [ ] Write component tests for role visibility, submission status, bracket progression and replay seek/fog.
- [ ] Run tests and witness missing behavior.
- [ ] Build accessible responsive views, theme variables and Canvas replay renderer; use polling for live state.
- [ ] Run frontend tests, TypeScript check and static export build.

### Task 6: Deployment and remote acceptance

**Files:** Create `backend/Dockerfile`, `worker/Dockerfile`, `render.yaml`, `frontend/next.config.*`, `docs/deployment.md`, `docs/tournament-runbook.md`.

**Interfaces:** Public Pages URL -> Render API -> Supabase DB/Auth; OCI worker -> DB/R2; GitHub CI -> signed artifact/callback.

- [ ] Add configuration validation tests and deployment smoke probes for health, auth, submission, queue and replay.
- [ ] Run local builds and smoke probes; fix discovered failures.
- [ ] Deploy with connected accounts and secrets, verify health and two separate remote accounts through a complete match; record public URLs and evidence.
- [ ] Run a 16-team rehearsal, capture runtime/quota metrics and mark readiness only when all spec acceptance gates pass.

## Execution note

The user authorized independent implementation and online deployment. Work proceeds inline because subagent delegation was not requested. External account connection, an available OCI VM and real provider secrets are prerequisites for Task 6; missing credentials do not prevent Tasks 1–5. This workspace initially had no Git repository; `git init` succeeded, but the sandbox denies writes to `.git/index.lock`, so commits and worktrees are unavailable here. Preserve progress in source files and verification output.
