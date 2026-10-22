# DOCUMENT 6: DYNAMIC UI/UX SPECIFICATION (KINETIC BRIGHT AEROSPACE LAB) & AI AGENT GUIDELINES
**File:** `docs/doc6_ui_design_guide.md`  
**Project:** BattleCode-UET AI Arena Platform  
**Target Audience:** Computer Science & Software Engineering Students passionate about algorithms, AI, and competitive Esports  
**Primary Aesthetic:** **Kinetic Bright Aerospace Lab** (Bright, High-Energy Aerospace AI Research Lab – Optical, Interactive, Kinetic, and Deeply Digital)  
**Frontend Stack:** React.js (Next.js 14+ App Router) on Cloudflare Pages, Tailwind CSS, Framer Motion (or high-performance CSS Keyframes), Lucide Icons, TanStack Query, Zustand, HTML5 Canvas 2D/WebGL.

---

## 1. MARKDOWN-DRIVEN THEME ENGINE (`docs/ui_theme_manifest.md`)

The user interface of **BattleCode-UET** is engineered as a **Markdown-Driven Live Theme Engine**:
1. **Golden Rule for AI Agents:** Before generating or editing any frontend code (especially during Phase 5 & Phase 6), the AI Agent **MUST** read `docs/ui_theme_manifest.md`, `docs/thiet_ke_giao_dien.md`, and this guide `docs/doc6_ui_design_guide.md`.
2. **Dynamic Configuration:** The Agent must parse key settings from `docs/ui_theme_manifest.md`:
   - `ACTIVE_PRESET` (Color palette: `AERO_LAB_BRIGHT`, `CYBER_TERMINAL_HYBRID`, `QUANTUM_HOLO_MINT`).
   - `MOTION_INTENSITY` (Motion level: `HIGH`, `MEDIUM`, `LOW`, `OFF`).
   - `CARD_CORNER_STYLE` (Card geometry: `CHAMFER_45`, `SHARP_TECH`, `ROUNDED_GLASS`).
   - Feature flags: `ENABLE_TEXT_SCRAMBLE`, `ENABLE_SVG_PULSE_LINES`, `ENABLE_SPOTLIGHT_HOVER`, `ENABLE_RADAR_SWEEP`.
3. **Use CSS Variables:** All colors, chamfer angles (`clip-path`), and animation timing curves in `app/globals.css` and `tailwind.config.ts` must rely on **CSS Variables** (`var(--bg-canvas)`, `var(--bg-panel)`, `var(--bg-arena)`, `var(--accent-primary)`, `var(--team-1)`, `var(--team-2)`, `var(--card-clip-path)`).
4. **Strict Prohibition of Generic AI UI:** Strictly avoid generic, sterile office templates (such as plain `rounded-xl shadow-md bg-white text-gray-800`). Modifying `docs/ui_theme_manifest.md` must immediately re-skin the entire application.

---

## 2. DESIGN PHILOSOPHY: "KINETIC BRIGHT AEROSPACE LAB"

CS students dislike static, bureaucratic admin forms. The **BattleCode-UET** interface is designed like a **Live Satellite Ground Station / Tactical Aerospace AI Simulation Cockpit**:
- **Bright, Optical & Sharp:** An icy laboratory white backdrop `#F4F7FB` accented with technical coordinate grids, creating high-contrast vibrance with team Neon glows.
- **Living Data (Kinetic Telemetry):** Every metric, status flag, and data line breathes, pulses, and provides optical tactile feedback on interaction.
- **Academic Rigor Meets Competitive Esports:** Merges the atmosphere of a quantum computational laboratory with a high-stakes real-time esports arena.
- **Role-Based Clarity (RBAC):** Clean segregation between student activities (`STUDENT`) and administrative governance (`ADMIN`).

---

## 3. THE 6 MANDATORY KINETIC PRIMITIVES (`kinetic-primitives.tsx`)

All views across the portal must utilize standardized kinetic UI components defined in `frontend/src/components/ui/kinetic-primitives.tsx`:

### 3.1. `<CipherText text="..." durationMs={300} />` (Matrix Text Scramble / Cipher Decode)
- **Description:** When a page loads or when `elo_rating`, team names, or `commit_hash` update, the text cycles through random alphanumeric and hex characters (`01X#9A` $\rightarrow$ `UET_WARRIORS`) over 250–300ms, evoking a quantum computer decrypting data frames.
- **Mechanism:** Implemented via `requestAnimationFrame` or `setInterval` iterating through random glyphs `0123456789ABCDEF_#$<>*`, resolving left-to-right to the target string. Automatically bypassed if `ENABLE_TEXT_SCRAMBLE: false`.

### 3.2. `<AnimatedNumber value={...} precision={1} />` (Smooth Odometer Spring Counter)
- **Description:** All numerical telemetry (`elo_rating`, `score`, `resources`, active queue count, `time_bank_ms`, `cpu_ms`) roll smoothly using Spring/Odometer physics transitions rather than abruptly snapping between values.
- **Typography:** Strictly paired with digital monospace typography `font-mono tabular-nums`.

### 3.3. `<SpotlightTechCard />` (Interactive Spotlight & Chamfered Card)
- **Description:** Hovering over Top 3 Podium Cards, team profile cards, or telemetry metric tiles triggers:
  - Technical 45-degree chamfered edges via `clip-path` (governed by `CARD_CORNER_STYLE = CHAMFER_45`).
  - Four crosshair reticle markers (`+`) positioned at the corners.
  - Optical cursor tracking (`mouseX`, `mouseY`) driving an electric radial glow (`radial-gradient` following cursor coordinates) across the frosted glass surface `bg-white/88 backdrop-blur-md`, accentuating the chamfered border.
  - Interactive cards with clickable buttons keep `enableTilt: false` to avoid mouse misalignment during click events.

