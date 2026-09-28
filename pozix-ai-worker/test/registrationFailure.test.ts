import { describe, expect, it } from "vitest";
import { registrationFailure } from "../src/registrationFailure";
import { PASSWORD_HASH_ITERATIONS } from "../src/passwordHashPolicy";

describe("registrationFailure", () => {
  it("reports only a username unique-constraint violation as unavailable", () => {
    expect(registrationFailure(new Error("UNIQUE constraint failed: users.username"))).toMatchObject({ status: 409, code: "username_unavailable" });
  });

  it("does not misreport infrastructure errors as duplicate usernames", () => {
    expect(registrationFailure(new Error("D1_ERROR: database temporarily unavailable"))).toMatchObject({ status: 503, code: "account_creation_unavailable" });
  });

  it("keeps PBKDF2 iterations within Cloudflare Workers' supported maximum", () => {
    expect(PASSWORD_HASH_ITERATIONS).toBe(100_000);
  });
});
