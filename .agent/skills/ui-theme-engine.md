---
name: ui-theme-engine
description: Synchronizes dynamic themes from docs/ui_theme_manifest.md and docs/doc6_ui_design_guide.md into Next.js Frontend source code (colors, chamfer clip-paths, and kinetic motion).
---

# SKILL EXECUTION PROTOCOL: UI THEME ENGINE (`/apply-ui-theme`)

Whenever the user requests **"Change theme"**, **"Update UI from markdown"**, edits `docs/ui_theme_manifest.md`, or during the execution of **Phase 5 & Phase 6**, the AI Agent must strictly follow this 4-step autonomous procedure:

---

## Step 1: Parse Configuration From Markdown
1. Read `docs/ui_theme_manifest.md` to extract:
   - Active palette at `ACTIVE_PRESET` (`AERO_LAB_BRIGHT`, `CYBER_TERMINAL_HYBRID`, `QUANTUM_HOLO_MINT`, or `CUSTOM`).
   - Card geometry at `CARD_CORNER_STYLE` (`CHAMFER_45`, `SHARP_TECH`, or `ROUNDED_GLASS`).
   - Kinetic motion intensity at `MOTION_INTENSITY` (`HIGH`, `MEDIUM`, `LOW`, `OFF`).
   - Feature flags: `ENABLE_TEXT_SCRAMBLE`, `ENABLE_SVG_PULSE_LINES`, `ENABLE_SPOTLIGHT_HOVER`, `ENABLE_RADAR_SWEEP`.
2. Review `docs/doc6_ui_design_guide.md` for exact UI component specifications.

---

## Step 2: Synchronize CSS Variables in `frontend/src/app/globals.css`
Update or regenerate the `:root` block in `frontend/src/app/globals.css` to 100% match the selected preset:
- **Color Variables:**
  - `--bg-canvas`
  - `--bg-panel`
  - `--bg-arena`
  - `--text-main`
  - `--text-muted`
  - `--border-color`
  - `--accent-primary`
  - `--accent-secondary`
  - `--team-1` & `--team-1-ui`
  - `--team-2` & `--team-2-ui`
  - `--status-passed`, `--status-running`, `--status-error`
- **Card Geometry Variables:**
  - If `CHAMFER_45`:
    ```css
    --card-clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px));
    --card-radius: 0px;
    ```
  - If `SHARP_TECH`:
    ```css
    --card-clip-path: none;
    --card-radius: 0px;
    ```
  - If `ROUNDED_GLASS`:
    ```css
    --card-clip-path: none;
    --card-radius: 12px;
    ```
- **Motion Variables:**
  - Adjust animation durations and transitions based on `MOTION_INTENSITY`.

---

## Step 3: Verify & Standardize `kinetic-primitives.tsx`
Ensure `frontend/src/components/ui/kinetic-primitives.tsx` (created in Phase 5) exports all 6 core components and respects feature flags:
1. `<SpotlightTechCard>`: Frosted glass card with `--card-clip-path`, 4 corner crosshair markers `+`, mouse-following `radial-gradient` spotlight, and 3D tilt.
2. `<CipherText>`: Rapid random-character decipher animation (250–300ms) for dynamic text. Bypassed if `ENABLE_TEXT_SCRAMBLE: false`.
3. `<AnimatedNumber>`: Smooth Spring/Odometer rolling numbers with `font-mono tabular-nums`.
4. `<SegmentedBar>`: 20-segment gauge skewed at `skew-x-[-18deg]`, flashing red alert when capacity < 15%.
5. `<LiveCircuitPipeline>`: 4-stage Git CI PCB pipeline with animated SVG light beam particles.
6. `<LiveTelemetryTicker>`: Live system status ribbon with pulsing LED, waveform, and tournament event broadcast.

---

## Step 4: Verification & Reporting
1. Verify CSS syntax and TypeScript integrity.
2. If frontend code exists, execute `npm run typecheck` or `npm run build` in `frontend/`.
3. Provide a concise report: active preset, corner geometry, enabled kinetic effects, and build readiness.
