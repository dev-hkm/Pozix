// Cloudflare Workers' WebCrypto implementation rejects PBKDF2 above 100,000 rounds.
export const PASSWORD_HASH_ITERATIONS = 100_000;
