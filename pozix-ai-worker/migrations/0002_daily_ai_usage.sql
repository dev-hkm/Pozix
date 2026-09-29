CREATE TABLE ai_request_usage (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  usage_date TEXT NOT NULL,
  request_id TEXT NOT NULL,
  request_type TEXT NOT NULL CHECK(request_type IN ('chat', 'quiz')),
  created_at INTEGER NOT NULL,
  PRIMARY KEY(user_id, usage_date, request_id)
);
CREATE INDEX ai_request_usage_daily ON ai_request_usage(user_id, usage_date, request_type);

CREATE TRIGGER ai_request_usage_daily_limit
BEFORE INSERT ON ai_request_usage
WHEN NOT EXISTS (
  SELECT 1 FROM ai_request_usage
  WHERE user_id=NEW.user_id AND usage_date=NEW.usage_date AND request_id=NEW.request_id
) AND (
  (NEW.request_type='chat' AND (SELECT COUNT(*) FROM ai_request_usage WHERE user_id=NEW.user_id AND usage_date=NEW.usage_date AND request_type='chat') >= 20)
  OR
  (NEW.request_type='quiz' AND (SELECT COUNT(*) FROM ai_request_usage WHERE user_id=NEW.user_id AND usage_date=NEW.usage_date AND request_type='quiz') >= 6)
)
BEGIN
  SELECT RAISE(ABORT, 'daily_ai_limit_exceeded');
END;
