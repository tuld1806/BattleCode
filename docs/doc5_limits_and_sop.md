# OPERATIONAL LIMITS, RESOURCE QUOTAS & TOURNAMENT SOP
## Standard Operating Procedures for Instructors, TAs & System Administrators

**Project:** BattleCode-UET AI Arena Platform  
**Target Scale:** 1,000 – 2,000 student teams (3,000 – 6,000 undergraduate participants)  
**Infrastructure Budget:** 100% Free-Tier Architecture (Cloudflare Pages & R2 + Supabase + Render Free + GitHub Actions + Hybrid Burst Workers).

---

## PART 1: FREE-TIER QUOTAS & SYSTEM SAFEGUARDS

To operate a tournament at this scale with $0 cloud cost, workload is distributed across 5 zero-cost cloud tiers.

### 1.1. Service Quota & Safeguard Matrix
| Platform & Service | Free-Tier Hard Limits | Estimated Monthly Usage (2,000 Teams) | Protection Mechanism & Admin Guidelines |
| :--- | :--- | :--- | :--- |
| **Cloudflare Pages & CDN** *(Web Portal & Replay)* | - Bandwidth: **Unlimited**<br>- Builds: 500/month<br>- Workers: 100,000 req/day | - Bandwidth: $\approx 1.5 - 3\text{ TB}$<br>- Pageviews: $\approx 2,000,000$ | - Edge Cache (`s-maxage=5, stale-while-revalidate=10`) enabled for Leaderboards (`/[gameSlug]/scrimmage-leaderboard`, `/tournaments/[tournamentId]/tournament-leaderboard`) and Brackets (`/tournaments/[tournamentId]/bracket`).<br>- Static assets (`renderer.js`, `replay.json.gz`) stream directly from CDN. |
| **Cloudflare R2** *(Object Storage)* | - Storage: **10 GB/month**<br>- Class A (Write): 1,000,000 ops<br>- Class B (Read): 10,000,000 ops<br>- Egress: **$0 (Zero Egress)** | - Peak Storage: $\approx 6.5\text{ GB}$<br>- Writes: $\approx 150,000$ ops<br>- Reads: $\approx 2,500,000$ ops | - Artifacts capped at $\le 500\text{ KB}$, replays $\le 80\text{ KB}$.<br>- Old bot artifacts pruned upon new push.<br>- Scrimmage and practice replays (`/replay/[matchId]`) auto-deleted after 48 hours via R2 Lifecycle rules.<br>- Official tournament replays (`/replay/[tournamentId]/[matchId]`) preserved throughout the tournament season. |
| **Supabase** *(PostgreSQL, Auth & Realtime)* | - Database Size: **500 MB**<br>- Egress: 5 GB/month<br>- Auth: 50,000 MAU<br>- Realtime: 200 concurrent connections | - Database Size: $\approx 180 - 250\text{ MB}$<br>- Auth: $\approx 6,000$ students | - **Zero DB Bloat:** Logs & replays stored in R2. Relational DB holds only URLs and `error_summary VARCHAR(255)`.<br>- All Worker queries route through **Supavisor Pooler** (port 6543, Transaction mode). |
| **Render** *(Free Web Service)* | - Hours: **750 hours/month**<br>- RAM: **512 MB**<br>- CPU: 0.1 vCPU<br>- Bandwidth: 100 GB/month | - 720 hours (1 persistent instance 24/7)<br>- RAM Usage: $350 - 440\text{ MB}$ | - Maintain exactly **1 Web Service instance** per account.<br>- Cloudflare Worker cron-pings `/api/health` every 10 min during active hours (07:00 - 23:00) to mitigate spin-down delays. |
| **GitHub Actions** *(CI Build & Smoke Test)* | - Public Repos: **Unlimited**<br>- Private Repos: 2,000 min/month<br>- GitHub Campus: 3,000 min/month | - $\approx 25 - 40\text{s}$ per push ($\approx 15$ pushes/team/season) | - Use GitHub Classroom (Education tier) or public repositories secured with `git_webhook_secret` signatures. |

### 1.2. Render Free Tier 512MB RAM Budgeting
$$\text{Total Memory} = \text{OS Overhead } (40\text{MB}) + \text{Java API/Runner } (180\text{MB}) + 2 \times \text{Bot Sandbox } (96\text{MB}) = 412\text{MB} < 512\text{MB}$$
- **Java Core API & Match Runner:** Launched with JVM flags `-Xms64m -Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=48m`.
- **Bot Sandboxes:** Capped at `96 MB` RAM via cgroups / `bwrap`.
- **Concurrency on Render Free:** Locked to **1 concurrent match** (`WORKER_CONCURRENCY=1`).

