import { describe, expect, it } from "vitest";
import { DAILY_CHAT_TURN_LIMIT, DAILY_QUIZ_COMPLETION_LIMIT, MAX_REQUEST_BODY_BYTES, MAX_COMPLETION_TOKENS, isValidRequestId } from "../src/usagePolicy";

describe("AI usage policy", () => {
  it("caps daily chat turns separately from quiz generations", () => {
    expect(DAILY_CHAT_TURN_LIMIT).toBe(20);
    expect(DAILY_QUIZ_COMPLETION_LIMIT).toBe(6);
  });

  it("bounds request size and generated output", () => {
    expect(MAX_REQUEST_BODY_BYTES).toBe(262_144);
    expect(MAX_COMPLETION_TOKENS).toBe(2048);
  });

  it("accepts UUID request identifiers and rejects missing or malformed identifiers", () => {
    expect(isValidRequestId("123e4567-e89b-42d3-a456-426614174000")).toBe(true);
    expect(isValidRequestId("not-a-request-id")).toBe(false);
    expect(isValidRequestId(null)).toBe(false);
  });
});
