"use client";

import React, { useState, useEffect } from "react";
import { BracketTree } from "@/components/bracket/BracketTree";
import { CipherText } from "@/components/ui/kinetic-primitives";
import { getTournaments, getBracket, SeriesNode } from "@/lib/api";
import { Trophy, RefreshCw } from "lucide-react";

export default function BracketPage() {
  const [nodes, setNodes] = useState<SeriesNode[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchBracket = async () => {
    setLoading(true);
    try {
      const tournaments = await getTournaments();
      if (tournaments.length > 0) {
        const bracketData = await getBracket(tournaments[0].id);
        setNodes(bracketData);
      }
    } catch (e) {
      // Fallback 15-series demo structure if backend offline
      const mock: SeriesNode[] = [
        // Round 1 (8 series)
        { round_number: 1, id: "s1", position: 0, status: "COMPLETED", team_one: "CyberVanguard", team_two: "ByteBrawlers", winner: "CyberVanguard" },
        { round_number: 1, id: "s2", position: 1, status: "COMPLETED", team_one: "NeuralShock", team_two: "QuantumRift", winner: "NeuralShock" },
        { round_number: 1, id: "s3", position: 2, status: "COMPLETED", team_one: "AlgoRhythm", team_two: "GridPhantoms", winner: "AlgoRhythm" },
        { round_number: 1, id: "s4", position: 3, status: "COMPLETED", team_one: "BitShifters", team_two: "DataKnights", winner: "DataKnights" },
        { round_number: 1, id: "s5", position: 4, status: "COMPLETED", team_one: "SyntaxSirens", team_two: "LogicStorm", winner: "LogicStorm" },
        { round_number: 1, id: "s6", position: 5, status: "COMPLETED", team_one: "KernelCrafters", team_two: "StackBreakers", winner: "KernelCrafters" },
        { round_number: 1, id: "s7", position: 6, status: "COMPLETED", team_one: "PixelTitans", team_two: "CodeCommanders", winner: "PixelTitans" },
        { round_number: 1, id: "s8", position: 7, status: "COMPLETED", team_one: "VectorVipers", team_two: "ZeroDayLegion", winner: "ZeroDayLegion" },
        // Round 2 (4 series)
        { round_number: 2, id: "s9", position: 0, status: "COMPLETED", team_one: "CyberVanguard", team_two: "NeuralShock", winner: "CyberVanguard" },
        { round_number: 2, id: "s10", position: 1, status: "COMPLETED", team_one: "AlgoRhythm", team_two: "DataKnights", winner: "AlgoRhythm" },
        { round_number: 2, id: "s11", position: 2, status: "RUNNING", team_one: "LogicStorm", team_two: "KernelCrafters", winner: null },
        { round_number: 2, id: "s12", position: 3, status: "PENDING", team_one: "PixelTitans", team_two: "ZeroDayLegion", winner: null },
        // Round 3 (2 series)
        { round_number: 3, id: "s13", position: 0, status: "PENDING", team_one: "CyberVanguard", team_two: "AlgoRhythm", winner: null },
        { round_number: 3, id: "s14", position: 1, status: "PENDING", team_one: null, team_two: null, winner: null },
        // Round 4 (1 series)
        { round_number: 4, id: "s15", position: 0, status: "PENDING", team_one: null, team_two: null, winner: null },
      ];
      setNodes(mock);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBracket();
  }, []);

  return (
    <div className="space-y-6 py-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 font-mono text-xs text-sky-700 bg-sky-50 px-2.5 py-1 rounded">
            <Trophy className="w-3.5 h-3.5 text-amber-500" />
            16-TEAM SINGLE-ELIMINATION TOURNAMENT BRACKET
          </div>
          <h1 className="text-3xl font-mono font-bold text-slate-900">
            <CipherText text="LIVE CHAMPIONSHIP BRACKET" />
          </h1>
        </div>

        <button
          onClick={fetchBracket}
          disabled={loading}
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded border border-slate-300 bg-white hover:bg-slate-50 text-xs font-mono font-semibold text-slate-700 transition-colors shadow-sm self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
          REFRESH BRACKET
        </button>
      </div>

      {/* Bracket Canvas View */}
      <div className="rounded border border-slate-300 bg-white/90 shadow-sm backdrop-blur-md p-4 sm:p-6">
        <BracketTree nodes={nodes} />
      </div>
    </div>
  );
}
