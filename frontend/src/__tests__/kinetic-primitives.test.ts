import { describe, it, expect } from "vitest";
import * as Primitives from "../components/ui/kinetic-primitives";

describe("Kinetic Primitives Suite", () => {
  it("exports all 6 required kinetic components", () => {
    expect(Primitives.SpotlightTechCard).toBeDefined();
    expect(Primitives.CipherText).toBeDefined();
    expect(Primitives.AnimatedNumber).toBeDefined();
    expect(Primitives.SegmentedBar).toBeDefined();
    expect(Primitives.LiveCircuitPipeline).toBeDefined();
    expect(Primitives.LiveTelemetryTicker).toBeDefined();
  });
});
