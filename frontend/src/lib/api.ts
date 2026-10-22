const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export interface Tournament {
  id: string;
  title: string;
  status: "REGISTRATION_OPEN" | "SUBMISSION_FROZEN" | "RUNNING_MATCHES" | "COMPLETED" | "PUBLISHED";
  map_size: number;
  max_ticks: number;
  registration_deadline?: string;
  submission_deadline?: string;
  created_at: string;
}

export interface Team {
  id: string;
  name: string;
  seed: number | null;
}

export interface SeriesNode {
  round_number: number;
  id: string;
  position: number;
  status: "PENDING" | "RUNNING" | "COMPLETED";
  team_one: string | null;
  team_two: string | null;
  winner: string | null;
}

export interface StandingEntry {
  id: string;
  name: string;
  seed: number;
  series_wins: number;
}

export interface MatchEntry {
  id: string;
  series_id: string;
  game_number: number;
  status: "QUEUED" | "RUNNING" | "COMPLETED" | "SYSTEM_ERROR";
  winner_slot: 1 | 2 | null;
  total_ticks: number | null;
  error_summary: string | null;
}

export interface MatchDetail extends MatchEntry {
  replay_data_url: string | null;
}

export interface SubmissionDetail {
  id: string;
  team_id: string;
  commit_sha: string;
  language: "python" | "cpp" | "java";
  status: "PENDING" | "PASSED" | "FAILED";
  error_summary: string | null;
  created_at: string;
}

// ---------------------------------------------------------------------------
// Public API Calls
// ---------------------------------------------------------------------------
export async function getTournaments(): Promise<Tournament[]> {
  const res = await fetch(`${API_BASE}/api/public/tournaments`, { next: { revalidate: 5 } });
  if (!res.ok) throw new Error("Failed to fetch tournaments");
  return res.json();
}

export async function getTournament(id: string): Promise<Tournament | null> {
  const res = await fetch(`${API_BASE}/api/public/tournaments/${id}`, { next: { revalidate: 5 } });
  if (!res.ok) return null;
  const list = await res.json();
  return list[0] || null;
}

export async function getTournamentTeams(id: string): Promise<Team[]> {
  const res = await fetch(`${API_BASE}/api/public/tournaments/${id}/teams`, { next: { revalidate: 5 } });
  if (!res.ok) throw new Error("Failed to fetch teams");
  return res.json();
}

export async function getBracket(id: string): Promise<SeriesNode[]> {
  const res = await fetch(`${API_BASE}/api/public/tournaments/${id}/bracket`, { next: { revalidate: 5 } });
  if (!res.ok) throw new Error("Failed to fetch bracket");
  return res.json();
}

export async function getStandings(id: string): Promise<StandingEntry[]> {
  const res = await fetch(`${API_BASE}/api/public/tournaments/${id}/standings`, { next: { revalidate: 5 } });
  if (!res.ok) throw new Error("Failed to fetch standings");
  return res.json();
}

export async function getMatches(id: string): Promise<MatchEntry[]> {
  const res = await fetch(`${API_BASE}/api/public/tournaments/${id}/matches`, { next: { revalidate: 5 } });
  if (!res.ok) throw new Error("Failed to fetch matches");
  return res.json();
}

export async function getMatch(id: string): Promise<MatchDetail | null> {
  const res = await fetch(`${API_BASE}/api/public/matches/${id}`, { next: { revalidate: 5 } });
  if (!res.ok) return null;
  const list = await res.json();
  return list[0] || null;
}

export async function getSubmission(id: string): Promise<SubmissionDetail | null> {
  const res = await fetch(`${API_BASE}/api/public/submissions/${id}`);
  if (!res.ok) return null;
  const list = await res.json();
  return list[0] || null;
}

// ---------------------------------------------------------------------------
// Authenticated Student API Calls
// ---------------------------------------------------------------------------
export async function createTeam(tournamentId: string, name: string, token: string): Promise<{ id: string }> {
  const res = await fetch(`${API_BASE}/api/tournaments/${tournamentId}/teams`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to create team");
  return res.json();
}

export async function joinTeam(tournamentId: string, teamId: string, token: string): Promise<{ status: string }> {
  const res = await fetch(`${API_BASE}/api/tournaments/${tournamentId}/teams/${teamId}/join`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to join team");
  return res.json();
}

export async function submitBot(
  tournamentId: string,
  teamId: string,
  submission: { repoUrl: string; commitSha: string; language: string },
  token: string
): Promise<{ id: string }> {
  const res = await fetch(`${API_BASE}/api/tournaments/${tournamentId}/teams/${teamId}/submissions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(submission),
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to submit bot");
  return res.json();
}

// ---------------------------------------------------------------------------
// Authenticated Admin API Calls
// ---------------------------------------------------------------------------
export async function adminFreeze(tournamentId: string, token: string): Promise<{ status: string }> {
  const res = await fetch(`${API_BASE}/api/admin/tournaments/${tournamentId}/freeze`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to freeze tournament");
  return res.json();
}

export async function adminGenerate(tournamentId: string, token: string): Promise<{ status: string }> {
  const res = await fetch(`${API_BASE}/api/admin/tournaments/${tournamentId}/generate`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to generate bracket");
  return res.json();
}

export async function adminRetry(tournamentId: string, matchId: string, token: string): Promise<{ accepted: boolean }> {
  const res = await fetch(`${API_BASE}/api/admin/tournaments/${tournamentId}/matches/${matchId}/retry`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to retry match");
  return res.json();
}

export async function adminPublish(tournamentId: string, token: string): Promise<{ accepted: boolean }> {
  const res = await fetch(`${API_BASE}/api/admin/tournaments/${tournamentId}/publish`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to publish tournament");
  return res.json();
}
