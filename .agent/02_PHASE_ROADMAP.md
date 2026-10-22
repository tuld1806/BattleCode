# BattleCode-UET Phase Roadmap & Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Follow the Zero-Interruption Protocol defined in `.agent/00_CONSTITUTION.md` and execute the handoff in `.agent/03_HANDOFF_PROTOCOL.md` at each phase boundary.

**Goal:** Deliver the complete student-facing web cockpit, real-time 60fps Canvas replay visualizer, production containerization, and cloud deployment for the 16-team BattleCode-UET tournament.

**Architecture:** Next.js 14 App Router static export on Cloudflare Pages, consuming the Java 21 Spring Boot API on Render Free. Replays rendered directly on HTML5 Canvas via `requestAnimationFrame` with zero per-frame React state overhead. Multi-stage Docker containers for backend and Linux worker daemon (`bwrap` + cgroups).

**Tech Stack:** Next.js 14, TypeScript, Tailwind CSS, TanStack Query, Zustand, HTML5 Canvas 2D, Docker, Render Free, Cloudflare Pages, Supabase Auth/PostgreSQL, Cloudflare R2.

**Specs & Design Guides:**
- `docs/superpowers/specs/2026-10-23-battlecode-uet-pilot-design.md`
- `docs/ui_theme_manifest.md`
- `docs/doc6_ui_design_guide.md`
- `.agent/00_CONSTITUTION.md`

---

## Global Constraints

- **Theme Compliance:** Strict adherence to `docs/ui_theme_manifest.md` (Active Preset: `AERO_LAB_BRIGHT`, chamfer polygon clip-paths, zero generic AI layouts).
- **Frontend Architecture:** Static export (`output: 'export'`) compatible with Cloudflare Pages. Routes use static shells with search params (`?id=...`, `?match=...`) for dynamic entities.
- **Canvas 60fps Rule:** Canvas rendering strictly uses `useRef<HTMLCanvasElement>` and `requestAnimationFrame` reading from mutable playback refs synchronized with Zustand. No React `useState` calls on tick updates.
- **Container Memory Quota:** Backend container tuned for Render Free 512MB RAM (`-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=48m`).
- **Worker Isolation:** Worker container requires Linux namespaces (`bwrap`) with cgroups memory limits (`96 MB/bot`). Official matches are blocked unless preflight probe passes.
- **Verification Gate:** No phase is complete without clean terminal verification (`npm run build` with 0 type errors, `mvn test` 100% pass).

## Review Focus

1. **Replay Scrubbing Glitch:** Rapid scrubbing backwards across keyframes must cleanly reconstruct intermediate deltas without ghost units.
2. **Fog of War Entity Leak:** In team perspective mode, unseen enemy units or hidden grid terrain must never be painted on the canvas.
3. **CI Webhook Race:** Multiple concurrent submission callbacks must not corrupt job status or create duplicate active entries.
4. **Token Expiry on Static Pages:** Expired Supabase session must trigger graceful re-authentication without infinite redirect loops.
5. **Worker Stale Result Rejection:** A slow worker that finishes after lease expiration must have its completion rejected atomically.

---

## PHASE 5: FRONTEND COCKPIT & REPLAY VISUALIZER

### Task 5.1: Next.js Project Scaffolding & Theme Engine Setup
**Files:**
- Create: `frontend/package.json`, `frontend/tsconfig.json`, `frontend/next.config.mjs`, `frontend/tailwind.config.ts`, `frontend/postcss.config.mjs`
- Create: `frontend/src/app/globals.css`, `frontend/src/app/layout.tsx`, `frontend/src/app/page.tsx`
- Test: `frontend/src/__tests__/theme.test.ts`

**Interfaces:**
- Consumes: CSS variables and geometry defined in `docs/ui_theme_manifest.md`.
- Produces: Base layout with Dot Matrix animated background, Aero Lab Bright palette variables (`--bg-canvas`, `--bg-panel`, `--accent-primary`, `--card-clip-path`).

