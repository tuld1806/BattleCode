"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { ReplayCanvas } from "@/components/replay/ReplayCanvas";
import { ReplayControls } from "@/components/replay/ReplayControls";
import {
  useReplayStore,
  ReplayDocument,
  ReplayState,
} from "@/store/useReplayStore";
import {
  SpotlightTechCard,
  CipherText,
  SegmentedBar,
} from "@/components/ui/kinetic-primitives";
import { Play, Shield, Terminal, Activity, Zap, Cpu } from "lucide-react";
import { cn } from "@/lib/utils";

// Generate rich 50-tick demo replay if offline or testing
function generateDemoReplay(): ReplayDocument {
  const size = 16;
  const obstacles = [35, 36, 51, 52, 195, 196, 211, 212, 119, 120, 135, 136];
  const resourceNodes = [22, 25, 230, 233, 114, 125, 130, 141];

  const initialState: ReplayState = {
    tick: 0,
    units: {
      1: { id: 1, team: 1, type: "CORE", x: 1, y: 1, hp: 100 },
      2: { id: 2, team: 1, type: "WORKER", x: 2, y: 1, hp: 25 },
      3: { id: 3, team: 2, type: "CORE", x: 14, y: 14, hp: 100 },
      4: { id: 4, team: 2, type: "WORKER", x: 13, y: 14, hp: 25 },
    },
    resources: { 1: 100, 2: 100 },
    visibility: {
      1: [17, 18, 33, 34, 0, 1, 2, 16],
      2: [238, 239, 254, 255, 222, 223],
    },
  };

  const ticks: ReplayDocument["ticks"] = [];
  for (let t = 1; t <= 60; t++) {
    // Generate moving units and dynamic action
    const t1WorkerX = Math.min(6, 2 + Math.floor(t / 8));
    const t1WorkerY = Math.min(6, 1 + Math.floor(t / 10));
    const t2WorkerX = Math.max(9, 13 - Math.floor(t / 8));
    const t2WorkerY = Math.max(9, 14 - Math.floor(t / 10));

    const events: string[] = [];
    if (t === 5) events.push("Team 1 spawned Scout #5 at (2,2)");
    if (t === 10) events.push("Team 2 spawned Tank #6 at (13,13)");
    if (t === 25) events.push("Team 1 Scout #5 attacked Resource Node guardian");
    if (t === 45) events.push("Clash at central sector! Shockwave detected.");

    const upserts: Record<number, any> = {
      2: { id: 2, team: 1, type: "WORKER", x: t1WorkerX, y: t1WorkerY, hp: 25 },
      4: { id: 4, team: 2, type: "WORKER", x: t2WorkerX, y: t2WorkerY, hp: 25 },
    };
    if (t >= 5) {
      upserts[5] = { id: 5, team: 1, type: "SCOUT", x: Math.min(10, 2 + Math.floor(t / 3)), y: Math.min(10, 2 + Math.floor(t / 4)), hp: 20 };
    }
    if (t >= 10) {
      upserts[6] = { id: 6, team: 2, type: "TANK", x: Math.max(6, 13 - Math.floor(t / 4)), y: Math.max(6, 13 - Math.floor(t / 4)), hp: Math.max(10, 60 - t) };
    }

    ticks.push({
      tick: t,
      delta: {
        tick: t,
        upserts,
        removed: t >= 55 ? [5] : [],
        resources: {
          1: 100 + t * 5,
          2: 100 + t * 5,
        },
        visibility: {
          1: [0, 1, 2, 16, 17, 18, 33, 34, 49, 50, 65, 66, 81, 82, 97, 98, 114],
          2: [255, 254, 253, 239, 238, 237, 223, 222, 207, 206, 191, 190, 175, 174, 141],
        },
      },
      events,
    });
  }

  return {
    formatVersion: "1",
    matchId: "demo-cybergrid-match-001",
    seed: 133742,
    config: { size: 16, maxTicks: 200 },
    teams: [
      { id: 1, name: "CyberVanguard" },
      { id: 2, name: "NeuralShock" },
    ],
    map: { size, obstacles, resourceNodes },
    initialState,
    ticks,
  };
}

