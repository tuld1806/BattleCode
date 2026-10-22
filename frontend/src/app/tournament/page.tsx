"use client";

import React, { useState, useEffect } from "react";
import { SpotlightTechCard, CipherText, SegmentedBar } from "@/components/ui/kinetic-primitives";
import { getTournaments, getTournamentTeams, createTeam, joinTeam, Tournament, Team } from "@/lib/api";
import { useAuthStore } from "@/store/useAuthStore";
import { Shield, Users, Plus, CheckCircle, AlertCircle } from "lucide-react";

export default function TournamentPage() {
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [selectedTournament, setSelectedTournament] = useState<Tournament | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [newTeamName, setNewTeamName] = useState("");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const { session } = useAuthStore();

  useEffect(() => {
    async function load() {
      try {
        const list = await getTournaments();
        setTournaments(list);
        if (list.length > 0) {
          setSelectedTournament(list[0]);
          const teamList = await getTournamentTeams(list[0].id);
          setTeams(teamList);
        }
      } catch (e: any) {
        // Fallback demo tournament if backend is offline
        const mockTournament: Tournament = {
          id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          title: "UET Autumn Grand Arena 2026",
          status: "REGISTRATION_OPEN",
          map_size: 16,
          max_ticks: 200,
          created_at: new Date().toISOString(),
        };
        setTournaments([mockTournament]);
        setSelectedTournament(mockTournament);
        setTeams([
          { id: "1", name: "CyberVanguard", seed: 1 },
          { id: "2", name: "NeuralShock", seed: 2 },
          { id: "3", name: "QuantumMatrix", seed: 3 },
          { id: "4", name: "AlgoRhythm", seed: 4 },
        ]);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const handleCreateTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);
    if (!newTeamName.trim()) {
      setErrorMsg("Team name cannot be empty");
      return;
    }
    if (!selectedTournament) return;
    try {
      const token = session?.token || "mock-token";
      await createTeam(selectedTournament.id, newTeamName.trim(), token);
      setSuccessMsg(`Team "${newTeamName}" created successfully!`);
      setNewTeamName("");
      const updated = await getTournamentTeams(selectedTournament.id);
      setTeams(updated);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to create team. Ensure you are signed in.");
    }
  };

  const handleJoinTeam = async (teamId: string, teamName: string) => {
    setErrorMsg(null);
    setSuccessMsg(null);
    if (!selectedTournament) return;
    try {
      const token = session?.token || "mock-token";
      await joinTeam(selectedTournament.id, teamId, token);
      setSuccessMsg(`Joined team "${teamName}" successfully!`);
    } catch (err: any) {
      setErrorMsg(err.message || "Failed to join team.");
    }
  };

  return (
    <div className="space-y-8 py-4">
      {/* Header */}
      <div className="space-y-2 border-b border-slate-200 pb-4">
        <div className="inline-flex items-center gap-2 font-mono text-xs text-sky-700 bg-sky-50 px-2.5 py-1 rounded">
          <Shield className="w-3.5 h-3.5" />
          TOURNAMENT REGISTRATION & ROSTER
        </div>
        <h1 className="text-3xl font-mono font-bold text-slate-900">
          <CipherText text={selectedTournament?.title || "TOURNAMENT ARENA"} />
        </h1>
        <div className="flex items-center gap-3 text-xs font-mono text-slate-500">
          <span>STATUS: <strong className="text-emerald-600">{selectedTournament?.status || "OPEN"}</strong></span>
          <span>•</span>
          <span>MAP: {selectedTournament?.map_size || 16}x{selectedTournament?.map_size || 16}</span>
          <span>•</span>
          <span>MAX TICKS: {selectedTournament?.max_ticks || 200}</span>
        </div>
      </div>

      {/* Notifications */}
      {errorMsg && (
        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded text-xs font-mono flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {errorMsg}
        </div>
      )}
      {successMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded text-xs font-mono flex items-center gap-2">
          <CheckCircle className="w-4 h-4 shrink-0" />
          {successMsg}
        </div>
      )}

      {/* Roster & Team Creation Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Registered Teams */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-mono font-bold text-slate-900 flex items-center gap-2">
              <Users className="w-4 h-4 text-sky-600" />
              Registered Teams ({teams.length}/16)
            </h2>
            <SegmentedBar value={teams.length} max={16} segments={16} />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {teams.map((t) => (
              <SpotlightTechCard key={t.id} className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold text-slate-500">
                    SEED #{t.seed || "—"}
                  </span>
                  <button
                    onClick={() => handleJoinTeam(t.id, t.name)}
                    className="text-[11px] font-mono px-2 py-0.5 rounded bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200 transition-colors"
                  >
                    JOIN
                  </button>
                </div>
                <div className="text-sm font-mono font-bold text-slate-900 truncate">
                  {t.name}
                </div>
              </SpotlightTechCard>
            ))}
            {teams.length === 0 && !loading && (
              <div className="p-6 text-center text-xs font-mono text-slate-500 border border-dashed border-slate-300 rounded sm:col-span-2">
                No teams registered yet. Be the first to register!
              </div>
            )}
          </div>
        </div>

        {/* Right: Team Registration Form */}
        <div className="space-y-4">
          <SpotlightTechCard className="space-y-4">
            <h3 className="text-sm font-mono font-bold text-slate-900 flex items-center gap-2">
              <Plus className="w-4 h-4 text-sky-600" />
              Register New Team
            </h3>
            <form onSubmit={handleCreateTeam} className="space-y-3">
              <div>
                <label className="block text-xs font-mono text-slate-600 mb-1">
                  TEAM NAME
                </label>
                <input
                  type="text"
                  value={newTeamName}
                  onChange={(e) => setNewTeamName(e.target.value)}
                  placeholder="e.g. CyberVanguard"
                  maxLength={100}
                  className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded focus:outline-none focus:border-sky-500 bg-white"
                />
              </div>
              <button
                type="submit"
                className="w-full py-2 bg-sky-600 hover:bg-sky-700 text-white rounded font-mono text-xs font-bold shadow-sm transition-colors"
              >
                CREATE TEAM
              </button>
            </form>
            <p className="text-[11px] text-slate-500">
              * The creator automatically becomes team captain. Other students may join using the team list.
            </p>
          </SpotlightTechCard>
        </div>
      </div>
    </div>
  );
}