### 3.4. `<SegmentedBar value={...} max={...} segments={20} />` (Segmented Energy & Time Gauge)
- **Description:** Visualizes Health Points (HP), Time Budget (`time_bank_ms` default 2000ms), and Harvested Resources (Data/Ore).
- **Geometry:** Avoids solid continuous bars; divided into 20 skewed parallelogram segments `skew-x-[-18deg]`.
- **Emergency Alert:** When remaining capacity drops below `< 15%` (or `time_bank_ms < 300ms`), all active segments illuminate in vivid danger red `#EF4444` and pulse with high-frequency strobe animation.

### 3.5. `<LiveCircuitPipeline steps={[...]} activeIndex={...} />` (Animated SVG Data Beams)
- **Description:** Powers the Git Submission History pages (`/[gameSlug]/submission` and `/tournaments/[tournamentId]/submission`) and tournament stage monitors.
- **Mechanism:** Connects the 4 CI stages (`GIT PUSH` $\rightarrow$ `MANIFEST CHECK` $\rightarrow$ `BUILD 15s` $\rightarrow$ `SMOKE TEST 10 TICKS`) with printed-circuit-board (PCB) SVG pathways. While GitHub Actions executes, moving light particles (animated SVG data beams using `stroke-dasharray` offsets) flow along the trace lines in real time toward the active stage.

### 3.6. `<LiveTelemetryTicker />` (Live System Telemetry & Pulse Ribbon)
- **Description:** Positioned directly below the primary navigation bar.
- **Elements:**
  - Pulsing emerald LED indicator (`SYS.ONLINE | TICK_RATE: 20HZ | SANDBOX: BWRAP_96MB`).
  - Mini heartbeat waveform canvas.
  - Live broadcast marquee relaying system and tournament-wide events in real time (e.g., `[LIVE] Team Alpha defeated Team Omega in Bo3 (+18.4 ELO)`, `[CI] CyberKnight passed Smoke Test`).

---

## 4. TECHNICAL COLOR PALETTE & SCIENTIFIC TYPOGRAPHY

### 4.1. Default Theme Palette (`AERO_LAB_BRIGHT` - Crisp Aerospace AI Laboratory)
| CSS Variable | Default HEX / RGBA | Role & Application Context |
| :--- | :--- | :--- |
| `--bg-canvas` | `#F4F7FB` | Lab ice white base, overlaid with subtle coordinate dots `#CBD5E1` (20px) and technical grid lines (100px). |
| `--bg-panel` | `rgba(255, 255, 255, 0.88)` | Frosted optical glass surface (`backdrop-blur-md`) with metallic border `#CBD5E1`. |
| `--bg-arena` | `#0B192C` | Deep Blueprint Navy background for `ReplayCanvas`, maximizing contrast for laser beams and team neon glows. |
| `--text-main` | `#0F172A` | Deep space obsidian text providing maximum legibility against bright backgrounds. |
| `--text-muted` | `#64748B` | Technical slate for labels, metadata timestamps, and secondary coordinates. |
| `--border-color` | `#CBD5E1` | Polished metallic borders with corner chamfers. |
| `--accent-primary` | `#0284C7` $\rightarrow$ `#2563EB` | Electric Sky / Cobalt for primary action buttons, active borders, and radar sweep beams. |
| `--team-1` (Neon) | `#00F5D4` | Brilliant Neon Aqua rendered on dark match arenas. |
| `--team-1-ui` (Contrast) | `#00A896` | High-contrast Deep Cyan for text, badges, and borders on bright HUD panels. |
| `--team-2` (Neon) | `#FF2A85` | Brilliant Neon Rose rendered on dark match arenas. |
| `--team-2-ui` (Contrast) | `#E11D48` | High-contrast Deep Rose for text, badges, and borders on bright HUD panels. |
| `--status-passed` | `#10B981` | Emerald green (`PASSED` / `WIN` / `STABLE`). |
| `--status-running` | `#F59E0B` | Electric amber (`RUNNING` / `COMPILING` / `IN_QUEUE`). |
| `--status-error` | `#EF4444` | Coral red alert (`TIMEOUT` / `CRASH` / `DISQUALIFIED`). |

### 4.2. Mandatory Typography Triad
1. **Headings & HUD Labels (`Space Grotesk` or `Orbitron`):** `uppercase`, `tracking-wider`, `font-bold` for a futuristic cockpit aesthetic.
2. **Body & Documentation (`Plus Jakarta Sans` or `Inter`):** Clean, modern, highly legible sans-serif for descriptions and guidance.
3. **Algorithmic Telemetry, Code & Coordinates (`JetBrains Mono` with `tabular-nums` - MANDATORY):** Strictly applied to `elo_rating`, `commit_hash`, `random_seed`, `tick`, `resources`, `score`, `cpu_ms`, `time_bank_ms`, `(x, y)` coordinates, and `debug_logs`.

---

## 5. REFINED ROUTE TREE & SCREEN ARCHITECTURE

Synchronized with `docs/thiet_ke_giao_dien.md`, the platform navigation is structured into five distinct operational domains:

