"use client";

import React from "react";
import Link from "next/link";
import { SeriesNode } from "@/lib/api";
import { SpotlightTechCard } from "@/components/ui/kinetic-primitives";
import { Trophy, Play } from "lucide-react";
import { cn } from "@/lib/utils";

interface BracketTreeProps {
  nodes: SeriesNode[];
}

const ROUND_NAMES = [
  "ROUND OF 16",
  "QUARTERFINALS",
  "SEMIFINALS",
  "GRAND FINALS",
];

export function BracketTree({ nodes }: BracketTreeProps) {
  const rounds = [1, 2, 3, 4].map((r) =>
    nodes.filter((n) => n.round_number === r).sort((a, b) => a.position - b.position)
  );

  return (
    <div className="w-full overflow-x-auto py-6">
      <div className="min-w-[900px] grid grid-cols-4 gap-6">
        {rounds.map((roundNodes, roundIdx) => (
          <div key={roundIdx} className="space-y-4 flex flex-col">
            {/* Round Column Header */}
            <div className="text-center pb-2 border-b border-slate-200">
              <span className="font-mono text-xs font-bold tracking-widest text-slate-700 uppercase">
                {ROUND_NAMES[roundIdx]}
              </span>
              <div className="text-[10px] font-mono text-slate-400">
                {roundNodes.length} {roundNodes.length === 1 ? "SERIES" : "MATCHUPS"}
              </div>
            </div>

            {/* Series Cards */}
            <div className="flex-1 flex flex-col justify-around gap-4">
              {roundNodes.map((series) => {
                const isRunning = series.status === "RUNNING";
                const isCompleted = series.status === "COMPLETED";

                return (
                  <SpotlightTechCard
                    key={series.id}
                    enableTilt={false}
                    className={cn(
                      "p-3 space-y-2 border",
                      isRunning
                        ? "border-amber-400 bg-amber-50/50 shadow-md"
                        : isCompleted
                        ? "border-slate-300 bg-white"
                        : "border-slate-200 bg-slate-50/60 opacity-80"
                    )}
                  >
                    {/* Status & Replay Button */}
                    <div className="flex items-center justify-between text-[10px] font-mono">
                      <span
                        className={cn(
                          "px-1.5 py-0.5 rounded font-bold uppercase",
                          isRunning
                            ? "bg-amber-100 text-amber-700 animate-pulse"
                            : isCompleted
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-slate-200 text-slate-600"
                        )}
                      >
                        {series.status}
                      </span>
                      {isCompleted && (
                        <Link
                          href={`/replay?series=${series.id}`}
                          className="flex items-center gap-1 text-sky-600 hover:text-sky-800 font-bold"
                          title="Watch Series Replay"
                        >
                          <Play className="w-3 h-3 fill-current" />
                          REPLAY
                        </Link>
                      )}
                    </div>

                    {/* Team One */}
                    <div
                      className={cn(
                        "flex items-center justify-between p-1.5 rounded font-mono text-xs transition-colors",
                        series.winner && series.winner === series.team_one
                          ? "bg-sky-50 text-sky-900 font-bold border border-sky-300"
                          : "text-slate-800"
                      )}
                    >
                      <span className="truncate">{series.team_one || "TBD"}</span>
                      {series.winner && series.winner === series.team_one && (
                        <Trophy className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                      )}
                    </div>

                    {/* Team Two */}
                    <div
                      className={cn(
                        "flex items-center justify-between p-1.5 rounded font-mono text-xs transition-colors",
                        series.winner && series.winner === series.team_two
                          ? "bg-sky-50 text-sky-900 font-bold border border-sky-300"
                          : "text-slate-800"
                      )}
                    >
                      <span className="truncate">{series.team_two || "TBD"}</span>
                      {series.winner && series.winner === series.team_two && (
                        <Trophy className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                      )}
                    </div>
                  </SpotlightTechCard>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
