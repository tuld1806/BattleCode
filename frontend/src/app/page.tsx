import React from "react";
import Link from "next/link";
import {
  SpotlightTechCard,
  CipherText,
  AnimatedNumber,
  SegmentedBar,
} from "@/components/ui/kinetic-primitives";
import { Terminal, Shield, Play, Trophy, Code, ArrowRight, Activity, Zap } from "lucide-react";

export default function LobbyPage() {
  return (
    <div className="space-y-8 py-4">
      {/* Hero Section */}
      <div className="relative overflow-hidden rounded border border-slate-300 bg-white/80 p-8 sm:p-12 shadow-sm backdrop-blur-md">
        <div className="max-w-3xl space-y-4">
          <div className="inline-flex items-center gap-2 rounded-full border border-sky-300 bg-sky-50 px-3 py-1 text-xs font-mono font-medium text-sky-800">
            <Zap className="h-3.5 w-3.5 text-sky-600 animate-pulse" />
            OFFICIAL PILOT TOURNAMENT • UET AI COCKPIT
          </div>
          <h1 className="text-3xl sm:text-5xl font-mono font-extrabold tracking-tight text-slate-900 leading-tight">
            <CipherText text="CYBER-GRID ARENA" speedMs={300} />
          </h1>
          <p className="text-sm sm:text-base text-slate-600 leading-relaxed font-sans">
            A high-speed, deterministic multi-agent strategic tournament platform. 16 engineering teams deploy autonomous algorithms in Python, C++20, and Java 21 into isolated Linux sandboxes to compete for grid dominance.
          </p>
          <div className="flex flex-wrap gap-3 pt-2">
            <Link
              href="/tournament"
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded font-mono text-xs font-bold shadow-sm transition-all"
            >
              REGISTER TEAM
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
            <Link
              href="/bracket"
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-300 rounded font-mono text-xs font-semibold transition-all"
            >
              VIEW BRACKET
              <Trophy className="w-3.5 h-3.5 text-amber-500" />
            </Link>
            <Link
              href="/rules"
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-300 rounded font-mono text-xs font-semibold transition-all"
            >
              GAME RULES & KITS
              <Code className="w-3.5 h-3.5 text-sky-600" />
            </Link>
          </div>
        </div>
      </div>

      {/* Telemetry Numbers Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <SpotlightTechCard className="space-y-2">
          <div className="flex items-center justify-between text-xs font-mono text-slate-500">
            <span>REGISTERED TEAMS</span>
            <Shield className="w-4 h-4 text-sky-600" />
          </div>
          <div className="text-3xl font-mono font-bold text-slate-900 flex items-baseline gap-1">
            <AnimatedNumber value={16} />
            <span className="text-xs font-mono text-slate-400">/ 16 SLOTS</span>
          </div>
          <SegmentedBar value={16} max={16} segments={16} />
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-2">
          <div className="flex items-center justify-between text-xs font-mono text-slate-500">
            <span>TURN TIME BUDGET</span>
            <Activity className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-3xl font-mono font-bold text-slate-900 flex items-baseline gap-1">
            <AnimatedNumber value={50} />
            <span className="text-xs font-mono text-slate-400">ms BASE</span>
          </div>
          <div className="text-[11px] font-mono text-slate-500">
            + 2,000 ms Time Bank reserve
          </div>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-2">
          <div className="flex items-center justify-between text-xs font-mono text-slate-500">
            <span>SIMULATION SPEED</span>
            <Zap className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-3xl font-mono font-bold text-slate-900 flex items-baseline gap-1">
            <AnimatedNumber value={60} />
            <span className="text-xs font-mono text-slate-400">FPS REPLAY</span>
          </div>
          <div className="text-[11px] font-mono text-slate-500">
            Full keyframe + delta stream
          </div>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-2">
          <div className="flex items-center justify-between text-xs font-mono text-slate-500">
            <span>SANDBOX MEMORY CAP</span>
            <Terminal className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="text-3xl font-mono font-bold text-slate-900 flex items-baseline gap-1">
            <AnimatedNumber value={96} />
            <span className="text-xs font-mono text-slate-400">MB RAM</span>
          </div>
          <div className="text-[11px] font-mono text-slate-500">
            bwrap + cgroups isolated
          </div>
        </SpotlightTechCard>
      </div>

      {/* Feature Showcase Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <SpotlightTechCard className="space-y-3">
          <div className="w-10 h-10 rounded bg-sky-100 flex items-center justify-center text-sky-700">
            <Code className="w-5 h-5" />
          </div>
          <h3 className="text-base font-mono font-bold text-slate-900">
            Polyglot Starter Kits
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            Write your bot in Python 3, C++20, or Java 21. Every bot consumes standard JSON observations over stdin and outputs actions on stdout.
          </p>
          <Link
            href="/rules"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-sky-600 hover:text-sky-700 font-semibold pt-1"
          >
            Read Protocol Specs &rarr;
          </Link>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-3">
          <div className="w-10 h-10 rounded bg-indigo-100 flex items-center justify-center text-indigo-700">
            <Trophy className="w-5 h-5" />
          </div>
          <h3 className="text-base font-mono font-bold text-slate-900">
            Best-of-Three Bracket
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            Double-mirrored map matches plus tiebreaker. Elimination progression happens automatically as the match queue executes on the Linux worker.
          </p>
          <Link
            href="/bracket"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-sky-600 hover:text-sky-700 font-semibold pt-1"
          >
            Inspect Bracket Tree &rarr;
          </Link>
        </SpotlightTechCard>

        <SpotlightTechCard className="space-y-3">
          <div className="w-10 h-10 rounded bg-rose-100 flex items-center justify-center text-rose-700">
            <Play className="w-5 h-5" />
          </div>
          <h3 className="text-base font-mono font-bold text-slate-900">
            Tactical Replay Theater
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            60fps Canvas 2D engine. Scrub forward/backward across 200 ticks, inspect friendly vs enemy vision with Fog-of-War toggle, and monitor action logs.
          </p>
          <Link
            href="/replay"
            className="inline-flex items-center gap-1.5 text-xs font-mono text-sky-600 hover:text-sky-700 font-semibold pt-1"
          >
            Launch Replay Theater &rarr;
          </Link>
        </SpotlightTechCard>
      </div>
    </div>
  );
}
