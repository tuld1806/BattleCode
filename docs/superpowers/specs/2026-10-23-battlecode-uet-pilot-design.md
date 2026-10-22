# BattleCode-UET pilot design

## Purpose and scope

Deliver an Internet-accessible coding tournament for 16 UET teams at zero service spend. Students register, submit a public GitHub repository and immutable commit SHA, practice, compete in Cyber-Grid, view results and replays. Admins open registration, freeze a tournament, seed a best-of-three single-elimination bracket, retry infrastructure errors and publish results. Python, C++20 and Java 21 bots use the same persistent NDJSON process protocol. The first release includes one game; the plug-in boundary permits Stellar Armada later.

The project starts from documentation only. An online launch requires accounts for Render, Cloudflare, Supabase, R2 and a Linux worker VM. Service availability and quota cannot be guaranteed by code alone. The API and frontend are useful online without the worker, but the deployment is not tournament-ready until a real Linux sandbox and a remote two-user match have passed acceptance tests.

## Architecture

- `backend/`: Java 21 Spring Boot API and pure Java deterministic game engine. Render Free hosts the HTTP API. It never executes student source or binaries.
- `worker/`: Java 21 process on Oracle Always Free Arm Linux. It claims match rows using PostgreSQL `FOR UPDATE SKIP LOCKED`, renews leases, downloads immutable artifacts, runs two bots in separate `bwrap` sandboxes with cgroup quotas, writes replay to R2 and finalizes the match idempotently. Initially one match at a time.
- `frontend/`: Next.js App Router with a static export to Cloudflare Pages. Because arbitrary dynamic routes cannot be generated ahead of time, the pilot uses static route shells with query parameters for tournament and match IDs. It fetches API data client-side. All colors and UI primitives follow `ui_theme_manifest.md` and `doc6_ui_design_guide.md`.
- Supabase Auth supplies identity; the Java API verifies access tokens against Supabase Auth and enforces role and team ownership in every mutation. Supabase PostgreSQL stores only relational metadata. R2 Standard stores compiled bot artifacts and compressed replays.
- GitHub Actions performs trusted validation, Arm-compatible compilation and smoke testing. The platform repository owns the workflow; student repositories provide source at the submitted SHA. Official matches never run in Actions. The API verifies the CI callback's scoped one-use token and artifact checksum.

## Cyber-Grid rules for first release

Use a symmetric 16x16 map with an obstacle pattern and resource nodes derived only from the published seed. Each team starts with a core, one worker and 100 data. Maximum 200 ticks in pilot mode; production config may extend up to the documented 2,000 cap. Units, costs, HP, attack values, vision and four resolution phases match `doc4_games_cybergrid_stellar.md`. No more than 60 mobile units per team.

The observation line contains `tick`, `seed`, `team`, `size`, `resources`, `visible_cells`, `friendly_units`, and `visible_enemies`. Each action line is a JSON object with `actions`, an array of `{unit_id,type,direction,target_id,unit_type}` records. Types are `WAIT`, `MOVE`, `ATTACK`, `SPAWN`, `BUILD`; fields not relevant to the selected type are absent. One action per existing unit; missing or malformed actions become `WAIT` and are recorded. Illegal actions cost the unit's turn. Visibility uses Manhattan distance, unioned across friendly units; unseen cells and enemy entities never appear in the observation. Replay keeps omniscient state, with per-team visibility masks for viewer perspective.

Moves into blocked cells incur 10 damage. Simultaneous competing moves to the same cell, occupied cell or swap bounce all involved movers and incur 10 damage. Combat damage is accumulated before removal. Economy applies after combat; every living core yields 10 data, workers on resource nodes yield 5. Core destruction ends the match. If both cores die in one phase or the tick cap is reached, rank by surviving core HP, then total unit HP, then resources; exact ties draw. Every map is generated symmetrically and match result is deterministic for a given pair of bot artifacts, game version, config and seed.

## Bot protocol and sandbox

Bot writes `READY\n` within 1,000 ms and thereafter one JSON line per observation. Bot processes persist throughout the match. Runner drains stdout/stderr concurrently, enforces 16,384 bytes stdout per turn, 200 stderr chars per team per tick and 30 KB per match. Base turn time is 50 ms with 2,000 ms initial reserve, measured by a monotonic clock; overspend reduces reserve. A timeout/crash disqualifies the responsible bot. If both fail in one tick, draw with both disqualified.

Linux worker requires successful sandbox preflight before claiming any official match: unshared network, read-only runtime and bot mounts, private writable tmpfs, no inherited secret environment, cgroup memory/CPU/pid limits, process tree termination. `bwrap` handles namespaces and mounts; cgroups enforce resource limits. The 96 MB per-bot target from the docs is tested on all three runtimes, particularly Java, on the actual Arm host. If it is not viable, official matches remain disabled until the constraint is resolved transparently.

## Data and tournament workflow

Tables: profiles (role), tournaments (status/config/deadlines), teams and team_members, submissions (public URL, SHA, language, validation state, artifact key/digest), rounds, series, matches, participants. Database constraints prevent duplicate membership, duplicate active official submissions and conflicting slot assignment. Replays/logs stay outside PostgreSQL. Match lease claims use row locking; completion checks worker ID and current lease to reject stale results. System errors can be requeued within a bounded retry count. A submitted SHA never changes; a newer submission is a new row.

Flow: admin opens registration; students authenticate, join/create teams and submit SHAs; trusted CI validates and reports; students may practice against DummyBot with a validated artifact; admin freezes submissions and generates a seeded bracket; each series contains two mirrored games and an optional third tiebreak game; worker drains matches; API publishes series and round results only after every required game has a final result. Public standings and replays refresh via bounded HTTP polling, avoiding Supabase Free's 200 Realtime connection cap.

## Replay and UI

`replay.json.gz` has version, match metadata, initial map, keyframe every 50 ticks and delta records in between. Reconstructing any tick from the preceding keyframe must equal engine state. Positions are integral for Cyber-Grid. Viewer supports pause, speed, scrub, team fog perspective and a per-tick action/observation inspector. Public pages: lobby, game rules/starter kits, tournament, registration, submission, bracket, standings, replay. Admin pages: tournament status/freeze/generate/retry/publish. UI controls are role gated in the browser for clarity and enforced again by the API.

## Acceptance gates

1. Deterministic Java engine tests cover map symmetry, movement collision, combat simultaneity, economy, fog, tie-breaks, malformed actions and replay reconstruction.
2. Starter bots in Python, C++ and Java pass a 10-tick smoke test and a complete match. Infinite loop, oversized output, memory/network/file abuse are stopped on the actual Linux host.
3. API integration tests prove unauthorized mutation denial, team ownership, immutable SHA freeze, queue claim/retry/idempotent completion and bracket progression.
4. Two remote accounts on separate networks register, submit, see the same official match and replay through public URLs. Admin can recover a worker failure without duplicate scoring.
5. A 16-team rehearsal completes within an organizer-approved event window and stays below current free quotas. The rehearsal, not documentation estimates, determines whether 32 teams are admitted.

## Deployment constraints

Render Free can sleep and lose local files; no state or artifact lives there. Cloudflare Pages serves static output; no domain purchase is required if provider subdomains are accepted. Supabase Free project can pause after inactivity, so organizers verify readiness shortly before an event. R2 requires a subscription and can charge above its free tier; the organizer must monitor usage and cap replay size/retention. OCI Always Free VM may be unavailable in the chosen region or reclaimed when idle. No source changes or credential substitutions can guarantee a public deployment without these accounts.
