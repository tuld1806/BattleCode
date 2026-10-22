"use client";

import React, { useState, useEffect, useRef } from "react";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// 1. SpotlightTechCard
// Chamfered 45-degree technical aerospace card with mouse-following radial
// spotlight, 3D tilt perspective, and 4 corner crosshair markers (+).
// ---------------------------------------------------------------------------
interface SpotlightTechCardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
  enableTilt?: boolean;
}

export function SpotlightTechCard({
  children,
  className,
  enableTilt = true,
  ...props
}: SpotlightTechCardProps) {
  const cardRef = useRef<HTMLDivElement>(null);
  const [mousePos, setMousePos] = useState<{ x: number; y: number } | null>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    setMousePos({ x, y });

    if (enableTilt) {
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;
      const tiltX = ((y - centerY) / centerY) * -4;
      const tiltY = ((x - centerX) / centerX) * 4;
      setTilt({ x: tiltX, y: tiltY });
    }
  };

  const handleMouseLeave = () => {
    setMousePos(null);
    setTilt({ x: 0, y: 0 });
  };

  return (
    <div
      ref={cardRef}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{
        transform: enableTilt
          ? `perspective(1000px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`
          : undefined,
        transition: "transform 0.15s ease-out, box-shadow 0.2s ease",
      }}
      className={cn(
        "relative chamfer-card p-6 shadow-sm border border-slate-300 transition-all duration-200 overflow-hidden",
        className
      )}
      {...props}
    >
      {/* 4 Corner Crosshairs */}
      <span className="absolute top-2 left-2 text-[10px] font-mono text-slate-400 select-none pointer-events-none">+</span>
      <span className="absolute top-2 right-2 text-[10px] font-mono text-slate-400 select-none pointer-events-none">+</span>
      <span className="absolute bottom-2 left-2 text-[10px] font-mono text-slate-400 select-none pointer-events-none">+</span>
      <span className="absolute bottom-2 right-2 text-[10px] font-mono text-slate-400 select-none pointer-events-none">+</span>

      {/* Mouse Radial Spotlight */}
      {mousePos && (
        <div
          className="pointer-events-none absolute inset-0 opacity-100 transition-opacity duration-300"
          style={{
            background: `radial-gradient(circle 260px at ${mousePos.x}px ${mousePos.y}px, rgba(2, 132, 199, 0.12), transparent 75%)`,
          }}
        />
      )}

      {/* Card Content */}
      <div className="relative z-10">{children}</div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// 2. CipherText
// Rapid 250-300ms matrix character decoding scramble animation.
// ---------------------------------------------------------------------------
interface CipherTextProps {
  text: string;
  className?: string;
  speedMs?: number;
}

const GLYPHS = "0123456789ABCDEF!@#$%^&*<>[]{}=";

export function CipherText({ text, className, speedMs = 280 }: CipherTextProps) {
  const [displayText, setDisplayText] = useState(text);

  useEffect(() => {
    let iteration = 0;
    const totalFrames = 10;
    const intervalTime = speedMs / totalFrames;

    const interval = setInterval(() => {
      setDisplayText(() =>
        text
          .split("")
          .map((char, index) => {
            if (char === " ") return " ";
            if (index < (iteration / totalFrames) * text.length) {
              return text[index];
            }
            return GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
          })
          .join("")
      );

      iteration++;
      if (iteration > totalFrames) {
        clearInterval(interval);
        setDisplayText(text);
      }
    }, intervalTime);

    return () => clearInterval(interval);
  }, [text, speedMs]);

  return <span className={cn("font-mono tracking-wider", className)}>{displayText}</span>;
}

// ---------------------------------------------------------------------------
// 3. AnimatedNumber
// Odometer-style rolling numbers with font-mono tabular-nums.
// ---------------------------------------------------------------------------
interface AnimatedNumberProps {
  value: number;
  className?: string;
  durationMs?: number;
}

export function AnimatedNumber({ value, className, durationMs = 600 }: AnimatedNumberProps) {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    let startTimestamp: number | null = null;
    const startValue = current;

    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / durationMs, 1);
      const easeOut = 1 - Math.pow(1 - progress, 3);
      setCurrent(Math.round(startValue + (value - startValue) * easeOut));

      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }, [value, durationMs]);

  return <span className={cn("font-mono tabular-nums font-semibold", className)}>{current}</span>;
}

// ---------------------------------------------------------------------------
// 4. SegmentedBar
// 20-segment gauge skewed at -18deg with pulsating red alert when < 15%.
// ---------------------------------------------------------------------------
interface SegmentedBarProps {
  value: number;
  max: number;
  segments?: number;
  className?: string;
}

