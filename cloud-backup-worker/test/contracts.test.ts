import { describe, expect, it } from "vitest";
import { isValidBackupPayload, isValidBackupToken, validateSharePayload } from "../src/contracts";

describe("cloud backup contracts", () => {
  it("accepts a generated Pozix token and rejects arbitrary token strings", () => {
    expect(isValidBackupToken(`hkm-${"a".repeat(36)}`)).toBe(true);
    expect(isValidBackupToken("hkm-short")).toBe(false);
    expect(isValidBackupToken(`other-${"a".repeat(36)}`)).toBe(false);
  });

  it("accepts compact JSON share payloads and caps oversized shares", () => {
    expect(validateSharePayload({
      quizJson: JSON.stringify({
        title: "Demo",
        questions: [{
          type: "true_false",
          question: "Pozix works offline",
          correctAnswer: true
        }]
      })
    }).ok).toBe(true);
    expect(validateSharePayload({
      quizJson: '{"title":"Empty","questions":[]}'
    }).ok).toBe(false);
    expect(validateSharePayload({ quizJson: "x".repeat(1_048_577) }).ok).toBe(false);
  });

  it("requires a bounded verifier, salt, and ciphertext for backups", () => {
    const valid = {
      verifier: "v".repeat(43),
      salt: "s".repeat(22),
      ciphertext: "c".repeat(64)
    };
    expect(isValidBackupPayload(valid)).toBe(true);
    expect(isValidBackupPayload({ ...valid, verifier: "short" })).toBe(false);
    expect(isValidBackupPayload({ ...valid, salt: "bad+base64" })).toBe(false);
    expect(isValidBackupPayload({ ...valid, ciphertext: "short" })).toBe(false);
    expect(isValidBackupPayload({ ...valid, ciphertext: "x".repeat(1_048_577) })).toBe(false);
  });
});
