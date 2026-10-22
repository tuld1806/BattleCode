# SYSTEM ARCHITECTURE & END-TO-END SPECIFICATION
## Project: BattleCode-UET AI Arena Platform
**Target Audience:** First-Year Computer Science & Software Engineering Students (Gamified Algorithmic Learning)  
**Technology Stack:** Next.js App Router on Cloudflare Pages + Cloudflare CDN; Supabase PostgreSQL, Supabase Auth & Realtime; Java 21 API / Match Worker on Render Free Tier combined with Burst Docker Workers; Cloudflare R2 Storage; GitHub Actions CI.

> **Architecture Convention:** The frontend is a React application built with Next.js App Router, deployed on Cloudflare Pages and accelerated by Cloudflare CDN. All backend logic, webhook processing, queue polling, simulation engines, and match execution run on Java 21. Supabase provides PostgreSQL, Auth, and Realtime Broadcast via Supavisor Connection Pooler. No business domain logic runs in Next.js serverless functions. The match queue uses PostgreSQL rows with `SELECT ... FOR UPDATE SKIP LOCKED` (Zero Redis). Student bot languages are strictly Python, C++, and Java.

---

## 1. SYSTEM OVERVIEW & PEDAGOGICAL GOALS

### 1.1. Design Philosophy for First-Year Students
First-year university students are typically learning fundamental programming syntax (C/C++, Python, Java) and basic data structures (arrays, linked lists) without prior experience in distributed systems. Thus, the system embodies the **"Low Floor, High Ceiling"** principle:
- **Low Floor:** Ready-to-run Starter Kits provided in all 3 languages. Students only need to modify the observation handler and run `git push origin main` to see their bot battle on the Web visualizer.
- **High Ceiling:** The arena accommodates advanced algorithms: Graph Search ($BFS, A^*$), Dynamic Programming, Finite State Machines ($FSM$), Computational Geometry, and Vector Kinematics.
- **Immediate Visual Feedback:** Every algorithmic decision is replayable in 60fps with Fog of War perspective switches and per-tick debug inspection.

### 1.2. Major Subsystems
1. **Web Portal & Replay Viewer (Next.js 14 App Router on Cloudflare Pages):**
   - **Central Lobby (`/`) & Game Hubs (`/[gameSlug]`):** Game rules overview, starter kits, 24/7 practice arena against system bots (`DummyBot`, `EasyBot`, `HardBot`), game-level bot submission, and peer scrimmage matchmaking with scrimmage leaderboard.
   - **Tournament Spaces (`/tournaments` & `/tournaments/[tournamentId]`):** Team registration (`/register`), official tournament bot submission (`/submission`), dynamic bracket tree (`/bracket`), and live tournament leaderboard (`/tournament-leaderboard`).
   - **User Profiles (`/profile` & `/profile/[playerId]`):** Match history, telemetry, bot configuration, and peer scrimmage challenges.
   - **Tactical Simulation Replay Cockpits (`/replay/*`):** Fullscreen canvas 2D/WebGL viewer with Fog of War perspective switching (`GOD VIEW`, `TEAM 1 FOG`, `TEAM 2 FOG`), tick scrubbing, and collapsible `TickInspector` drawer. Dedicated routes for practice/scrimmage (`/replay/[matchId]`) and official tournament Bo3 games (`/replay/[tournamentId]/[matchId]`).
   - **Admin Management Suite (`/admin/*`):** Protected by `AdminGuard` (`role === 'ADMIN'`) for game plugin registration (`/admin/plugins`), JSON-schema game configuration (`/admin/plugins/[gameSlug]/config`), tournament creation (`/tournaments/new`), and tournament mission control & bracket generation (`/tournaments/[tournamentId]/config`).
