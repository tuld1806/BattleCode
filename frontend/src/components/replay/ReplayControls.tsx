"use client";

import React, { useEffect } from "react";
import { useReplayStore, FogMode } from "@/store/useReplayStore";
import { Play, Pause, SkipBack, SkipForward, Eye, Shield } from "lucide-react";
import { cn } from "@/lib/utils";

interface ReplayControlsProps {
  maxTicks: number;
  className?: string;
}

export function ReplayControls({ maxTicks, className }: ReplayControlsProps) {
  const {
    currentTick,
    isPlaying,
    playbackSpeed,
    fogMode,
    setTick,
    togglePlay,
    setSpeed,
    setFogMode,
  } = useReplayStore();

  // Keyboard shortcut listener: Space to toggle play/pause
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.code === "Space") {
        e.preventDefault();
        togglePlay();
      } else if (e.code === "ArrowLeft") {
        e.preventDefault();
        setTick(Math.max(0, currentTick - 1));
      } else if (e.code === "ArrowRight") {
        e.preventDefault();
        setTick(Math.min(maxTicks, currentTick + 1));
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [currentTick, maxTicks, setTick, togglePlay]);

  return (
    <div className={cn("p-4 rounded border border-slate-300 bg-white/95 backdrop-blur-md shadow-sm space-y-4", className)}>
      {/* Scrubber Track */}
      <div className="space-y-1">
        <div className="flex items-center justify-between text-xs font-mono text-slate-600">
          <span className="font-bold">
            TICK <span className="text-sky-600">{currentTick}</span> / {maxTicks}
          </span>
          <span className="text-[11px] text-slate-400">
            {((currentTick / (maxTicks || 1)) * 100).toFixed(0)}%
          </span>
        </div>
        <input
          type="range"
          min={0}
          max={maxTicks}
          value={currentTick}
          onChange={(e) => setTick(Number(e.target.value))}
          className="w-full accent-sky-600 cursor-pointer h-1.5 bg-slate-200 rounded-lg appearance-none"
        />
      </div>

      {/* Buttons Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
        {/* Playback Controls */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setTick(Math.max(0, currentTick - 1))}
            className="p-1.5 rounded hover:bg-slate-100 text-slate-700 transition-colors"
            title="Step Back (Left Arrow)"
          >
            <SkipBack className="w-4 h-4" />
          </button>
          <button
            onClick={togglePlay}
            className="p-2 rounded-full bg-sky-600 hover:bg-sky-700 text-white transition-colors shadow-sm"
            title="Play / Pause (Space)"
          >
            {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
          </button>
          <button
            onClick={() => setTick(Math.min(maxTicks, currentTick + 1))}
            className="p-1.5 rounded hover:bg-slate-100 text-slate-700 transition-colors"
            title="Step Forward (Right Arrow)"
          >
            <SkipForward className="w-4 h-4" />
          </button>

          {/* Speed Buttons */}
          <div className="flex items-center gap-1 ml-2 border-l border-slate-200 pl-2">
            {([1, 2, 4] as const).map((spd) => (
              <button
                key={spd}
                onClick={() => setSpeed(spd)}
                className={cn(
                  "px-2 py-0.5 rounded text-[10px] font-mono font-bold transition-colors",
                  playbackSpeed === spd
                    ? "bg-slate-900 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                )}
              >
                {spd}x
              </button>
            ))}
          </div>
        </div>

        {/* Fog of War Perspective */}
        <div className="flex items-center gap-1 text-xs font-mono">
          <span className="text-[10px] text-slate-500 uppercase mr-1">VISION:</span>
          {(["OMNISCIENT", "TEAM_1", "TEAM_2"] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setFogMode(mode)}
              className={cn(
                "px-2 py-1 rounded text-[10px] font-bold uppercase transition-all",
                fogMode === mode
                  ? mode === "TEAM_1"
                    ? "bg-cyan-500 text-slate-950 font-extrabold shadow-sm"
                    : mode === "TEAM_2"
                    ? "bg-rose-500 text-white font-extrabold shadow-sm"
                    : "bg-sky-600 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              )}
            >
              {mode === "OMNISCIENT" ? "ALL" : mode === "TEAM_1" ? "T1 AQUA" : "T2 ROSE"}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
