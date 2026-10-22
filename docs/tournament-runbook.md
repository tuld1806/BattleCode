# BattleCode-UET Tournament Operations Runbook

A step-by-step SOP for organizers running an official 16-team Cyber-Grid tournament.

---

## 1. Pre-Event Checklist (T-2 Hours)

- [ ] Verify Supabase project is active (not paused due to inactivity).
- [ ] Verify Render Web Service responds 200 OK on `/api/public/tournaments`.
- [ ] Start the Linux match worker daemon and verify `preflight` passed cleanly in worker logs.
- [ ] Confirm R2 bucket has valid read/write credentials and CORS enabled.

---

## 2. Tournament Lifecycle Operations

### Phase A: Registration Open
1. Create new tournament in Admin Cockpit (`/admin`) or through API:
   ```bash
   POST /api/admin/tournaments
   {"title": "UET Autumn Grand Arena 2026", "seed": 133742}
   ```
2. Direct 16 student teams to `/tournament` to register their team names.
3. Students push bot code to GitHub and submit commit SHAs via `/submit`.

### Phase B: Submission Freeze
1. When deadline expires, click **FREEZE REGISTRATION** in `/admin`.
2. All team submissions are locked into immutable commit SHAs.

### Phase C: Bracket Generation & Execution
1. Click **GENERATE BRACKET & QUEUE MATCHES** in `/admin`.
2. The API creates 15 series across 4 rounds:
   - Round 1: 8 series (16 teams)
   - Round 2: 4 series (8 teams)
   - Round 3: 2 series (4 teams)
   - Round 4: 1 series (Grand Finals)
3. The Linux match worker automatically claims matches via PostgreSQL skip locked queries.
4. Spectators can follow live bracket advancement on `/bracket`.

### Phase D: Incident Recovery (Stalled Matches)
If a match shows `SYSTEM_ERROR` due to temporary network glitch or worker restart:
1. Locate the match ID from the database or `/admin`.
2. Enter the Match UUID into **3. Retry Match on System Error** in `/admin` and click **RETRY**.
3. The match status resets to `QUEUED` without duplicating scores or corrupting the bracket tree.

### Phase E: Publication & Replay Release
1. When Grand Finals complete, click **PUBLISH STANDINGS & REPLAYS** in `/admin`.
2. Official standings lock into final rankings.
3. Match replay download links become public on `/standings` and `/replay`.