```text
/ (Trang chủ / Sảnh chờ trung tâm)
│
├── /[gameSlug] (Hub của từng Game: /cyber-grid | /stellar-armada)
│   ├── /[gameSlug]/overview                ──► /[gameSlug]/practice (Đấu với Bot hệ thống 24/7)
│   │                                             └──► /replay/[matchId]
│   ├── /[gameSlug]/submission              (Nộp bài cấp độ Game để đấu Bot & Giao hữu)
│   ├── /[gameSlug]/scrimmage               (Đấu giao hữu người chơi trong Game)
│   │                                         └──► /replay/[matchId]
│   ├── /[gameSlug]/scrimmage-leaderboard   (BXH người chơi đấu giao hữu của Game)
│   │                                         └──► /profile/[playerId]
│   └── /[gameSlug]/tournaments             (Danh sách giải đấu đang diễn ra của Game này)
│                                             └──► /tournaments/[tournamentId]
│
├── /tournaments (Danh sách giải đấu của tất cả các Game)
│   └── /tournaments/[tournamentId] (Sảnh chính của 1 Giải đấu)
│       ├── /tournaments/[tournamentId]/register                (Đăng ký đội thi)
│       ├── /tournaments/[tournamentId]/submission              (Nơi nộp bài của sinh viên trong Giải)
│       ├── /tournaments/[tournamentId]/bracket                 (Cây nhánh đấu hiện tại)
│       │                                                         └──► /replay/[tournamentId]/[matchId]
│       └── /tournaments/[tournamentId]/tournament-leaderboard  (BXH các đội trong Giải)
│                                                                 └──► /replay/[tournamentId]/[matchId]
│
├── /profile (Hồ sơ của tôi & Lịch sử trận đấu)
│   ├── /profile/[playerId]                 (Hồ sơ người chơi khác)
│   └── /replay/[matchId]                   (Xem Replay trận đấu từ lịch sử hồ sơ)
│
└── /admin (Trung tâm Quản trị — Chỉ dành cho ADMIN)
    ├── /admin/plugins                      (Quản lý & Thêm Game Plug-in mới)
    │   └── /admin/plugins/[gameSlug]/config (Config thông số luật Game từ JSON Schema)
    ├── /tournaments/new                    (Tạo giải đấu mới — Chỉ ADMIN)
    └── /tournaments/[tournamentId]/config  (Config giải đấu & Khởi tạo cây nhánh đấu — Chỉ ADMIN)
```

---

## 6. CATALOG OF 18 CORE INTERFACES & RBAC MATRIX

| STT | Cụm phân hệ | Tên Giao diện | Route chuẩn | Quyền truy cập |
| :--: | :--- | :--- | :--- | :--- |
| $1$ | **Sảnh Chung** | Trang chủ / Sảnh chờ | `/` | Tất cả (`STUDENT`, `ADMIN`) |
| $2$ | **Phân hệ Game** | Điều hướng Sảnh Game | `/[gameSlug]` | `STUDENT`, `ADMIN` (Xem) |
| $3$ | **Phân hệ Game** | Tổng quan Luật & Starter Kit Game (cho phép tải) | `/[gameSlug]/overview` | `STUDENT`, `ADMIN` (Xem) |
| $4$ | **Phân hệ Game** | Đấu tập với Bot Hệ thống ($24/7$) | `/[gameSlug]/practice` | Chỉ `STUDENT` |
| $5$ | **Phân hệ Game** | Nộp bài Luyện tập & Giao hữu | `/[gameSlug]/submission` | Chỉ `STUDENT` |
| $6$ | **Phân hệ Game** | Sảnh Đấu Giao hữu (Scrimmage) | `/[gameSlug]/scrimmage` | Chỉ `STUDENT` |
| $7$ | **Phân hệ Game** | BXH Đấu Giao hữu của Game | `/[gameSlug]/scrimmage-leaderboard` | `STUDENT`, `ADMIN` (Xem) |
| $8$ | **Phân hệ Game** | Danh sách Giải đấu của riêng Game | `/tournaments?game=[gameSlug]` | `STUDENT`, `ADMIN` |
| $9$ | **Phân hệ Giải đấu** | Danh sách Giải đấu Toàn hệ thống | `/tournaments` | `STUDENT`, `ADMIN` |
| $10$ | **Phân hệ Giải đấu** | Sảnh Chính Giải đấu | `/tournaments/[tournamentId]` | `STUDENT`, `ADMIN` |
| $11$ | **Phân hệ Giải đấu** | Đăng ký Đội thi vào Giải | `/tournaments/[tournamentId]/register` | Chỉ `STUDENT` |
| $12$ | **Phân hệ Giải đấu** | Nơi Nộp bài trong Giải đấu | `/tournaments/[tournamentId]/submission` | Chỉ `STUDENT` |
| $13$ | **Phân hệ Giải đấu** | Cây Nhánh đấu Hiện tại | `/tournaments/[tournamentId]/bracket` | `STUDENT`, `ADMIN` |
| $14$ | **Phân hệ Giải đấu** | BXH các Đội trong Giải đấu | `/tournaments/[tournamentId]/tournament-leaderboard` | `STUDENT`, `ADMIN` |
| $15$ | **Hồ sơ & Replay** | Hồ sơ Người chơi & Lịch sử đấu | `/profile` & `/profile/[playerId]` | `STUDENT`, `ADMIN` |
| $16$ | **Hồ sơ & Replay** | Buồng lái Phát lại Trận đấu (Replay) | `/replay/[matchId]` & `/replay/[tournamentId]/[matchId]` | `STUDENT`, `ADMIN` |
| $17$ | **Phân hệ Admin** | Quản trị Tổng & Quản lý Plug-in Game | `/admin` & `/admin/plugins` & `/admin/plugins/[gameSlug]/config` | Chỉ `ADMIN` |
| $18$ | **Phân hệ Admin** | Tạo Giải đấu, Config Giải & Khởi tạo Nhánh đấu | `/tournaments/new` & `/tournaments/[tournamentId]/config` | Chỉ `ADMIN` |