export default function ReplayPage() {
  const [doc, setDoc] = useState<ReplayDocument | null>(null);
  const { currentTick, selectedUnitId } = useReplayStore();

  useEffect(() => {
    // Load demo replay by default
    const demo = generateDemoReplay();
    setDoc(demo);
  }, []);

  if (!doc) {
    return (
      <div className="p-12 text-center font-mono text-xs text-slate-500">
        INITIALIZING TACTICAL REPLAY THEATER...
      </div>
    );
  }

  const currentFrame = doc.ticks[currentTick - 1];

  return (
    <div className="space-y-6 py-4">
      {/* Top Match Bar HUD */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Team 1 Banner (Neon Aqua) */}
        <SpotlightTechCard className="border-l-4 border-[#00F5D4] bg-white space-y-2 p-4">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="font-bold text-[#00A896] flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5" />
              TEAM 1 • AQUA CORE
            </span>
            <span className="text-slate-500 font-semibold">
              DATA: {100 + currentTick * 5}
            </span>
          </div>
          <div className="text-xl font-mono font-bold text-slate-900 truncate">
            <CipherText text={doc.teams[0]?.name || "CyberVanguard"} />
          </div>
          <div className="flex items-center justify-between pt-1">
            <span className="text-[10px] font-mono text-slate-500">CORE INTEGRITY</span>
            <SegmentedBar value={100} max={100} segments={10} />
          </div>
        </SpotlightTechCard>

        {/* Team 2 Banner (Neon Rose) */}
        <SpotlightTechCard className="border-l-4 border-[#FF2A85] bg-white space-y-2 p-4">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="font-bold text-[#E11D48] flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5" />
              TEAM 2 • ROSE CORE
            </span>
            <span className="text-slate-500 font-semibold">
              DATA: {100 + currentTick * 5}
            </span>
          </div>
          <div className="text-xl font-mono font-bold text-slate-900 truncate">
            <CipherText text={doc.teams[1]?.name || "NeuralShock"} />
          </div>
          <div className="flex items-center justify-between pt-1">
            <span className="text-[10px] font-mono text-slate-500">CORE INTEGRITY</span>
            <SegmentedBar value={Math.max(20, 100 - currentTick)} max={100} segments={10} />
          </div>
        </SpotlightTechCard>
      </div>

      {/* Main Tactical Arena & Side Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left 2 Cols: Canvas & Controls */}
        <div className="lg:col-span-2 space-y-4">
          <ReplayCanvas doc={doc} width={640} height={640} />
          <ReplayControls maxTicks={doc.ticks.length} />
        </div>

        {/* Right Col: Tactical Telemetry & Events */}
        <div className="space-y-4">
          {/* Unit Inspector */}
          <SpotlightTechCard className="space-y-3">
            <h3 className="text-xs font-mono font-bold text-slate-700 flex items-center gap-2 border-b border-slate-200 pb-2">
              <Cpu className="w-4 h-4 text-sky-600" />
              UNIT TELEMETRY INSPECTOR
            </h3>
            {selectedUnitId ? (
              <div className="space-y-2 font-mono text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">UNIT ID:</span>
                  <span className="font-bold text-slate-900">#{selectedUnitId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">TEAM:</span>
                  <span className="font-bold text-sky-600">
                    {selectedUnitId % 2 === 1 ? "TEAM 1 (AQUA)" : "TEAM 2 (ROSE)"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">STATUS:</span>
                  <span className="text-emerald-600 font-bold">OPERATIONAL</span>
                </div>
              </div>
            ) : (
              <div className="text-xs font-mono text-slate-400 py-4 text-center">
                Click any unit on the 16x16 grid to inspect telemetry.
              </div>
            )}
          </SpotlightTechCard>

          {/* Event Stream Log */}
          <SpotlightTechCard className="space-y-3">
            <h3 className="text-xs font-mono font-bold text-slate-700 flex items-center gap-2 border-b border-slate-200 pb-2">
              <Activity className="w-4 h-4 text-amber-500" />
              TICK {currentTick} EVENT LOG
            </h3>
            <div className="space-y-1.5 font-mono text-xs max-h-48 overflow-y-auto">
              {currentFrame?.events && currentFrame.events.length > 0 ? (
                currentFrame.events.map((evt, idx) => (
                  <div key={idx} className="p-2 rounded bg-slate-50 border border-slate-200 text-slate-700 flex items-start gap-1.5">
                    <span className="text-sky-600 font-bold">&gt;</span>
                    <span>{evt}</span>
                  </div>
                ))
              ) : (
                <div className="text-xs text-slate-400 py-2">
                  No critical combat events on Tick {currentTick}.
                </div>
              )}
            </div>
          </SpotlightTechCard>

          {/* Match Parameters */}
          <SpotlightTechCard className="space-y-2 text-xs font-mono text-slate-600">
            <div className="flex justify-between">
              <span>SEED:</span>
              <span className="text-slate-900 font-bold">{doc.seed}</span>
            </div>
            <div className="flex justify-between">
              <span>MATCH ID:</span>
              <span className="text-slate-900 font-bold truncate max-w-[150px]">
                {doc.matchId}
              </span>
            </div>
            <div className="flex justify-between">
              <span>ENGINE:</span>
              <span className="text-sky-600 font-bold">CyberGrid v1.0</span>
            </div>
          </SpotlightTechCard>
        </div>
      </div>
    </div>
  );
}