export function SegmentedBar({ value, max, segments = 20, className }: SegmentedBarProps) {
  const ratio = Math.max(0, Math.min(1, max > 0 ? value / max : 0));
  const activeSegments = Math.round(ratio * segments);
  const isCritical = ratio > 0 && ratio < 0.15;

  return (
    <div className={cn("flex items-center gap-[3px]", className)}>
      {Array.from({ length: segments }).map((_, i) => {
        const isActive = i < activeSegments;
        let colorClass = "bg-slate-200";

        if (isActive) {
          if (isCritical) {
            colorClass = "bg-rose-500 animate-pulse";
          } else if (ratio < 0.35) {
            colorClass = "bg-amber-400";
          } else {
            colorClass = "bg-sky-500";
          }
        }

        return (
          <div
            key={i}
            style={{ transform: "skewX(-18deg)" }}
            className={cn("h-4 w-1.5 transition-colors duration-150 rounded-[1px]", colorClass)}
          />
        );
      })}
    </div>
  );
}

// ---------------------------------------------------------------------------
// 5. LiveCircuitPipeline
// Animated SVG PCB circuit pipeline across 4 Git CI stages with light beam particles.
// ---------------------------------------------------------------------------
export interface PipelineStep {
  label: string;
  status: "PENDING" | "RUNNING" | "PASSED" | "FAILED";
}

interface LiveCircuitPipelineProps {
  steps: PipelineStep[];
  className?: string;
}

export function LiveCircuitPipeline({ steps, className }: LiveCircuitPipelineProps) {
  return (
    <div className={cn("w-full py-4 px-2 bg-slate-900 text-white rounded border border-slate-800", className)}>
      <div className="flex items-center justify-between relative">
        {/* Animated Connecting Track */}
        <div className="absolute top-1/2 left-6 right-6 h-[2px] bg-slate-700 -translate-y-1/2 z-0 overflow-hidden">
          <div className="w-1/3 h-full bg-cyan-400 animate-beam" />
        </div>

        {/* Steps */}
        {steps.map((step, idx) => {
          let badgeColor = "bg-slate-800 text-slate-400 border-slate-600";
          if (step.status === "PASSED") badgeColor = "bg-emerald-950 text-emerald-400 border-emerald-500";
          if (step.status === "RUNNING") badgeColor = "bg-amber-950 text-amber-400 border-amber-500 animate-pulse";
          if (step.status === "FAILED") badgeColor = "bg-rose-950 text-rose-400 border-rose-500";

          return (
            <div key={idx} className="relative z-10 flex flex-col items-center gap-1.5">
              <div
                className={cn(
                  "w-8 h-8 rounded-full flex items-center justify-center font-mono text-xs border font-bold",
                  badgeColor
                )}
              >
                {idx + 1}
              </div>
              <span className="font-mono text-[10px] tracking-wider uppercase text-slate-300 font-medium">
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// 6. LiveTelemetryTicker
// Live system pulse ribbon, pulsing status LED, and real-time tournament marquee.
// ---------------------------------------------------------------------------
interface LiveTelemetryTickerProps {
  systemStatus?: "ONLINE" | "BUSY" | "DEGRADED";
  messages: string[];
  className?: string;
}

export function LiveTelemetryTicker({
  systemStatus = "ONLINE",
  messages,
  className,
}: LiveTelemetryTickerProps) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (messages.length <= 1) return;
    const interval = setInterval(() => {
      setIndex((prev) => (prev + 1) % messages.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [messages]);

  return (
    <div
      className={cn(
        "flex items-center gap-3 px-4 py-2 border-y border-slate-200 bg-slate-100/90 text-xs font-mono",
        className
      )}
    >
      {/* Pulsing Status LED */}
      <div className="flex items-center gap-1.5 shrink-0">
        <span
          className={cn(
            "w-2 h-2 rounded-full",
            systemStatus === "ONLINE"
              ? "bg-emerald-500 animate-pulse shadow-[0_0_8px_rgba(16,185,129,0.7)]"
              : systemStatus === "BUSY"
              ? "bg-amber-500 animate-pulse"
              : "bg-rose-500 animate-ping"
          )}
        />
        <span className="text-slate-700 font-bold tracking-wider">CORE: {systemStatus}</span>
      </div>

      <span className="text-slate-300 select-none">|</span>

      {/* Marquee Message */}
      <div className="overflow-hidden whitespace-nowrap text-slate-600 transition-opacity duration-300">
        <span>{messages[index] || "SYSTEM READY"}</span>
      </div>
    </div>
  );
}
