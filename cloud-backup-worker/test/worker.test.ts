import { describe, expect, it } from "vitest";
import worker from "../src/index";
import type { Env } from "../src/index";

describe("worker routes", () => {
  it("reports healthy only when D1 is reachable", async () => {
    const env = {
      POZIX_BACKUPS: {
        prepare: () => ({ first: async () => ({ ok: 1 }) })
      }
    } as unknown as Env;
    const response = await worker.fetch!(
      new Request("https://worker.test/health") as any,
      env,
      {} as ExecutionContext
    );
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({ status: "ok", database: "ok" });
  });

  it("reports unavailable when D1 cannot be queried", async () => {
    const env = {
      POZIX_BACKUPS: {
        prepare: () => ({ first: async () => { throw new Error("D1 unavailable"); } })
      }
    } as unknown as Env;
    const response = await worker.fetch!(
      new Request("https://worker.test/health") as any,
      env,
      {} as ExecutionContext
    );
    expect(response.status).toBe(503);
    await expect(response.json()).resolves.toMatchObject({
      status: "error",
      database: "unavailable"
    });
  });
});