---

## 7. DETAILED INTERACTION BUTTONS & NAVIGATION SPECS

### 7.1. Nhánh Gốc & Hồ Sơ Cá Nhân (`/`, `/profile`, `/profile/[playerId]`)

| Giao diện (Route hiện tại) | Nút thao tác trên giao diện | Ai thấy nút này? | Route đích / Hành động |
| :--- | :--- | :--- | :--- |
| `/` *(Trang chủ / Sảnh chờ)* | `Chọn Game: Cyber-Grid` | Tất cả | `/cyber-grid` *(tự chuyển tiếp vào `/cyber-grid/overview`)* |
| | `Chọn Game: Stellar Armada` | Tất cả | `/stellar-armada` *(tự chuyển tiếp vào `/stellar-armada/overview`)* |
| | `Đấu tập ngay với Bot (Cyber-Grid)` | `STUDENT` | `/cyber-grid/practice` |
| | `Đấu tập ngay với Bot (Stellar Armada)` | `STUDENT` | `/stellar-armada/practice` |
| | `Danh sách Tất cả Giải đấu` | Tất cả | `/tournaments` |
| | `Hồ sơ của tôi` | `STUDENT` | `/profile` |
| | `Admin Room (Bảng điều khiển Admin)` | `ADMIN` | `/admin` |
| `/profile` *(Hồ sơ của tôi)* | `Xem Replay trận đã đấu` *(Bấm vào 1 trận trong lịch sử)* | `STUDENT` | `/replay/[matchId]` *(hoặc `/replay/[tournamentId]/[matchId]`)* |
| | `Quản lý Code Bot Cyber-Grid` | `STUDENT` | `/cyber-grid/submission` |
| | `Quản lý Code Bot Stellar Armada` | `STUDENT` | `/stellar-armada/submission` |
| | `Về Trang chủ` | Tất cả | `/` |
| `/profile/[playerId]` *(Hồ sơ người chơi khác)* | `Thách đấu Giao hữu với người chơi này` | `STUDENT` | `/[gameSlug]/scrimmage?opponent=[playerId]` |
| | `Xem Replay trận công khai của người chơi` | Tất cả | `/replay/[matchId]` |
| | `Quay lại BXH Giao hữu` | Tất cả | `/[gameSlug]/scrimmage-leaderboard` |

---

### 7.2. Nhánh Trung Tâm Từng Game (`/[gameSlug]/*`)
> Áp dụng đồng bộ cho cả 2 game: `[gameSlug] = cyber-grid` và `[gameSlug] = stellar-armada`.

| Giao diện (Route hiện tại) | Nút thao tác trên giao diện | Ai thấy nút này? | Route đích / Hành động |
| :--- | :--- | :--- | :--- |
| `/[gameSlug]` *(Khung điều hướng Game)* | `Tab: Tổng quan Game (Overview)` | Tất cả | `/[gameSlug]/overview` |
| | `Tab: Đấu với Bot (Practice)` | `STUDENT` | `/[gameSlug]/practice` |
| | `Tab: Nộp Code Bot (Submission)` | `STUDENT` | `/[gameSlug]/submission` |
| | `Tab: Đấu Giao hữu (Scrimmage)` | `STUDENT` | `/[gameSlug]/scrimmage` |
| | `Tab: BXH Giao hữu (Scrimmage Leaderboard)` | Tất cả | `/[gameSlug]/scrimmage-leaderboard` |
| | `Tab: Giải đấu của Game (Tournaments)` | Tất cả | `/[gameSlug]/tournaments` |
| | `Nút: Config Luật Game này` | `ADMIN` | `/admin/plugins/[gameSlug]/config` |
| `/[gameSlug]/overview` *(Tổng quan Luật & Tài liệu)* | `Bắt đầu Đấu tập với Bot ngay` | `STUDENT` | `/[gameSlug]/practice` |
| | `Tải Starter Kit (Python / C++ / Java)` | `STUDENT` | Tải gói zip hoặc mở hướng dẫn clone Git repo |
| | `Nộp Bot cho Game này` | `STUDENT` | `/[gameSlug]/submission` |
| | `Xem Danh sách Giải đấu của Game` | Tất cả | `/[gameSlug]/tournaments` |
| `/[gameSlug]/practice` *(Đấu với Bot hệ thống 24/7)* | `Đấu với DummyBot (Khởi động)` | `STUDENT` | Tạo trận & chuyển sang `/replay/[matchId]` |
| | `Đấu với EasyBot (Cơ bản)` | `STUDENT` | Tạo trận & chuyển sang `/replay/[matchId]` |
| | `Đấu với HardBot (Nâng cao)` | `STUDENT` | Tạo trận & chuyển sang `/replay/[matchId]` |
| | `Đổi bản nộp Bot mang đi đấu` | `STUDENT` | `/[gameSlug]/submission` |
| | `Quay lại Tổng quan Game` | `STUDENT` | `/[gameSlug]/overview` |
| `/[gameSlug]/submission` *(Nộp bài cấp Game)* | `Liên kết Git Repo & Lấy Webhook` | `STUDENT` | Mở Modal cấu hình Git tại `/[gameSlug]/submission` |
| | `Đặt làm Bot Mặc định cho Giao hữu & Đấu Bot` | `STUDENT` | Cập nhật cờ `is_active = true` tại `/[gameSlug]/submission` |
| | `Xem Build & Smoke Test Log (10 Ticks)` | `STUDENT` | Mở ngăn kéo `BuildLogPanel` tại `/[gameSlug]/submission` |
| | `Mang Bot này đi Đấu thử với Bot hệ thống` | `STUDENT` | `/[gameSlug]/practice` |
| `/[gameSlug]/scrimmage` *(Đấu giao hữu người chơi)* | `Chọn Đối thủ & Bắt đầu Trận Giao hữu` | `STUDENT` | Tạo trận Scrimmage & chuyển sang `/replay/[matchId]` |
| | `Ghép trận Ngẫu nhiên theo mức Elo` | `STUDENT` | Tạo trận Scrimmage & chuyển sang `/replay/[matchId]` |
| | `Xem Replay các trận giao hữu vừa đấu` | `STUDENT` | `/replay/[matchId]` |
| | `Xem BXH Đấu Giao hữu của Game` | Tất cả | `/[gameSlug]/scrimmage-leaderboard` |
| `/[gameSlug]/scrimmage-leaderboard` *(BXH Giao hữu của Game)* | `Bấm vào Tên Người chơi trên BXH` | Tất cả | `/profile/[playerId]` |
| | `Thách đấu nhanh người chơi này` | `STUDENT` | `/[gameSlug]/scrimmage?opponent=[playerId]` |
| | `Xem Replay trận gần nhất của người chơi` | Tất cả | `/replay/[matchId]` |
| `/[gameSlug]/tournaments` *(DS Giải đấu của riêng Game)* | `Chọn 1 Giải đấu (Vào Đấu Trường)` | Tất cả | `/tournaments/[tournamentId]` |
| | `Đăng ký tham gia giải` *(Khi mở đăng ký)* | `STUDENT` | `/tournaments/[tournamentId]/register` |
| | `Config Giải đấu này` | `ADMIN` | `/tournaments/[tournamentId]/config` |

