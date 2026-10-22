import { describe, it, expect } from "vitest";
import * as api from "../lib/api";

describe("Admin Tournament Operations", () => {
  it("defines all 4 administrative mutation functions", () => {
    expect(typeof api.adminFreeze).toBe("function");
    expect(typeof api.adminGenerate).toBe("function");
    expect(typeof api.adminRetry).toBe("function");
    expect(typeof api.adminPublish).toBe("function");
  });
});