2. **Core Backend & Data Layer (Render + Supabase):** Java 21 Spring Boot API on Render; Supabase Auth for identity, Supabase PostgreSQL with Supavisor pooler for relational entities and transactional queues.
3. **Submission CI (GitHub Actions):** Workflows validate file structure, compile binaries in $\le 15\text{s}$, execute a 10-tick Smoke Test against `DummyBot`, and upload artifacts to R2 via Presigned URLs.
4. **Hybrid Match Execution:** Render Free Tier operates as the baseline worker for round-the-clock Practice and Scrimmage matches. During tournament spikes, Burst Docker Workers running on lab workstations or private organizer nodes pull jobs from the Supabase PostgreSQL queue.

---

## 2. DEPLOYMENT TOPOLOGY & DATA FLOW

```
+-----------------------------------------------------------------------------------+
|                                 USERS & CODE HOST                                 |
|   [Student Browser]               [Admin Browser]             [GitHub / GitLab]   |
+---------------+-----------------------------+-------------------------+-----------+
                | HTTPS                       | HTTPS                   | Actions / Webhooks
                v                             v                         v
+-----------------------------------------------------------------------------------+
|        CLOUDFLARE PAGES + CLOUDFLARE CDN (React.js + Next.js App Router)          |
|  +-----------------------------------------------------------------------------+  |
|  | React Server Components + Client Components                                |  |
|  | - Web Portal, CDN Cache for Leaderboards (s-maxage=5, SWR=10)               |  |
|  | - Replay Viewer streams replay.json.gz directly from Cloudflare R2          |  |
|  +-----------------------------------------------------------------------------+  |
+-------------------------------+---------------------------------------+-----------+
                                | HTTPS Java API                         | R2 Direct
                                v                                       v
+-----------------------------------------------------------------------------------+
|                       SUPABASE + RENDER (Data & API Layer)                        |
|                                                                                   |
|  +------------------------------+         +------------------------------------+  |
|  | Supabase PostgreSQL          |         | Supabase Auth + Supavisor          |  |
|  | - Relational entities        |         | - Identity / Connection Pooler     |  |
|  | - SELECT ... SKIP LOCKED     |         | Supabase Realtime Broadcast        |  |
|  +---------------+--------------+         +------------------------------------+  |
|                  |                                                              |
|  +---------------v------------------------------------------------------------+  |
|  | Render Free Web Service: Java 21 API + Scrimmage Worker                    |  |
|  | Webhooks, Presigned URLs, Match Coordination; no in-process student builds |  |
|  +----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
                 ^                                      |
                 | claim/complete jobs                  | presigned upload
                 |                                      v
+----------------------------------+       +---------------------------------------+
| Burst Docker Workers (Lab/Nodes) |       | Cloudflare R2                         |
| Java 21 + ProcessBuilder + bwrap |       | Binaries, build logs, replay.json.gz  |
| High-throughput tournament runs  |       | Zero-Egress CDN downloads             |
+----------------------------------+       +---------------------------------------+
```

---

## 3. DATABASE SCHEMA SPECIFICATION (SUPABASE POSTGRESQL)

Conforms to Free Tier constraints (500MB limit). Zero long logs or raw replay JSON in PostgreSQL; tables store R2 URLs and `error_summary VARCHAR(255)`.

### 3.1. Users & Teams
- **`users`**:
  - `id` (UUID, PK references `auth.users.id`)
  - `student_id` (VARCHAR, Unique)
  - `email` (VARCHAR, Unique)
  - `full_name` (VARCHAR)
  - `role` (ENUM: `STUDENT`, `TEACHING_ASSISTANT`, `ADMIN`)
  - `git_username` (VARCHAR)
- **`teams`**:
  - `id` (UUID, PK)
  - `name` (VARCHAR, Unique)
  - `tournament_id` (UUID, FK $\rightarrow$ `tournaments.id`)
  - `leader_id` (UUID, FK $\rightarrow$ `users.id`)
  - `git_repo_url` (VARCHAR)
  - `git_webhook_secret` (VARCHAR)
  - `elo_rating` (DOUBLE PRECISION, Default: 1200.0)
- **`team_members`**:
  - `team_id` (UUID, FK $\rightarrow$ `teams.id`)
  - `user_id` (UUID, FK $\rightarrow$ `users.id`)
  - `role` (VARCHAR, Default: 'MEMBER')
  - `joined_at` (TIMESTAMPTZ)
  - Primary Key: `(team_id, user_id)`

