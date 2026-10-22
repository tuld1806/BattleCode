import { ReplayDocument, ReplayState, ReplayMap, FogMode, ReplayUnit } from "@/store/useReplayStore";

export interface ReplayRenderer {
  render: (
    state: ReplayState,
    map: ReplayMap,
    fogMode: FogMode,
    selectedUnitId: number | null,
    animTime: number,
    events?: string[]
  ) => void;
  reconstructAllStates: (doc: ReplayDocument) => ReplayState[];
}

export function createRenderer(canvas: HTMLCanvasElement): ReplayRenderer {
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Could not acquire 2D canvas context");

  function reconstructAllStates(doc: ReplayDocument): ReplayState[] {
    const totalTicks = doc.ticks.length;
    const states: ReplayState[] = new Array(totalTicks + 1);

    // Initial state at tick 0
    let currentState: ReplayState = {
      tick: doc.initialState.tick,
      units: { ...doc.initialState.units },
      resources: { ...doc.initialState.resources },
      visibility: { ...doc.initialState.visibility },
    };
    states[0] = currentState;

    for (let i = 0; i < totalTicks; i++) {
      const frame = doc.ticks[i];
      if (frame.keyframe) {
        currentState = {
          tick: frame.keyframe.tick,
          units: { ...frame.keyframe.units },
          resources: { ...frame.keyframe.resources },
          visibility: { ...frame.keyframe.visibility },
        };
      } else if (frame.delta) {
        const nextUnits = { ...currentState.units };
        for (const remId of frame.delta.removed) {
          delete nextUnits[remId];
        }
        for (const [id, unit] of Object.entries(frame.delta.upserts)) {
          nextUnits[Number(id)] = unit;
        }
        currentState = {
          tick: frame.delta.tick,
          units: nextUnits,
          resources: { ...frame.delta.resources },
          visibility: { ...frame.delta.visibility },
        };
      }
      states[i + 1] = currentState;
    }

    return states;
  }

  function render(
    state: ReplayState,
    map: ReplayMap,
    fogMode: FogMode,
    selectedUnitId: number | null,
    animTime: number,
    events?: string[]
  ) {
    if (!ctx) return;
    const width = canvas.width;
    const height = canvas.height;
    const size = map.size || 16;
    const cellSize = width / size;

    // 1. Arena Background
    ctx.fillStyle = "#0B192C";
    ctx.fillRect(0, 0, width, height);

    // 2. Blueprint Grid lines
    ctx.strokeStyle = "rgba(2, 132, 199, 0.12)";
    ctx.lineWidth = 1;
    for (let i = 0; i <= size; i++) {
      ctx.beginPath();
      ctx.moveTo(i * cellSize, 0);
      ctx.lineTo(i * cellSize, height);
      ctx.stroke();

      ctx.beginPath();
      ctx.moveTo(0, i * cellSize);
      ctx.lineTo(width, i * cellSize);
      ctx.stroke();
    }

    // 3. Obstacles
    ctx.fillStyle = "#1E293B";
    for (const cellIdx of map.obstacles) {
      const x = (cellIdx % size) * cellSize;
      const y = Math.floor(cellIdx / size) * cellSize;
      ctx.fillRect(x + 1, y + 1, cellSize - 2, cellSize - 2);

      // Technical crosshatch
      ctx.strokeStyle = "rgba(100, 116, 139, 0.3)";
      ctx.beginPath();
      ctx.moveTo(x + 2, y + 2);
      ctx.lineTo(x + cellSize - 2, y + cellSize - 2);
      ctx.stroke();
    }

    // 4. Resource Nodes
    for (const cellIdx of map.resourceNodes) {
      const x = (cellIdx % size) * cellSize;
      const y = Math.floor(cellIdx / size) * cellSize;

      ctx.fillStyle = "rgba(245, 158, 11, 0.25)";
      ctx.fillRect(x + 2, y + 2, cellSize - 4, cellSize - 4);
      ctx.strokeStyle = "#F59E0B";
      ctx.lineWidth = 1.5;
      ctx.strokeRect(x + 3, y + 3, cellSize - 6, cellSize - 6);

      // Gold Core Dot
      ctx.fillStyle = "#F59E0B";
      ctx.beginPath();
      ctx.arc(x + cellSize / 2, y + cellSize / 2, 2.5, 0, Math.PI * 2);
      ctx.fill();
    }

    // Visibility Mask Calculation for Fog of War
    let visibleCells: Set<number> | null = null;
    if (fogMode === "TEAM_1" && state.visibility[1]) {
      visibleCells = new Set(state.visibility[1]);
    } else if (fogMode === "TEAM_2" && state.visibility[2]) {
      visibleCells = new Set(state.visibility[2]);
    }

    // 5. Units
    for (const unit of Object.values(state.units)) {
      const unitCell = unit.y * size + unit.x;

      // In team perspective, hide unseen enemy units
      if (visibleCells && !visibleCells.has(unitCell)) {
        continue;
      }

      const ux = unit.x * cellSize;
      const uy = unit.y * cellSize;
      const cx = ux + cellSize / 2;
      const cy = uy + cellSize / 2;
      const isTeamOne = unit.team === 1;
      const unitColor = isTeamOne ? "#00F5D4" : "#FF2A85";

      ctx.save();

      // Selection Highlight
      if (selectedUnitId === unit.id) {
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 2;
        ctx.strokeRect(ux + 2, uy + 2, cellSize - 4, cellSize - 4);
      }

      // Unit Body rendering by Type
      if (unit.type === "CORE") {
        ctx.fillStyle = unitColor;
        ctx.shadowColor = unitColor;
        ctx.shadowBlur = 10;
        ctx.beginPath();
        ctx.moveTo(cx, uy + 4);
        ctx.lineTo(ux + cellSize - 4, cy);
        ctx.lineTo(cx, uy + cellSize - 4);
        ctx.lineTo(ux + 4, cy);
        ctx.closePath();
        ctx.fill();
        ctx.shadowBlur = 0;
      } else if (unit.type === "TANK") {
        ctx.fillStyle = unitColor;
        ctx.fillRect(ux + 5, uy + 5, cellSize - 10, cellSize - 10);
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 1;
        ctx.strokeRect(ux + 5, uy + 5, cellSize - 10, cellSize - 10);
      } else if (unit.type === "SCOUT") {
        ctx.fillStyle = unitColor;
        ctx.beginPath();
        ctx.arc(cx, cy, cellSize / 3, 0, Math.PI * 2);
        ctx.fill();
      } else {
        // WORKER
        ctx.fillStyle = unitColor;
        ctx.beginPath();
        ctx.moveTo(cx, uy + 5);
        ctx.lineTo(ux + cellSize - 5, uy + cellSize - 5);
        ctx.lineTo(ux + 5, uy + cellSize - 5);
        ctx.closePath();
        ctx.fill();
      }

      // Mini Health Bar
      const maxHp = unit.type === "CORE" ? 100 : unit.type === "TANK" ? 60 : unit.type === "SCOUT" ? 20 : 25;
      const hpPct = Math.max(0, Math.min(1, unit.hp / maxHp));
      const barWidth = cellSize - 8;
      const barHeight = 2.5;

      ctx.fillStyle = "rgba(0,0,0,0.6)";
      ctx.fillRect(ux + 4, uy + 1, barWidth, barHeight);
      ctx.fillStyle = hpPct > 0.4 ? "#10B981" : "#EF4444";
      ctx.fillRect(ux + 4, uy + 1, barWidth * hpPct, barHeight);

      ctx.restore();
    }

    // 6. Fog of War Darkening Overlay
    if (visibleCells) {
      ctx.fillStyle = "rgba(11, 25, 44, 0.82)";
      for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
          const idx = y * size + x;
          if (!visibleCells.has(idx)) {
            ctx.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
          }
        }
      }
    }

    // 7. Radar Sweep Animation
    const sweepAngle = (animTime * 0.0015) % (Math.PI * 2);
    ctx.save();
    ctx.translate(width / 2, height / 2);
    ctx.strokeStyle = "rgba(0, 245, 212, 0.15)";
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(Math.cos(sweepAngle) * (width * 0.7), Math.sin(sweepAngle) * (height * 0.7));
    ctx.stroke();
    ctx.restore();
  }

  return {
    render,
    reconstructAllStates,
  };
}
