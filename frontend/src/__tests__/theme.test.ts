import { describe, it, expect } from "vitest";
import fs from "fs";
import path from "path";

describe("Theme Manifest Synchronization", () => {
  const cssPath = path.resolve(__dirname, "../app/globals.css");

  it("contains Aero Lab Bright preset color variables in globals.css", () => {
    const content = fs.readFileSync(cssPath, "utf-8");
    expect(content).toContain("--bg-canvas: #F4F7FB");
    expect(content).toContain("--bg-panel: rgba(255, 255, 255, 0.88)");
    expect(content).toContain("--bg-arena: #0B192C");
    expect(content).toContain("--text-main: #0F172A");
    expect(content).toContain("--accent-primary: #0284C7");
    expect(content).toContain("--team-1: #00F5D4");
    expect(content).toContain("--team-2: #FF2A85");
  });

  it("contains CHAMFER_45 clip-path geometry variable", () => {
    const content = fs.readFileSync(cssPath, "utf-8");
    expect(content).toContain("--card-clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px))");
  });
});