---

### 7.3. Nhánh Không Gian Giải Đấu (`/tournaments/*`)

| Giao diện (Route hiện tại) | Nút thao tác trên giao diện | Ai thấy nút này? | Route đích / Hành động |
| :--- | :--- | :--- | :--- |
| `/tournaments` *(Danh sách giải của tất cả Game)* | `Bộ lọc theo Game (Tất cả \| Cyber-Grid \| Stellar Armada)` | Tất cả | `/tournaments?game=[gameSlug]` |
| | `Vào Đấu Trường (Xem chi tiết giải)` | Tất cả | `/tournaments/[tournamentId]` |
| | `Tạo Giải đấu mới` | `ADMIN` | `/tournaments/new` |
| `/tournaments/[tournamentId]` *(Sảnh chính Giải đấu)* | `Đăng ký Tham gia Giải đấu` *(Khi `REGISTRATION_OPEN`)* | `STUDENT` | `/tournaments/[tournamentId]/register` |
| | `Nơi nộp bài của Sinh viên (Git Submission)` | `STUDENT` | `/tournaments/[tournamentId]/submission` |
| | `Cây nhánh đấu hiện tại (Bracket)` | Tất cả | `/tournaments/[tournamentId]/bracket` |
| | `Bảng xếp hạng các Đội (Tournament Leaderboard)` | Tất cả | `/tournaments/[tournamentId]/tournament-leaderboard` |
| | `Config Giải đấu & Khởi tạo Nhánh đấu` | `ADMIN` | `/tournaments/[tournamentId]/config` |
| `/tournaments/[tournamentId]/register` *(Đăng ký Đội thi)* | `Tạo Đội mới & Liên kết Kho Git` | `STUDENT` | Lưu đội thi & chuyển sang `/tournaments/[tournamentId]/submission` |
| | `Tham gia Đội bằng Mã mời (Invite Code)` | `STUDENT` | Lưu thành viên & chuyển sang `/tournaments/[tournamentId]` |
| | `Quay lại Sảnh Giải đấu` | `STUDENT` | `/tournaments/[tournamentId]` |
| `/tournaments/[tournamentId]/submission` *(Nơi nộp bài Giải đấu)* | `Chọn Bản Commit làm Bản thi đấu chính thức` (`is_active_for_tournament`) | `STUDENT` | Cập nhật trạng thái tại `/tournaments/[tournamentId]/submission` |
| | `Xem Log Kiểm định 4 bước (BuildLogPanel)` | `STUDENT` | Mở Terminal Log tại `/tournaments/[tournamentId]/submission` |
| | `Quay lại Sảnh Giải đấu` | `STUDENT` | `/tournaments/[tournamentId]` |
| `/tournaments/[tournamentId]/bracket` *(Cây nhánh đấu hiện tại)* | `Xem Replay Ván đấu (Game 1 / 2 / 3 trong cặp Bo3)` | Tất cả | `/replay/[tournamentId]/[matchId]` |
| | `Đổi Vòng đấu hiển thị trên Nhánh` | Tất cả | `/tournaments/[tournamentId]/bracket?round=[roundId]` |
| | `Chuyển sang Bảng xếp hạng các Đội` | Tất cả | `/tournaments/[tournamentId]/tournament-leaderboard` |
| | `Điều phối / Khởi tạo Nhánh đấu` | `ADMIN` | `/tournaments/[tournamentId]/config` |
| `/tournaments/[tournamentId]/tournament-leaderboard` *(BXH Đội trong Giải)* | `Xem Replay trận mới nhất của Đội` *(Trên Podium Top 3 / Bảng)* | Tất cả | `/replay/[tournamentId]/[matchId]` |
| | `Bấm vào Tên Đội / Thành viên` | Tất cả | `/profile/[playerId]` |
| | `Chuyển sang Cây nhánh đấu hiện tại` | Tất cả | `/tournaments/[tournamentId]/bracket` |