### 3.2. Plug-ins & Game Configurations
- **`game_plugins`**:
  - `id` (UUID, PK)
  - `slug` (VARCHAR, Unique: `cyber-grid`, `stellar-armada`)
  - `name` (VARCHAR)
  - `version` (VARCHAR)
  - `manifest_json` (JSONB)
  - `engine_bundle_url` (VARCHAR)
  - `visualizer_bundle_url` (VARCHAR)
  - `is_active` (BOOLEAN)
- **`game_configs`**:
  - `id` (UUID, PK)
  - `plugin_id` (UUID, FK $\rightarrow$ `game_plugins.id`)
  - `config_name` (VARCHAR)
  - `parameters_json` (JSONB)
  - `created_by` (UUID, FK $\rightarrow$ `users.id`)

### 3.3. Tournaments & Rounds
- **`tournaments`**:
  - `id` (UUID, PK)
  - `title` (VARCHAR)
  - `plugin_id` (UUID, FK $\rightarrow$ `game_plugins.id`)
  - `default_config_id` (UUID, FK $\rightarrow$ `game_configs.id`)
  - `status` (ENUM: `DRAFT`, `REGISTRATION_OPEN`, `CODING_PHASE`, `SUBMISSION_FROZEN`, `RUNNING_MATCHES`, `COMPLETED`)
  - `registration_start` / `registration_end` (TIMESTAMPTZ)
  - `submission_start` / `submission_deadline` (TIMESTAMPTZ)
  - `allow_scrimmage` (BOOLEAN)
- **`tournament_rounds`**:
  - `id` (UUID, PK)
  - `tournament_id` (UUID, FK $\rightarrow$ `tournaments.id`)
  - `round_name` (VARCHAR)
  - `game_config_id` (UUID, FK $\rightarrow$ `game_configs.id`)
  - `scheduled_run_time` (TIMESTAMPTZ)
  - `results_published` (BOOLEAN, Default: false)

### 3.4. Submissions, Matches & Participants
- **`submissions`**:
  - `id` (UUID, PK)
  - `user_id` (UUID, FK $\rightarrow$ `users.id`, student author)
  - `team_id` (UUID, FK $\rightarrow$ `teams.id`, Nullable for game-level personal practice)
  - `plugin_id` (UUID, FK $\rightarrow$ `game_plugins.id`)
  - `tournament_id` (UUID, FK $\rightarrow$ `tournaments.id`, Nullable for game-level practice/scrimmage)
  - `commit_hash` (VARCHAR(40))
  - `commit_message` (TEXT)
  - `branch` (VARCHAR)
  - `language` (ENUM: `python`, `cpp`, `java`)
  - `validation_status` (ENUM: `PENDING`, `VALIDATING`, `PASSED`, `FAILED_STRUCTURE`, `FAILED_SYNTAX`, `FAILED_SMOKE_TEST`)
  - `validation_log_url` (VARCHAR)
  - `build_status` (ENUM: `PENDING`, `COMPILING`, `SUCCESS`, `COMPILE_ERROR`)
  - `compile_log_url` (VARCHAR)
  - `source_bundle_url` (VARCHAR)
  - `artifact_url` (VARCHAR)
  - `is_active` (BOOLEAN, Default: false — marks default active bot for 24/7 Practice & Scrimmage at `/[gameSlug]/submission`)
  - `is_active_for_tournament` (BOOLEAN, Default: false — marks official active bot for tournament rounds at `/tournaments/[tournamentId]/submission`)
  - `submitted_at` (TIMESTAMPTZ)
