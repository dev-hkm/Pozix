export const MAX_PAYLOAD_BYTES = 1_048_576;

export type BackupPayload = { verifier: string; salt: string; ciphertext: string };

export type ValidationResult = { ok: true } | { ok: false; message: string };

export function isValidBackupToken(value: unknown): value is string {
  return typeof value === "string" && /^hkm-[A-Za-z0-9]{36}$/.test(value);
}

export function isValidBackupPayload(value: unknown): value is BackupPayload {
  if (typeof value !== "object" || value === null) return false;
  const payload = value as Record<string, unknown>;
  if (
    typeof payload.verifier !== "string" ||
    typeof payload.salt !== "string" ||
    typeof payload.ciphertext !== "string"
  ) return false;
  const base64Url = /^[A-Za-z0-9_-]+$/;
  if (
    payload.verifier.length !== 43 ||
    payload.salt.length !== 22 ||
    payload.ciphertext.length < 38 ||
    !base64Url.test(payload.verifier) ||
    !base64Url.test(payload.salt) ||
    !base64Url.test(payload.ciphertext)
  ) return false;
  return new TextEncoder().encode(
    `${payload.verifier}${payload.salt}${payload.ciphertext}`
  ).byteLength <= MAX_PAYLOAD_BYTES;
}

export function validateSharePayload(value: unknown): ValidationResult {
  if (typeof value !== "object" || value === null || !("quizJson" in value)) {
    return { ok: false, message: "quizJson is required" };
  }
  const quizJson = (value as { quizJson: unknown }).quizJson;
  if (typeof quizJson !== "string" || quizJson.trim().length === 0) {
    return { ok: false, message: "quizJson must be a non-empty string" };
  }
  if (new TextEncoder().encode(quizJson).byteLength > MAX_PAYLOAD_BYTES) {
    return { ok: false, message: "quizJson is too large" };
  }
  let quiz: unknown;
  try {
    quiz = JSON.parse(quizJson);
  } catch {
    return { ok: false, message: "quizJson must contain valid JSON" };
  }
  if (!isValidPozixQuiz(quiz)) {
    return { ok: false, message: "quizJson does not match the Pozix quiz schema" };
  }
  return { ok: true };
}

function isValidPozixQuiz(value: unknown): boolean {
  if (typeof value !== "object" || value === null) return false;
  const quiz = value as Record<string, unknown>;
  if (typeof quiz.title !== "string" || quiz.title.trim().length === 0) return false;
  if (!Array.isArray(quiz.questions) || quiz.questions.length === 0) return false;

  return quiz.questions.every((item) => {
    if (typeof item !== "object" || item === null) return false;
    const question = item as Record<string, unknown>;
    if (typeof question.question !== "string" || question.question.trim().length === 0) {
      return false;
    }
    if (question.type === "true_false") {
      return typeof question.correctAnswer === "boolean";
    }
    if (question.type === "single_choice") {
      if (!Array.isArray(question.options) || question.options.length < 2 || question.options.length > 6) {
        return false;
      }
      if (!question.options.every(option => typeof option === "string" && option.trim().length > 0)) {
        return false;
      }
      return Number.isInteger(question.correctIndex) &&
        (question.correctIndex as number) >= 0 &&
        (question.correctIndex as number) < question.options.length;
    }
    return false;
  });
}