---

### 7.4. Nhánh Buồng Lái Phát Lại Trận Đấu (`/replay/*`)

| Giao diện (Route hiện tại) | Nút thao tác trên giao diện | Ai thấy nút này? | Route đích / Hành động |
| :--- | :--- | :--- | :--- |
| `/replay/[matchId]` *(Replay Đấu Bot / Giao hữu)* | `Chuyển góc nhìn (GOD VIEW \| TEAM 1 FOG \| TEAM 2 FOG)` | Tất cả | Cập nhật góc nhìn Canvas tại chỗ |
| | `Bấm vào Đơn vị quân -> Mở TickInspector` | Tất cả | Mở ngăn kéo phải (Entity / I/O JSON / Debug Logs) |
| | `Đấu lại với Bot / Đối thủ này` | `STUDENT` | `/[gameSlug]/practice` hoặc `/[gameSlug]/scrimmage` |
| | `Về Hồ sơ cá nhân` | Tất cả | `/profile` |
| `/replay/[tournamentId]/[matchId]` *(Replay Trận Giải đấu)* | `Chuyển góc nhìn (GOD VIEW \| TEAM 1 FOG \| TEAM 2 FOG)` | Tất cả | Cập nhật góc nhìn Canvas tại chỗ |
| | `Chuyển nhanh giữa Ván 1 / Ván 2 / Ván 3 (Bo3)` | Tất cả | `/replay/[tournamentId]/[nextMatchId]` |
| | `Quay về BXH Giải đấu` | Tất cả | `/tournaments/[tournamentId]/tournament-leaderboard` |
| | `Quay về Cây nhánh đấu` | Tất cả | `/tournaments/[tournamentId]/bracket` |

---

### 7.5. Nhánh Quản Trị Hệ Thống — Dành Riêng Cho Admin (`/admin/*`, `/tournaments/new`, `/tournaments/[tournamentId]/config`)
> **Ràng buộc RBAC:** Toàn bộ các trang dưới đây bắt buộc kiểm tra `role === 'ADMIN'`.

| Giao diện (Route hiện tại) | Nút thao tác trên giao diện | Route đích / Hành động |
| :--- | :--- | :--- |
| `/admin` *(Bảng điều khiển Tổng của Admin)* | `Quản lý & Thêm Game Plug-in` | `/admin/plugins` |
| | `Tạo Giải đấu mới` | `/tournaments/new` |
| | `Chọn Giải đấu để Config & Khởi tạo Nhánh đấu` | `/tournaments/[tournamentId]/config` |
| `/admin/plugins` *(Quản lý & Thêm Plug-in Game)* | `Tải lên / Nạp Game Plug-in mới` (`plugin.manifest.json`) | Lưu Plug-in mới vào hệ thống tại `/admin/plugins` |
| | `Config Luật Game (Cyber-Grid / Stellar Armada)` | `/admin/plugins/[gameSlug]/config` |
| | `Quay lại Admin Dashboard` | `/admin` |
| `/admin/plugins/[gameSlug]/config` *(Config thông số Game)* | `Tạo Bản Cấu hình Luật mới (GameConfig)` *(Sinh tự động từ `config_schema`)* | Lưu `game_configs` (`map_size`, `max_ticks`, `fog_of_war`...) |
| | `Quay lại Danh sách Plug-in` | `/admin/plugins` |
| `/tournaments/new` *(Tạo Giải đấu mới)* | `Xác nhận Tạo Giải đấu` *(Chọn Game Plug-in, `default_config_id`, thời gian)* | Tạo giải đấu & chuyển sang `/tournaments/[tournamentId]/config` |
| | `Hủy / Quay lại Admin Dashboard` | `/admin` |
| `/tournaments/[tournamentId]/config` *(Config Giải đấu & Khởi tạo Nhánh đấu)* | `Lưu Cấu hình Thời gian & Bật/Tắt Giao hữu` (`allow_scrimmage`) | Cập nhật thông số giải tại `/tournaments/[tournamentId]/config` |
| | `Gia hạn nộp bài (+30m Extend Deadline)` | Cộng thêm $30$ phút vào `submission_deadline` |
| | `Đóng băng nộp bài (Freeze Submissions)` | Chuyển trạng thái giải sang `SUBMISSION_FROZEN` |
| | `Thêm Vòng đấu & Gán GameConfig cho Vòng` | Tạo bản ghi `tournament_rounds` mới |
| | `Khởi tạo Nhánh đấu (Generate Round Matchups - Swiss / Double Elimination / Round-Robin)` | Sinh các cặp đấu Bo3 Mirror Map vào hàng đợi `matches` (`QUEUED`) |
| | `Chạy lại các trận lỗi (Re-run SYSTEM_ERROR Matches)` | Đưa các trận `SYSTEM_ERROR` về lại `QUEUED` |
| | `Công bố Kết quả & Nhánh đấu (Publish Round Results)` | Bật `results_published = true`, phát Realtime & chuyển sang `/tournaments/[tournamentId]/bracket` |

