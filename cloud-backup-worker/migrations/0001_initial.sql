CREATE TABLE IF NOT EXISTS backups (
  token TEXT PRIMARY KEY,
  verifier TEXT NOT NULL,
  salt TEXT NOT NULL,
  ciphertext TEXT NOT NULL,
  updated_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS shares (
  id TEXT PRIMARY KEY,
  quiz_json TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_shares_expiry ON shares(expires_at);
