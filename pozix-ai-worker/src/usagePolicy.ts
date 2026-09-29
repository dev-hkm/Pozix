export const DAILY_CHAT_TURN_LIMIT = 20;
export const DAILY_QUIZ_GENERATION_LIMIT = 3;
export const DAILY_QUIZ_COMPLETION_LIMIT = 6;
export const MAX_REQUEST_BODY_BYTES = 262_144;
export const MAX_COMPLETION_TOKENS = 2048;

export function isValidRequestId(value: string | null): value is string {
  return value !== null && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}