---

## 8. CONCRETE ROUTE MAPPING FOR OFFICIAL GAMES (`cyber-grid` & `stellar-armada`)

| Giao diện chức năng | Route thực tế của Game 1 (`cyber-grid`) | Route thực tế của Game 2 (`stellar-armada`) |
| :--- | :--- | :--- |
| **1. Sảnh Game gốc** | `/cyber-grid` | `/stellar-armada` |
| **2. Tổng quan & Starter Kit** | `/cyber-grid/overview` | `/stellar-armada/overview` |
| **3. Đấu với Bot Hệ thống ($24/7$)** | `/cyber-grid/practice` | `/stellar-armada/practice` |
| **4. Nộp Code Bot cấp độ Game** | `/cyber-grid/submission` | `/stellar-armada/submission` |
| **5. Đấu Giao hữu Người chơi** | `/cyber-grid/scrimmage` | `/stellar-armada/scrimmage` |
| **6. BXH Đấu Giao hữu của Game** | `/cyber-grid/scrimmage-leaderboard` | `/stellar-armada/scrimmage-leaderboard` |
| **7. Danh sách Giải đấu của Game** | `/cyber-grid/tournaments` | `/stellar-armada/tournaments` |
| **8. Sảnh Giải đấu cụ thể** | `/tournaments/tourn-cyber-2026` | `/tournaments/tourn-stellar-2026` |
| **9. Đăng ký Đội vào Giải** | `/tournaments/tourn-cyber-2026/register` | `/tournaments/tourn-stellar-2026/register` |
| **10. Nộp bài của Sinh viên trong Giải** | `/tournaments/tourn-cyber-2026/submission` | `/tournaments/tourn-stellar-2026/submission` |
| **11. Cây nhánh đấu hiện tại** | `/tournaments/tourn-cyber-2026/bracket` | `/tournaments/tourn-stellar-2026/bracket` |
| **12. BXH các Đội trong Giải** | `/tournaments/tourn-cyber-2026/tournament-leaderboard` | `/tournaments/tourn-stellar-2026/tournament-leaderboard` |
| **13. Xem Replay Trận Giải đấu** | `/replay/tourn-cyber-2026/[matchId]` | `/replay/tourn-stellar-2026/[matchId]` |
| **14. Admin: Config Luật Game** | `/admin/plugins/cyber-grid/config` | `/admin/plugins/stellar-armada/config` |
| **15. Admin: Config Giải & Khởi tạo Nhánh** | `/tournaments/tourn-cyber-2026/config` | `/tournaments/tourn-stellar-2026/config` |

---

## 9. COCKPIT & BENTO LAYOUT SPECIFICATIONS

### 9.1. Central Lobby & Game Hubs (`/`, `/[gameSlug]/overview`, `/[gameSlug]/practice`, `/[gameSlug]/scrimmage`)
- **Central Lobby (`/`):**
  - Hero banner with dynamic matrix decrypt `<CipherText>` and active system status indicator.
  - Interactive Game Selection Showcase (Cyber-Grid and Stellar Armada) featuring quick links to Overview, 24/7 Practice vs Bot, and Tournament Hub.
  - Quick action toolbar: All Tournaments (`/tournaments`), My Profile (`/profile`), and Admin Room (`/admin` for admins).
- **Game Practice Arena (`/[gameSlug]/practice`):**
  - Direct 24/7 instant match generation against system bots (`DummyBot`, `EasyBot`, `HardBot`).
  - Bot slot selector showing active student bot version with a direct link to change submissions at `/[gameSlug]/submission`.
  - On match start, triggers instant match execution and navigates directly to `/replay/[matchId]`.
- **Game Scrimmage Arena (`/[gameSlug]/scrimmage`):**
  - Peer-to-peer matchmaking with target opponent input or Elo-based random queue.
  - Direct links to Scrimmage Leaderboard (`/[gameSlug]/scrimmage-leaderboard`) and latest match replays (`/replay/[matchId]`).

### 9.2. Tournament Spaces & Leaderboards (`/tournaments/[tournamentId]/*`)
- **Tournament Sảnh Chính (`/tournaments/[tournamentId]`):**
  - Mission countdown clock styled after rocket launch telemetry, current tournament stage badge (`REGISTRATION_OPEN`, `CODING_PHASE`, `SUBMISSION_FROZEN`, `RUNNING_MATCHES`, `COMPLETED`).
  - Navigation tab bar: Sảnh chính, Đăng ký đội (`/register`), Nộp bài Git (`/submission`), Cây nhánh đấu (`/bracket`), BXH đội (`/tournament-leaderboard`).
- **Dynamic Leaderboard Table (`/tournaments/[tournamentId]/tournament-leaderboard`):**
  - **Top 12-Column Bento Grid:**
    - Left Section (8/12 cols) — 3D Holographic Podium (`SpotlightTechCard`): Top 1, 2, and 3 teams displayed in staggered height `<SpotlightTechCard>` containers with gold/silver/bronze edge glows, language badges, win-streak sparklines, `<AnimatedNumber>` ELO scores, and quick *"Watch Latest Replay"* triggers (`/replay/[tournamentId]/[matchId]`).
    - Right Section (4/12 cols) — Submission density heatmap and round telemetry.
  - **Live Standings Table:** Powered by Supabase Realtime Broadcast. Rows shift rankings smoothly with `layout` animations. Team names decrypt via `<CipherText>`; ELO ratings display with `RankChangeIndicator`.
