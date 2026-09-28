export function registrationFailure(error: unknown) {
  const message = error instanceof Error ? error.message : String(error);
  if (/UNIQUE constraint failed:\s*users\.username/i.test(message)) {
    return { status: 409, code: "username_unavailable", message: "Username is unavailable." } as const;
  }
  return { status: 503, code: "account_creation_unavailable", message: "Account creation is temporarily unavailable. Please try again." } as const;
}
