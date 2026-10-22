import { describe, it, expect } from "vitest";

export interface Unit {
  id: number;
  team: number;
  type: string;
  x: number;
  y: number;
  hp: number;
}

export interface ReplayState {
  tick: number;
  units: Record<number, Unit>;
  resources: Record<number, number>;
  visibility: Record<number, number[]>;
}

export interface ReplayDelta {
  tick: number;
  upserts: Record<number, Unit>;
  removed: number[];
  resources: Record<number, number>;
  visibility: Record<number, number[]>;
}

export function applyDelta(before: ReplayState, delta: ReplayDelta): ReplayState {
  const nextUnits = { ...before.units };
  for (const id of delta.removed) {
    delete nextUnits[id];
  }
  for (const [id, unit] of Object.entries(delta.upserts)) {
    nextUnits[Number(id)] = unit;
  }
  return {
    tick: delta.tick,
    units: nextUnits,
    resources: delta.resources,
    visibility: delta.visibility,
  };
}

describe("Replay State Delta Reconstruction", () => {
  it("reconstructs subsequent state from initial state and delta", () => {
    const initialState: ReplayState = {
      tick: 0,
      units: {
        1: { id: 1, team: 1, type: "CORE", x: 0, y: 0, hp: 100 },
        2: { id: 2, team: 2, type: "CORE", x: 15, y: 15, hp: 100 },
      },
      resources: { 1: 100, 2: 100 },
      visibility: { 1: [0, 1, 16], 2: [255, 254, 239] },
    };

    const delta: ReplayDelta = {
      tick: 1,
      upserts: {
        3: { id: 3, team: 1, type: "WORKER", x: 1, y: 0, hp: 25 },
      },
      removed: [],
      resources: { 1: 80, 2: 100 },
      visibility: { 1: [0, 1, 2, 16, 17], 2: [255, 254, 239] },
    };

    const nextState = applyDelta(initialState, delta);
    expect(nextState.tick).toBe(1);
    expect(nextState.units[3]).toBeDefined();
    expect(nextState.units[3].type).toBe("WORKER");
    expect(nextState.resources[1]).toBe(80);
  });

  it("correctly removes destroyed units from state", () => {
    const state: ReplayState = {
      tick: 10,
      units: {
        1: { id: 1, team: 1, type: "CORE", x: 0, y: 0, hp: 100 },
        3: { id: 3, team: 1, type: "WORKER", x: 1, y: 0, hp: 0 },
      },
      resources: { 1: 80, 2: 100 },
      visibility: { 1: [0], 2: [255] },
    };

    const delta: ReplayDelta = {
      tick: 11,
      upserts: {},
      removed: [3],
      resources: { 1: 90, 2: 100 },
      visibility: { 1: [0], 2: [255] },
    };

    const nextState = applyDelta(state, delta);
    expect(nextState.units[3]).toBeUndefined();
    expect(Object.keys(nextState.units).length).toBe(1);
  });
});