- [ ] **Step 1: Write test verifying CSS variables and theme preset tokens**
- [ ] **Step 2: Run test (`npx vitest run`) and witness failure**
- [ ] **Step 3: Scaffold Next.js app with Tailwind, configure `globals.css` with Aero Lab Bright preset**
- [ ] **Step 4: Run test and build verification (`npm run typecheck`)**
- [ ] **Step 5: Commit changes**

---

### Task 5.2: Kinetic UI Primitives Component Suite
**Files:**
- Create: `frontend/src/components/ui/kinetic-primitives.tsx`
- Create: `frontend/src/lib/utils.ts`
- Test: `frontend/src/__tests__/kinetic-primitives.test.tsx`

**Interfaces:**
- Produces:
  - `<SpotlightTechCard>`: Chamfered card with mouse radial spotlight and corner crosshairs `+`.
  - `<CipherText>`: Rapid random-character decipher animation (250-300ms) for team names and hashes.
  - `<AnimatedNumber>`: Odometer spring rolling number with `font-mono tabular-nums`.
  - `<SegmentedBar>`: 20-segment gauge skewed at `-18deg` with low-capacity alert.
  - `<LiveCircuitPipeline>`: SVG PCB circuit pipeline with light beam particles.
  - `<LiveTelemetryTicker>`: Real-time tournament telemetry ticker ribbon.

- [ ] **Step 1: Write unit tests for kinetic primitives (rendering, props, chamfer classes)**
- [ ] **Step 2: Run test and witness failure**
- [ ] **Step 3: Implement all 6 primitives in `kinetic-primitives.tsx` according to `docs/doc6_ui_design_guide.md`**
- [ ] **Step 4: Run component tests to verify 100% pass**
- [ ] **Step 5: Commit changes**

---

### Task 5.3: API Client, Auth State & Core Tournament Pages
**Files:**
- Create: `frontend/src/lib/api.ts`, `frontend/src/lib/auth.ts`, `frontend/src/store/useAuthStore.ts`
- Create: `frontend/src/app/rules/page.tsx`
- Create: `frontend/src/app/tournament/page.tsx`
- Create: `frontend/src/app/submit/page.tsx`
- Test: `frontend/src/__tests__/tournament-views.test.tsx`

**Interfaces:**
- Consumes: Backend REST endpoints (`/api/public/tournaments`, `/api/tournaments/{id}/teams`, `/api/tournaments/{id}/teams/{teamId}/submissions`).
- Produces: Tournament lobby, game rules handbook, team registration modal, and submission pipeline view with `<LiveCircuitPipeline>`.

- [ ] **Step 1: Write tests for API client mapping and submission form validation**
- [ ] **Step 2: Run test and witness failure**
- [ ] **Step 3: Implement API client with caching, Supabase auth integration, and pages**
- [ ] **Step 4: Run test and verify clean client-side build**
- [ ] **Step 5: Commit changes**

---

### Task 5.4: Interactive Bracket & Standings Dashboard
**Files:**
- Create: `frontend/src/app/bracket/page.tsx`
- Create: `frontend/src/app/standings/page.tsx`
- Create: `frontend/src/components/bracket/BracketTree.tsx`
- Test: `frontend/src/__tests__/bracket.test.tsx`

**Interfaces:**
- Consumes: `/api/public/tournaments/{id}/bracket` and `/api/public/tournaments/{id}/standings`.
- Produces: 16-team single-elimination interactive bracket tree with live series status and standings table.

- [ ] **Step 1: Write bracket tree rendering tests (round progression, winning path highlight)**
- [ ] **Step 2: Run test and witness failure**
- [ ] **Step 3: Implement `BracketTree` with SVG connection lines and `<SpotlightTechCard>` nodes**
- [ ] **Step 4: Run test and verify pass**
- [ ] **Step 5: Commit changes**

---

