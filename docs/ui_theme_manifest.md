# LIVE THEME MANIFEST & UI CONTROL PANEL
**File:** `docs/ui_theme_manifest.md`  
**Project:** BattleCode-UET AI Arena Platform  
**Instructions for Project Owner:** Modify the configuration values below to customize the visual appearance. When you update settings or invoke the theme engine skill, the AI Agent will automatically synchronize all color variables, chamfer geometries, and kinetic animation parameters into the Next.js Frontend codebase.

---

## 1. ACTIVE COLOR PRESET
- `ACTIVE_PRESET`: `AERO_LAB_BRIGHT`
*(Built-in options: `AERO_LAB_BRIGHT` | `CYBER_TERMINAL_HYBRID` | `QUANTUM_HOLO_MINT` | `CUSTOM`)*

---

## 2. GEOMETRY & MOTION CONTROLS

- `CARD_CORNER_STYLE`: `CHAMFER_45`
  - `CHAMFER_45`: 45-degree technical chamfered edges with aerospace polygon clip-path (`clip-path: polygon(...)`).
  - `SHARP_TECH`: 90-degree sharp corners with crosshair reticle markers (`+`) positioned at all 4 corners.
  - `ROUNDED_GLASS`: Modern rounded frosted glass corners (`border-radius: 12px`).

- `MOTION_INTENSITY`: `HIGH`
  - `HIGH`: Maximum kinetics (Matrix text scramble, animated SVG circuit pulses, 3D tilt, Odometer spring rolling numbers, radar sweeps).
  - `MEDIUM`: Balanced smooth transitions, disables heavier 3D perspective tilt calculations.
  - `LOW`: Minimalist transitions, basic state shifts only.
  - `OFF`: Completely disables animations for low-power or legacy client devices.

- `BACKGROUND_EFFECT`: `ANIMATED_DOT_MATRIX`
  - `ANIMATED_DOT_MATRIX`: Subtle breathing technical dot-matrix grid with mouse-following optical spotlight.
  - `TACTICAL_GRID`: 100px square architectural blueprint grid.
  - `CLEAN_SOLID`: Flat, clean solid background with no grid overlay.

- `ENABLE_TEXT_SCRAMBLE`: `true`
  - Enables/disables random matrix decoding animations (`<CipherText>`) on team names, commit hashes, and ELO ratings.

- `ENABLE_SVG_PULSE_LINES`: `true`
  - Enables/disables moving light particle beams along the Git CI submission pipeline (`<LiveCircuitPipeline>`).

- `ENABLE_SPOTLIGHT_HOVER`: `true`
  - Enables/disables mouse-following optical radial gradient spotlights on tech cards (`<SpotlightTechCard>`).

- `ENABLE_RADAR_SWEEP`: `true`
  - Enables/disables radar sweep rays and expanding shockwave rings on match arenas (`ReplayCanvas`).

---

## 3. BUILT-IN COLOR PRESET SPECIFICATIONS

### [PRESET 1] `AERO_LAB_BRIGHT` (Default: High-Energy Aerospace AI Research Lab)
*Aesthetic: Futuristic AI computing laboratory, icy white base, crisp, high-contrast, and deeply academic.*
- `--bg-canvas`: `#F4F7FB` (Lab Ice White base)
- `--bg-panel`: `rgba(255, 255, 255, 0.88)` (Frosted optical glass with backdrop-blur-md)
- `--bg-arena`: `#0B192C` (Deep Blueprint Navy for ReplayCanvas)
- `--text-main`: `#0F172A` (Deep space obsidian text)
- `--text-muted`: `#64748B` (Technical slate for secondary labels)
- `--border-color`: `#CBD5E1` (Polished metallic border)
- `--accent-primary`: `#0284C7` (Electric Cobalt/Sky)
- `--accent-secondary`: `#2563EB` (Electric Blue)
- `--team-1`: `#00F5D4` (Neon Aqua on dark match canvas)
- `--team-1-ui`: `#00A896` (Deep Cyan high-contrast text & badges on bright HUD)
- `--team-2`: `#FF2A85` (Neon Rose on dark match canvas)
- `--team-2-ui`: `#E11D48` (Vibrant Rose high-contrast text & badges on bright HUD)
- `--status-passed`: `#10B981` (Emerald Green)
- `--status-running`: `#F59E0B` (Electric Amber)
- `--status-error`: `#EF4444` (Coral Red)

### [PRESET 2] `CYBER_TERMINAL_HYBRID` (High Contrast: Metallic Silver & Quantum Indigo)
*Aesthetic: Hacker competitive algorithmic workstation with sharp metallic edges.*
- `--bg-canvas`: `#EAEFF5` (Brushed metallic silver)
- `--bg-panel`: `rgba(255, 255, 255, 0.95)` (High-opacity silver panel)
- `--bg-arena`: `#090D16` (Deep void black)
- `--text-main`: `#090D16` (Obsidian black)
- `--text-muted`: `#475569` (Dark slate)
- `--border-color`: `#94A3B8` (Chrome steel border)
- `--accent-primary`: `#4F46E5` (Quantum Indigo)
- `--accent-secondary`: `#6366F1` (Electric Iris)
- `--team-1`: `#00F5D4` | `--team-1-ui`: `#059669`
- `--team-2`: `#FF2A85` | `--team-2-ui`: `#DB2777`
- `--status-passed`: `#059669`
- `--status-running`: `#D97706`
- `--status-error`: `#DC2626`

### [PRESET 3] `QUANTUM_HOLO_MINT` (Biocomputational Energy: Jade White & Optical Teal)
*Aesthetic: Quantum bio-algorithmic computing institute, refreshing, clean, and inspiring.*
- `--bg-canvas`: `#F0FDF9` (Mint crystalline white)
- `--bg-panel`: `rgba(255, 255, 255, 0.86)` (Frosted jade glass)
- `--bg-arena`: `#06283D` (Deep oceanic abyss)
- `--text-main`: `#064E3B` (Deep pine green)
- `--text-muted`: `#0F766E` (Subdued teal)
- `--border-color`: `#99F6E4` (Pale jade border)
- `--accent-primary`: `#0D9488` (Optical Teal)
- `--accent-secondary`: `#14B8A6` (Bright Mint)
- `--team-1`: `#00F5D4` | `--team-1-ui`: `#0D9488`
- `--team-2`: `#FF2A85` | `--team-2-ui`: `#F43F5E`
- `--status-passed`: `#10B981`
- `--status-running`: `#F59E0B`
- `--status-error`: `#F43F5E`

---

## 4. GEOMETRIC CLIP-PATH RULES FOR CARD_CORNER_STYLE

When generating the `--card-clip-path` variable into `app/globals.css`:
- **When `CHAMFER_45`:**
  ```css
  --card-clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px));
  --card-radius: 0px;
  ```
- **When `SHARP_TECH`:**
  ```css
  --card-clip-path: none;
  --card-radius: 0px;
  ```
- **When `ROUNDED_GLASS`:**
  ```css
  --card-clip-path: none;
  --card-radius: 12px;
  ```