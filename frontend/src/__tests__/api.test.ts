import { describe, it, expect } from "vitest";
import * as api from "../lib/api";

describe("API Client Suite", () => {
  it("defines public and authenticated API endpoints", () => {
    expect(typeof api.getTournaments).toBe("function");
    expect(typeof api.getTournament).toBe("function");
    expect(typeof api.getBracket).toBe("function");
    expect(typeof api.getStandings).toBe("function");
    expect(typeof api.getMatches).toBe("function");
    expect(typeof api.createTeam).toBe("function");
    expect(typeof api.submitBot).toBe("function");
  });
});