- **`matches`**:
  - `id` (UUID, PK)
  - `match_type` (ENUM: `PRACTICE`, `SCRIMMAGE`, `TOURNAMENT`)
  - `tournament_id` (UUID, FK $\rightarrow$ `tournaments.id`, Nullable for practice & scrimmage)
  - `round_id` (UUID, FK $\rightarrow$ `tournament_rounds.id`, Nullable for practice & scrimmage)
  - `game_config_id` (UUID, FK $\rightarrow$ `game_configs.id`)
  - `random_seed` (BIGINT)
  - `status` (ENUM: `QUEUED`, `RUNNING`, `COMPLETED`, `SYSTEM_ERROR`)
  - `worker_id` (VARCHAR)
  - `lease_expires_at` (TIMESTAMPTZ)
  - `attempt_count` (INT, Default: 0)
  - `total_ticks` (INT)
  - `replay_data_url` (VARCHAR — streamed directly by `/replay/[matchId]` or `/replay/[tournamentId]/[matchId]`)
  - `created_at` / `started_at` / `completed_at` (TIMESTAMPTZ)
- **`match_participants`**:
  - `match_id` (UUID, FK $\rightarrow$ `matches.id`)
  - `team_id` (UUID, FK $\rightarrow$ `teams.id`, Nullable for practice bots)
  - `submission_id` (UUID, FK $\rightarrow$ `submissions.id`)
  - `slot_index` (INT)
  - `final_score` (DOUBLE PRECISION)
  - `result` (ENUM: `WIN`, `LOSS`, `DRAW`, `DISQUALIFIED_TIMEOUT`, `DISQUALIFIED_CRASH`, `DISQUALIFIED_SECURITY`)
  - `error_summary` (VARCHAR(255))
  - `log_url` (VARCHAR)

### 3.5. Atomic Queue Claim Template
```sql
WITH next_match AS (
    SELECT id
    FROM matches
    WHERE status = 'QUEUED'
       OR (status = 'RUNNING' AND lease_expires_at < NOW())
    ORDER BY created_at
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE matches AS m
SET status = 'RUNNING',
    worker_id = :worker_id,
    lease_expires_at = NOW() + INTERVAL '2 minutes',
    attempt_count = attempt_count + 1,
    started_at = COALESCE(started_at, NOW())
FROM next_match
WHERE m.id = next_match.id
RETURNING m.*;
```

---

## 4. DUAL GIT SUBMISSION WORKFLOW & CI PIPELINE

The platform supports two distinct submission scopes:
1. **Game-Level Submission (`/[gameSlug]/submission`):**
   - Enables students to link a personal or practice Git repository.
   - Pushes automatically trigger validation and smoke tests against `DummyBot`.
   - On passing, students can toggle `is_active = true` to immediately field this bot in 24/7 Practice matches (`/[gameSlug]/practice`) and peer Scrimmage games (`/[gameSlug]/scrimmage`).
2. **Tournament-Level Submission (`/tournaments/[tournamentId]/submission`):**
   - Linked to a registered student team (`/tournaments/[tournamentId]/register`).
   - Teams select a specific validated commit hash as their official entry (`is_active_for_tournament = true`).
   - Subject to tournament stage gating: submissions are locked when the admin triggers `SUBMISSION_FROZEN` at `/tournaments/[tournamentId]/config`.

### CI Execution Flow:
1. Student team pushes code to their GitHub repository: `git push origin main`.
2. Webhook triggers Java API `/api/webhooks/git`. API validates webhook signature, rate limits, and submission window status.
3. GitHub Actions builds the bot in an isolated runner container:
   - **Static & Manifest Validation:** Checks `bot.json`, file size $\le 1024\text{KB}$, and scans for forbidden identifiers (`eval`, `exec`, `subprocess`, `socket`, `fork`, `java.net`).
   - **Compilation ($\le 15\text{s}$):** Compiles via `py_compile` (Python), `g++ -O3 -std=c++20` (C++), or `javac` (Java).
   - **Smoke Test:** Runs a 10-tick match against `DummyBot` verifying `READY\n` handshake and valid NDJSON responses.
4. Validated binary / class artifact is uploaded to Cloudflare R2 using a Presigned PUT URL.
5. GitHub Actions calls `/api/webhooks/ci-result` to mark the submission `PASSED`, unlocking instant practice battles and live replay viewing.