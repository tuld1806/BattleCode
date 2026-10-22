"use client";

import React, { useState, useEffect } from "react";
import { SpotlightTechCard, CipherText, AnimatedNumber } from "@/components/ui/kinetic-primitives";
import { getTournaments, getStandings, StandingEntry } from "@/lib/api";
import { Trophy, Medal, Cpu, Shield } from "lucide-react";

export default function StandingsPage() {
  const [standings, setStandings] = useState<StandingEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const tournaments = await getTournaments();
        if (tournaments.length > 0) {
          const data = await getStandings(tournaments[0].id);
          setStandings(data);
        }
      } catch (e) {
        // Fallback demo leaderboard
        const mock: StandingEntry[] = [
          { id: "1", name: "CyberVanguard", seed: 1, series_wins: 4 },
          { id: "2", name: "AlgoRhythm", seed: 3, series_wins: 3 },
          { id: "3", name: "NeuralShock", seed: 2, series_wins: 2 },
          { id: "4", name: "LogicStorm", seed: 5, series_wins: 2 },
          { id: "5", name: "DataKnights", seed: 4, series_wins: 1 },
          { id: "6", name: "KernelCrafters", seed: 6, series_wins: 1 },
          { id: "7", name: "PixelTitans", seed: 7, series_wins: 1 },
          { id: "8", name: "ZeroDayLegion", seed: 8, series_wins: 1 },
        ];
        setStandings(mock);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <div className="space-y-8 py-4">
      {/* Header */}
      <div className="space-y-2 border-b border-slate-200 pb-4">
        <div className="inline-flex items-center gap-2 font-mono text-xs text-sky-700 bg-sky-50 px-2.5 py-1 rounded">
          <Cpu className="w-3.5 h-3.5" />
          OFFICIAL TOURNAMENT STANDINGS & ELO LEADERBOARD
        </div>
        <h1 className="text-3xl font-mono font-bold text-slate-900">
          <CipherText text="CHAMPIONSHIP STANDINGS" />
        </h1>
        <p className="text-sm text-slate-600">
          Rankings are calculated automatically based on total series victories, match differentials, and initial tournament seed.
        </p>
      </div>

      {/* Top 3 Podium Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {standings.slice(0, 3).map((team, idx) => {
          const medalColors = [
            "border-amber-400 bg-amber-50/60 text-amber-800",
            "border-slate-300 bg-slate-100/70 text-slate-800",
            "border-amber-600/40 bg-orange-50/50 text-amber-900",
          ];
          const titles = ["1ST PLACE • CHAMPION", "2ND PLACE • RUNNER-UP", "3RD PLACE • FINALIST"];

          return (
            <SpotlightTechCard key={team.id} className={`space-y-3 border-2 ${medalColors[idx]}`}>
              <div className="flex items-center justify-between">
                <span className="font-mono text-[10px] font-bold tracking-wider uppercase">
                  {titles[idx]}
                </span>
                <Medal className="w-5 h-5 text-amber-500" />
              </div>
              <div className="text-xl font-mono font-bold truncate">
                <CipherText text={team.name} />
              </div>
              <div className="flex items-center justify-between text-xs font-mono pt-2 border-t border-slate-200/60">
                <span className="text-slate-500">SERIES WINS</span>
                <span className="text-lg font-bold text-slate-900">
                  <AnimatedNumber value={team.series_wins} />
                </span>
              </div>
            </SpotlightTechCard>
          );
        })}
      </div>

      {/* Full Leaderboard Table */}
      <div className="overflow-x-auto rounded border border-slate-300 bg-white shadow-sm">
        <table className="w-full text-left font-mono text-xs">
          <thead className="bg-slate-100 border-b border-slate-200 text-slate-600 uppercase tracking-wider">
            <tr>
              <th className="p-3 w-16 text-center">RANK</th>
              <th className="p-3">TEAM NAME</th>
              <th className="p-3 w-28 text-center">SEED</th>
              <th className="p-3 w-36 text-center">SERIES WINS</th>
              <th className="p-3 w-32 text-center">STATUS</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 text-slate-800">
            {standings.map((team, idx) => (
              <tr key={team.id} className="hover:bg-slate-50 transition-colors">
                <td className="p-3 text-center font-bold text-slate-600">
                  #{idx + 1}
                </td>
                <td className="p-3 font-bold text-slate-900 flex items-center gap-2">
                  <Shield className="w-3.5 h-3.5 text-sky-600" />
                  {team.name}
                </td>
                <td className="p-3 text-center text-slate-500">
                  #{team.seed}
                </td>
                <td className="p-3 text-center font-bold text-sky-700">
                  {team.series_wins}
                </td>
                <td className="p-3 text-center">
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                    ACTIVE
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
