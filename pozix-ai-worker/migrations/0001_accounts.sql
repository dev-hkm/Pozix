CREATE TABLE users (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  password_salt TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX sessions_user_id ON sessions(user_id);

CREATE TABLE quiz_usage (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  usage_date TEXT NOT NULL,
  generation_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  PRIMARY KEY(user_id, usage_date, generation_id)
);
CREATE INDEX quiz_usage_daily ON quiz_usage(user_id, usage_date);

CREATE TRIGGER quiz_usage_daily_limit
BEFORE INSERT ON quiz_usage
WHEN NOT EXISTS (
  SELECT 1 FROM quiz_usage WHERE user_id=NEW.user_id AND usage_date=NEW.usage_date AND generation_id=NEW.generation_id
) AND (
  SELECT COUNT(*) FROM quiz_usage WHERE user_id=NEW.user_id AND usage_date=NEW.usage_date
) >= 3
BEGIN
  SELECT RAISE(ABORT, 'daily_quota_exceeded');
END;