---

## PART 2: STUDENT BOT CONSTRAINTS & `GameConfig` RULES

### 2.1. Git Submissions & Build Phase Limits
1. **Source Package Size:** Max $1,024\text{ KB}$ ($1\text{ MB}$). Exclude binaries, neural network weight files, `.venv`, and `node_modules`.
2. **Allowed Extensions:** `.py`, `.cpp`, `.h`, `.hpp`, `.java`, `.json`, `.md`, `.yml`.
3. **Compilation Timeout:** Max $15\text{ seconds}$.
4. **Smoke Test Gate:** 10-tick match against `DummyBot` emitting `READY\n` at tick 0 and valid NDJSON without crashes or unhandled exceptions.

### 2.2. Match Sandbox Parameters
- **RAM Quota:** $96\text{ MB}$ per bot.
- **Tick 0 Warmup:** $1,000\text{ ms}$.
- **Turn Budget ($T_{\text{base}}$):** $50\text{ ms}$ per tick (event-driven advancement).
- **Time Bank:** $2,000\text{ ms}$ per match reserve.
- **Max Output:** $16,384\text{ Bytes}$ ($16\text{ KB}$) per tick.
- **Debug Logs:** Max $200\text{ characters}$ per tick per team; match total capped at $30\text{ KB}$.

### 2.3. `GameConfig` Rules for Admins
1. **Max Ticks:** Never set `max_ticks > 2000`. Recommended: Cyber-Grid 1000 ticks, Stellar-Armada 1200 ticks.
2. **Entity Cap:** Maintain $\le 60$ mobile units per team to prevent replay frame bloating.
3. **Coordinate Rounding:** All floating-point positions in replay files must be rounded to $\le 2$ decimal places.

---

## PART 3: TOURNAMENT STANDARD OPERATING PROCEDURES (SOP)

### 3.1. Swiss-System Tournament Execution (2,000 Teams)
- 7 rounds, Best-of-3 (Bo3) Mirror Map: $1,000\text{ matchups/round} \times 3 = 3,000\text{ matches/round}$.
- **Burst Workers Deployment:** Connect 10–20 lab workstations running `docker-compose.burst.yml`. Each workstation runs 4 concurrent workers, clearing 3,000 matches in under 30 minutes.

### 3.2. Step-by-Step Round Operations (Admin Control Panel: `/tournaments/[tournamentId]/config`)
1. **T - 30m (Announce Cutoff & Queue Drain):**
   - Access `/tournaments/[tournamentId]/config`.
   - Click `Lưu Cấu hình Thời gian & Bật/Tắt Giao hữu` to set `allow_scrimmage = false`, preventing casual matches from competing with official round queues.
2. **T - 0m (Freeze Submissions):**
   - At `/tournaments/[tournamentId]/config`, click **"Đóng băng nộp bài (Freeze Submissions)"**.
   - System updates tournament state to `SUBMISSION_FROZEN`. Subsequent git pushes to `/tournaments/[tournamentId]/submission` are rejected from official play.
   - *(Optional Emergency Extension)*: Click **"Gia hạn nộp bài (+30m Extend Deadline)"** if technical incidents warrant additional time.
3. **Match Generation & Seeding:**
   - Click **"Thêm Vòng đấu & Gán GameConfig cho Vòng"** to create a new `tournament_rounds` record.
   - Click **"Khởi tạo Nhánh đấu (Generate Round Matchups - Swiss / Double Elimination / Round-Robin)"**.
   - System seeds teams, generates Bo3 mirror map pairings, and enqueues 3,000 records into `matches` with `status = 'QUEUED'`.
4. **Monitoring & Fault Recovery:**
   - Monitor worker throughput and queue drain telemetry.
   - If transient node failures cause execution errors, click **"Chạy lại các trận lỗi (Re-run SYSTEM_ERROR Matches)"** to safely re-enqueue failed matches without restarting completed games.
5. **Publish Results & Reveal Brackets:**
   - Click **"Công bố Kết quả & Nhánh đấu (Publish Round Results)"**.
   - Backend sets `results_published = true` and emits a Supabase Realtime Broadcast.
   - Redirects to `/tournaments/[tournamentId]/bracket`, where student clients automatically refresh brackets and unlock match replays (`/replay/[tournamentId]/[matchId]`).