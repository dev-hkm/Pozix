import { describe, expect, it, vi } from "vitest";
import worker from "../src/index";
import { fetchNativeYoutubeTranscript, validateYoutubeRequest } from "../src/youtube";

const d1 = {
  prepare: () => ({
    bind: () => ({
      run: async () => ({}),
      first: async () => ({ count: 1 })
    }),
    run: async () => ({}),
    first: async () => ({ count: 1 })
  })
} as any;

describe("YouTube transcript gateway", () => {
  it("validates native-only video requests", () => {
    expect(validateYoutubeRequest({ videoId: "dQw4w9WgXcQ", mode: "native" }).languages).toEqual(["en"]);
    expect(() => validateYoutubeRequest({ videoId: "playlist" })).toThrow(/video id/i);
    expect(() => validateYoutubeRequest({ videoId: "dQw4w9WgXcQ", mode: "generate" })).toThrow(/native/i);
  });

  it("normalizes provider offsets and durations without inventing source timestamps", async () => {
    const fetcher = vi.fn<typeof fetch>()
      .mockResolvedValue(new Response(JSON.stringify({
        content: [
          { text: " first line ", offset: 1250, duration: 900 },
          { text: "second line", offset: 2150, duration: 1100 }
        ],
        lang: "en",
        availableLangs: ["en"]
      }), { status: 200, headers: { "Content-Type": "application/json" } }));
    const result = await fetchNativeYoutubeTranscript(
      { videoId: "dQw4w9WgXcQ", languages: ["en"], mode: "native" },
      "secret",
      fetcher
    );
    expect(result.segments).toEqual([
      { id: "s1", startMs: 1250, endMs: 2150, text: "first line" },
      { id: "s2", startMs: 2150, endMs: 3250, text: "second line" }
    ]);
  });

  it("returns a clear error when the gateway has no provider key", async () => {
    const response = await worker.fetch(
      new Request("https://worker.test/v1/youtube/transcript", {
        method: "POST",
        body: JSON.stringify({ videoId: "dQw4w9WgXcQ" })
      }),
      { POZIX_BACKUPS: d1 } as any,
      {} as ExecutionContext
    );
    expect(response.status).toBe(503);
    await expect(response.json()).resolves.toMatchObject({ code: "PROVIDER_NOT_CONFIGURED" });
  });
});
