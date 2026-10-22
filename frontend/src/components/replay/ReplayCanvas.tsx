"use client";

import React, { useRef, useEffect } from "react";
import { useReplayStore, ReplayDocument } from "@/store/useReplayStore";
import { createRenderer, ReplayRenderer } from "./ReplayRenderer";

interface ReplayCanvasProps {
  doc: ReplayDocument;
  width?: number;
  height?: number;
}

export function ReplayCanvas({ doc, width = 640, height = 640 }: ReplayCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rendererRef = useRef<ReplayRenderer | null>(null);
  const cachedStatesRef = useRef<any[]>([]);

  const {
    currentTick,
    isPlaying,
    playbackSpeed,
    fogMode,
    selectedUnitId,
    setTick,
    selectUnit,
  } = useReplayStore();

  // Mutable playback state accessed inside requestAnimationFrame loop
  const playbackRef = useRef({
    tick: currentTick,
    isPlaying,
    speed: playbackSpeed,
    fogMode,
    selectedUnitId,
    lastFrameTime: performance.now(),
    tickAccumulator: 0,
  });

  // Sync React/Zustand state changes into mutable playbackRef
  useEffect(() => {
    playbackRef.current.tick = currentTick;
    playbackRef.current.isPlaying = isPlaying;
    playbackRef.current.speed = playbackSpeed;
    playbackRef.current.fogMode = fogMode;
    playbackRef.current.selectedUnitId = selectedUnitId;
  }, [currentTick, isPlaying, playbackSpeed, fogMode, selectedUnitId]);

  // Pre-calculate all tick states once when document changes
  useEffect(() => {
    if (!canvasRef.current) return;
    const renderer = createRenderer(canvasRef.current);
    rendererRef.current = renderer;
    cachedStatesRef.current = renderer.reconstructAllStates(doc);
  }, [doc]);

  // 60fps Continuous requestAnimationFrame Loop
  useEffect(() => {
    let animId: number;

    const frameLoop = (timestamp: number) => {
      const p = playbackRef.current;
      const renderer = rendererRef.current;
      const states = cachedStatesRef.current;
      const totalStates = states.length;

      const deltaMs = timestamp - p.lastFrameTime;
      p.lastFrameTime = timestamp;

      if (p.isPlaying && totalStates > 1) {
        // Base turn speed: ~200ms per tick at 1x speed
        const msPerTick = 200 / p.speed;
        p.tickAccumulator += deltaMs;

        if (p.tickAccumulator >= msPerTick) {
          const advance = Math.floor(p.tickAccumulator / msPerTick);
          p.tickAccumulator %= msPerTick;
          const nextTick = Math.min(p.tick + advance, totalStates - 1);

          if (nextTick !== p.tick) {
            p.tick = nextTick;
            // Sync tick to UI store
            setTick(nextTick);
          }
          if (p.tick >= totalStates - 1) {
            p.isPlaying = false;
          }
        }
      }

      // Render the current state
      const targetState = states[Math.min(p.tick, totalStates - 1)];
      if (renderer && targetState) {
        renderer.render(
          targetState,
          doc.map,
          p.fogMode,
          p.selectedUnitId,
          timestamp,
          doc.ticks[p.tick]?.events
        );
      }

      animId = requestAnimationFrame(frameLoop);
    };

    animId = requestAnimationFrame(frameLoop);
    return () => cancelAnimationFrame(animId);
  }, [doc, setTick]);

  // Click on canvas to select entity
  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const cellSize = rect.width / (doc.map.size || 16);
    const gridX = Math.floor(x / cellSize);
    const gridY = Math.floor(y / cellSize);

    const states = cachedStatesRef.current;
    const state = states[playbackRef.current.tick];
    if (!state) return;

    let clickedUnitId: number | null = null;
    for (const unit of Object.values(state.units) as any[]) {
      if (unit.x === gridX && unit.y === gridY) {
        clickedUnitId = unit.id;
        break;
      }
    }
    selectUnit(clickedUnitId);
  };

  return (
    <div className="relative rounded overflow-hidden border border-slate-700 shadow-xl bg-slate-950 flex items-center justify-center">
      <canvas
        ref={canvasRef}
        width={width}
        height={height}
        onClick={handleCanvasClick}
        className="cursor-crosshair max-w-full aspect-square block"
      />
    </div>
  );
}