- **Tournament Bracket (`/tournaments/[tournamentId]/bracket`):**
  - Visual tree rendering Swiss / Double Elimination / Round-Robin bracket stages.
  - Interactive matchups linking directly to Bo3 game replays at `/replay/[tournamentId]/[matchId]`.
  - Admin shortcut button to `/tournaments/[tournamentId]/config` for matchup generation and round publishing.

### 9.3. Dual Git CI Submission Pipelines (`/[gameSlug]/submission` & `/tournaments/[tournamentId]/submission`)
- **Scope 1: Game-Level Submission (`/[gameSlug]/submission`):**
  - Manages student's personal bot for casual 24/7 Bot Practice and Scrimmage matches.
  - Provides Git repo webhook configuration and button to mark bot as default (`is_active = true`).
- **Scope 2: Tournament Submission (`/tournaments/[tournamentId]/submission`):**
  - Manages official team submissions locked to a specific tournament.
  - Provides button to select the active official tournament commit (`is_active_for_tournament = true`).
- **Shared CI Circuit & Visual Terminal:**
  - `<LiveCircuitPipeline />`: 4-station automated test bench (`GIT COMMIT` $\rightarrow$ `MANIFEST CHECK` $\rightarrow$ `BUILD (<=15s)` $\rightarrow$ `SMOKE TEST (10 TICKS)`).
  - Visual Terminal Window (`BuildLogPanel`): Streams gzip logs directly from Cloudflare R2, presented in an IDE dark terminal theme with syntax highlighting for `ERROR` and `WARNING` tags, line numbers, and instant copy.

### 9.4. Tactical Simulation Replay Cockpits (`/replay/[matchId]` & `/replay/[tournamentId]/[matchId]`)
Full-screen 100vh Tactical Simulation Cockpit with zero window scroll:
1. **Dual-Wing Esports Telemetry HUD:**
   - **Team 1 Wing (`#00F5D4`) & Team 2 Wing (`#FF2A85`):** Team name with `<CipherText>`, score with high-contrast amber `<AnimatedNumber>`, `<SegmentedBar>` for health and `time_bank_ms`.
   - **Central Perspective Switcher:** High-tech selector toggles `GOD VIEW` | `TEAM 1 FOG` | `TEAM 2 FOG` alongside match `SEED_ID`.
   - **Match Context Header:**
     - In `/replay/[matchId]`: Shows Practice/Scrimmage tag and quick rematch button.
     - In `/replay/[tournamentId]/[matchId]`: Shows Bo3 Game switcher (`Game 1` | `Game 2` | `Game 3`) and quick links back to Bracket and Tournament Leaderboard.
2. **Center Hologram Viewport (`ReplayCanvas`):**
   - Deep Blueprint Navy `#0B192C` backdrop with coordinate rulers (`0..64` or `0..1000.0`).
   - Expanding shockwave rings triggered when entities explode or cores fall.
   - Flickering radar sweep beams, brilliant neon laser trails, and particle spark debris.
3. **Right Collapsible Inspector (`TickInspector`):**
   - Pausing and clicking any entity opens an inspector drawer displaying unit vector schematics, HP gauges, syntax-highlighted `Observation` $\rightarrow$ `Action` JSON payloads, and per-tick `debug_logs`.
4. **Bottom Timeline Scrubbing Dock (`ReplayControls`):**
   - Scrub bar annotated with combat density heatmaps, Play/Pause, $\pm 1$ tick step buttons, and playback speeds `0.25x` to `4x`.

### 9.5. Admin Suite (`/admin`, `/admin/plugins`, `/admin/plugins/[gameSlug]/config`, `/tournaments/new`, `/tournaments/[tournamentId]/config`)
Protected by `AdminGuard` (`role === 'ADMIN'`):
- **Admin Hub (`/admin`):** High-level telemetry, quick shortcuts to plugin registry, tournament creator, and active tournament configurations.
- **Plugin Management (`/admin/plugins` & `.../[gameSlug]/config`):** Drag-and-drop manifest loader, dynamic schema form generating `game_configs`.
- **Tournament Coordinator (`/tournaments/[tournamentId]/config`):** Mission control panel for tournament admins with buttons for +30m deadline extensions, freeze submissions, matchup generator, error reruns, and real-time result publishing.

---

## 10. STRICT PERFORMANCE GUARDRAILS (ZERO-LAG 60FPS)

1. **Never Invoke `useState` on Per-Frame Loops:** `ReplayCanvas` must strictly bind `useRef<HTMLCanvasElement>` and run a `requestAnimationFrame` loop reading from `playbackRef` synchronized with Zustand. Particle systems, shockwaves, and spatial interpolations render directly on Canvas 2D/WebGL.
2. **Strict Adherence to `ReplayRenderer` Contract:** All plugin visualizers export `createRenderer(canvas): ReplayRenderer` implementing `render(input)`, `resize(w, h, dpr)`, and `destroy()`.
3. **Zero Egress & Client-Side Decompression:** Replay payloads (`replay.json.gz`) and execution logs download directly from Cloudflare R2 / CDN and decompress client-side via browser `DecompressionStream('gzip')`.
4. **Theme Manifest Integration:** All frontend components must inherit CSS variables driven by `docs/ui_theme_manifest.md`.