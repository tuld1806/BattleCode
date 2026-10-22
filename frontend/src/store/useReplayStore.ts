import { create } from "zustand";

export interface ReplayUnit {
  id: number;
  team: number;
  type: "CORE" | "WORKER" | "SCOUT" | "TANK";
  x: number;
  y: number;
  hp: number;
}

export interface ReplayState {
  tick: number;
  units: Record<number, ReplayUnit>;
  resources: Record<number, number>;
  visibility: Record<number, number[]>;
}

export interface ReplayMap {
  size: number;
  obstacles: number[];
  resourceNodes: number[];
}

export interface ReplayDocument {
  formatVersion: string;
  matchId: string;
  seed: number;
  config: {
    size: number;
    maxTicks: number;
  };
  teams: { id: number; name: string }[];
  map: ReplayMap;
  initialState: ReplayState;
  ticks: {
    tick: number;
    keyframe?: ReplayState;
    delta?: {
      tick: number;
      upserts: Record<number, ReplayUnit>;
      removed: number[];
      resources: Record<number, number>;
      visibility: Record<number, number[]>;
    };
    events?: string[];
    actions?: Record<number, any[]>;
    observations?: Record<number, any>;
    debugLogs?: Record<number, string>;
  }[];
}

export type FogMode = "OMNISCIENT" | "TEAM_1" | "TEAM_2";

interface ReplayStoreState {
  replay: ReplayDocument | null;
  currentTick: number;
  isPlaying: boolean;
  playbackSpeed: 1 | 2 | 4;
  fogMode: FogMode;
  selectedUnitId: number | null;

  setReplay: (replay: ReplayDocument) => void;
  setTick: (tick: number) => void;
  play: () => void;
  pause: () => void;
  togglePlay: () => void;
  setSpeed: (speed: 1 | 2 | 4) => void;
  setFogMode: (mode: FogMode) => void;
  selectUnit: (id: number | null) => void;
}

export const useReplayStore = create<ReplayStoreState>((set) => ({
  replay: null,
  currentTick: 0,
  isPlaying: false,
  playbackSpeed: 1,
  fogMode: "OMNISCIENT",
  selectedUnitId: null,

  setReplay: (replay) => set({ replay, currentTick: 0, isPlaying: false, selectedUnitId: null }),
  setTick: (currentTick) => set({ currentTick }),
  play: () => set({ isPlaying: true }),
  pause: () => set({ isPlaying: false }),
  togglePlay: () => set((state) => ({ isPlaying: !state.isPlaying })),
  setSpeed: (playbackSpeed) => set({ playbackSpeed }),
  setFogMode: (fogMode) => set({ fogMode }),
  selectUnit: (selectedUnitId) => set({ selectedUnitId }),
}));
