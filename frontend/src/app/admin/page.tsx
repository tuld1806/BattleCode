"use client";

import React, { useState, useEffect } from "react";
import {
  SpotlightTechCard,
  CipherText,
  SegmentedBar,
} from "@/components/ui/kinetic-primitives";
import {
  getTournaments,
  adminFreeze,
  adminGenerate,
  adminRetry,
  adminPublish,
  Tournament,
} from "@/lib/api";
import { useAuthStore } from "@/store/useAuthStore";
import { Shield, Lock, Play, RefreshCw, Send, CheckCircle, AlertCircle, AlertTriangle } from "lucide-react";

export default function AdminPage() {
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [selectedTournament, setSelectedTournament] = useState<Tournament | null>(null);
  const [loading, setLoading] = useState(true);
  const [matchIdToRetry, setMatchIdToRetry] = useState("");
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const { session } = useAuthStore();

  const fetchTournaments = async () => {
    setLoading(true);
    try {
      const list = await getTournaments();
      setTournaments(list);
      if (list.length > 0) setSelectedTournament(list[0]);
    } catch (e) {
      // Mock tournament for preview
      const mock: Tournament = {
        id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        title: "UET Autumn Grand Arena 2026",
        status: "REGISTRATION_OPEN",
        map_size: 16,
        max_ticks: 200,
        created_at: new Date().toISOString(),
      };
      setTournaments([mock]);
      setSelectedTournament(mock);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTournaments();
  }, []);

  const handleAction = async (actionFn: () => Promise<any>, successText: string) => {
    setErrorMsg(null);
    setActionStatus(null);
    try {
      await actionFn();
      setActionStatus(successText);
      await fetchTournaments();
    } catch (err: any) {
      setErrorMsg(err.message || "Admin action failed. Ensure you possess ADMIN privileges.");
    }
  };

  const token = session?.token || "mock-admin-token";

  return (
    <div className="space-y-8 py-4">
      {/* Header */}
      <div className="space-y-2 border-b border-slate-200 pb-4">
        <div className="inline-flex items-center gap-2 font-mono text-xs text-rose-700 bg-rose-50 px-2.5 py-1 rounded">
          <Shield className="w-3.5 h-3.5" />
          SYSTEM ADMINISTRATION COCKPIT
        </div>
        <h1 className="text-3xl font-mono font-bold text-slate-900">
          <CipherText text="TOURNAMENT COMMAND" />
        </h1>
        <p className="text-sm text-slate-600">
          Supervisory control for tournament lifecycle: registration freeze, 16-team bracket generation, match retry, and public standings release.
        </p>
      </div>

      {/* Notifications */}
      {actionStatus && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded text-xs font-mono flex items-center gap-2">
          <CheckCircle className="w-4 h-4 shrink-0" />
          {actionStatus}
        </div>
      )}
      {errorMsg && (
        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded text-xs font-mono flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {errorMsg}
        </div>
      )}

      {/* Active Tournament Status Overview */}
      <SpotlightTechCard className="space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-200 pb-3">
          <div>
            <span className="text-[10px] font-mono text-slate-500 uppercase">SELECTED TOURNAMENT</span>
            <div className="text-lg font-mono font-bold text-slate-900">
              {selectedTournament?.title || "No Tournament Selected"}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-1 rounded text-xs font-mono font-bold bg-sky-100 text-sky-800 uppercase">
              STATUS: {selectedTournament?.status || "UNKNOWN"}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs font-mono">
          <div>
            <span className="text-slate-500">MAP SIZE:</span>
            <div className="font-bold text-slate-900">{selectedTournament?.map_size || 16}x16</div>
          </div>
          <div>
            <span className="text-slate-500">MAX TICKS:</span>
            <div className="font-bold text-slate-900">{selectedTournament?.max_ticks || 200}</div>
          </div>
          <div>
            <span className="text-slate-500">POOLER LIMIT:</span>
            <div className="font-bold text-emerald-600">2 HIKARI CONNECTIONS</div>
          </div>
          <div>
            <span className="text-slate-500">WORKER CONCURRENCY:</span>
            <div className="font-bold text-slate-900">1 MATCH / WORKER</div>
          </div>
        </div>
      </SpotlightTechCard>

      {/* 4 Core Lifecycle Actions Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Step 1: Freeze Submissions */}
        <SpotlightTechCard className="space-y-3">
          <div className="flex items-center gap-2 text-slate-900 font-mono font-bold text-sm">
            <Lock className="w-4 h-4 text-amber-500" />
            1. Freeze Submissions
          </div>
          <p className="text-xs text-slate-600">
            Locks student submissions and stops new registration. Pinned commit SHAs become permanently immutable.
          </p>
          <button
            onClick={() =>
              selectedTournament &&
              handleAction(
                () => adminFreeze(selectedTournament.id, token),
                "Submissions frozen successfully. Status: SUBMISSION_FROZEN"
              )
            }
            className="w-full py-2 bg-amber-600 hover:bg-amber-700 text-white rounded font-mono text-xs font-bold transition-colors shadow-sm"
          >
            FREEZE REGISTRATION
          </button>
        </SpotlightTechCard>

        {/* Step 2: Generate Bracket */}
        <SpotlightTechCard className="space-y-3">
          <div className="flex items-center gap-2 text-slate-900 font-mono font-bold text-sm">
            <Play className="w-4 h-4 text-sky-600" />
            2. Generate 16-Team Bracket
          </div>
          <p className="text-xs text-slate-600">
            Seeds 16 validated teams into the 4-round bracket, queues initial Round of 16 matches, and signals the Linux worker pool.
          </p>
          <button
            onClick={() =>
              selectedTournament &&
              handleAction(
                () => adminGenerate(selectedTournament.id, token),
                "16-team bracket generated and queued. Status: RUNNING_MATCHES"
              )
            }
            className="w-full py-2 bg-sky-600 hover:bg-sky-700 text-white rounded font-mono text-xs font-bold transition-colors shadow-sm"
          >
            GENERATE BRACKET & QUEUE MATCHES
          </button>
        </SpotlightTechCard>

        {/* Step 3: Match Recovery */}
        <SpotlightTechCard className="space-y-3">
          <div className="flex items-center gap-2 text-slate-900 font-mono font-bold text-sm">
            <RefreshCw className="w-4 h-4 text-indigo-600" />
            3. Retry Match on System Error
          </div>
          <p className="text-xs text-slate-600">
            Safely re-queues a match if a worker crashed or encountered an infrastructure error without corrupting bracket state.
          </p>
          <div className="flex gap-2">
            <input
              type="text"
              value={matchIdToRetry}
              onChange={(e) => setMatchIdToRetry(e.target.value)}
              placeholder="Match UUID to retry"
              className="flex-1 px-3 py-1.5 text-xs font-mono border border-slate-300 rounded focus:outline-none focus:border-sky-500 bg-white"
            />
            <button
              onClick={() =>
                selectedTournament &&
                matchIdToRetry &&
                handleAction(
                  () => adminRetry(selectedTournament.id, matchIdToRetry.trim(), token),
                  `Match ${matchIdToRetry} successfully re-queued`
                )
              }
              className="px-4 py-1.5 bg-slate-800 hover:bg-slate-900 text-white rounded font-mono text-xs font-bold transition-colors shadow-sm"
            >
              RETRY
            </button>
          </div>
        </SpotlightTechCard>

        {/* Step 4: Publish Results */}
        <SpotlightTechCard className="space-y-3">
          <div className="flex items-center gap-2 text-slate-900 font-mono font-bold text-sm">
            <Send className="w-4 h-4 text-emerald-600" />
            4. Publish Tournament Standings
          </div>
          <p className="text-xs text-slate-600">
            Publishes official champion standings and unlocks public replay download URLs for students and spectators.
          </p>
          <button
            onClick={() =>
              selectedTournament &&
              handleAction(
                () => adminPublish(selectedTournament.id, token),
                "Tournament published! Replay URLs now accessible publicly."
              )
            }
            className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded font-mono text-xs font-bold transition-colors shadow-sm"
          >
            PUBLISH STANDINGS & REPLAYS
          </button>
        </SpotlightTechCard>
      </div>
    </div>
  );
}