### Task 5.5: 60fps Canvas Replay Engine & Tactical Theater
**Files:**
- Create: `frontend/src/components/replay/ReplayCanvas.tsx`
- Create: `frontend/src/components/replay/ReplayRenderer.ts`
- Create: `frontend/src/components/replay/ReplayControls.tsx`
- Create: `frontend/src/store/useReplayStore.ts`
- Create: `frontend/src/app/replay/page.tsx`
- Test: `frontend/src/__tests__/replay-engine.test.ts`

**Interfaces:**
- Consumes: Decompressed `replay.json` containing keyframes and deltas.
- Produces: 60fps smooth Canvas renderer with radar sweeps, shockwaves, fog of war toggle (Omniscient / Team 1 / Team 2), scrub bar, and tick inspector.

- [ ] **Step 1: Write replay delta reconstruction and perspective masking tests**
- [ ] **Step 2: Run test and witness failure**
- [ ] **Step 3: Implement Canvas 2D engine with `requestAnimationFrame` loop and zero per-frame React state**
- [ ] **Step 4: Run tests and verify 60fps frame loop behavior**
- [ ] **Step 5: Commit changes**

---

### Task 5.6: Admin Operations Cockpit & Static Export Build
**Files:**
- Create: `frontend/src/app/admin/page.tsx`
- Create: `frontend/src/components/admin/TournamentControls.tsx`
- Modify: `frontend/next.config.mjs` (ensure `output: 'export'`)
- Test: Verification build script

**Interfaces:**
- Consumes: `/api/admin/tournaments/*` endpoints.
- Produces: Tournament freeze, bracket generation, match retry, and result publish controls.

- [ ] **Step 1: Write test for admin guard and tournament lifecycle actions**
- [ ] **Step 2: Run test and witness failure**
- [ ] **Step 3: Implement admin cockpit and configure static export**
- [ ] **Step 4: Execute `npm run build` in `frontend/` and verify `out/` static directory is generated with 0 errors**
- [ ] **Step 5: Commit changes and execute Phase 5 Handoff**

---

## PHASE 6: CONTAINERIZATION, CLOUD DEPLOYMENT & 16-TEAM REHEARSAL

### Task 6.1: Multi-Stage Production Dockerfiles
**Files:**
- Create: `backend/Dockerfile`
- Create: `worker/Dockerfile`
- Create: `.dockerignore`
- Test: `scripts/test_docker_builds.sh` or local docker build command

**Interfaces:**
- `backend/Dockerfile`: Java 21 Alpine image, non-root user, memory flags `-Xms64m -Xmx160m -XX:+UseSerialGC`.
- `worker/Dockerfile`: Linux Ubuntu image with `bubblewrap`, `cgroup-tools`, `python3`, `g++`, `openjdk-21-jdk`.

- [ ] **Step 1: Write Dockerfile specifications with security hardening and non-root execution**
- [ ] **Step 2: Validate syntax and build arguments**
- [ ] **Step 3: Implement both Dockerfiles and verify minimal layer footprint**
- [ ] **Step 4: Commit changes**

---

### Task 6.2: Cloud Infrastructure Configurations & Runbooks
**Files:**
- Create: `render.yaml`
- Create: `docs/deployment.md`
- Create: `docs/tournament-runbook.md`

**Interfaces:**
- Defines: Render Web Service specs, Cloudflare Pages routing, Supabase pooler connections, and Cloudflare R2 bucket policies.

- [ ] **Step 1: Draft infrastructure manifest `render.yaml` matching free-tier constraints**
- [ ] **Step 2: Write deployment and incident recovery runbook**
- [ ] **Step 3: Commit changes**

---

### Task 6.3: 16-Team Tournament Simulation Rehearsal
**Files:**
- Create: `scripts/simulate_tournament_e2e.py`
- Test: End-to-end rehearsal execution

**Interfaces:**
- Simulates 16 student teams submitting Python, C++, and Java bots, runs full bracket progression, generates replays, and verifies zero database errors.

- [ ] **Step 1: Write simulation rehearsal script**
- [ ] **Step 2: Run simulation in local test environment and record execution metrics**
- [ ] **Step 3: Verify all 15 series and match replays are correctly generated**
- [ ] **Step 4: Final project verification and handoff**
