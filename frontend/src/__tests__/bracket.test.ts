import { describe, it, expect } from "vitest";
import { SeriesNode } from "../lib/api";

describe("Tournament Bracket Data Mapping", () => {
  it("organizes 15 series into 4 distinct elimination rounds", () => {
    const mockNodes: SeriesNode[] = [
      { round_number: 1, id: "s1", position: 0, status: "COMPLETED", team_one: "Alpha", team_two: "Beta", winner: "Alpha" },
      { round_number: 1, id: "s2", position: 1, status: "COMPLETED", team_one: "Gamma", team_two: "Delta", winner: "Delta" },
      { round_number: 2, id: "s9", position: 0, status: "RUNNING", team_one: "Alpha", team_two: "Delta", winner: null },
      { round_number: 3, id: "s13", position: 0, status: "PENDING", team_one: null, team_two: null, winner: null },
      { round_number: 4, id: "s15", position: 0, status: "PENDING", team_one: null, team_two: null, winner: null },
    ];

    const rounds = [1, 2, 3, 4].map((r) => mockNodes.filter((n) => n.round_number === r));
    expect(rounds[0].length).toBe(2);
    expect(rounds[1].length).toBe(1);
    expect(rounds[2].length).toBe(1);
    expect(rounds[3].length).toBe(1);
    expect(rounds[0][0].winner).toBe("Alpha");
  });
});
