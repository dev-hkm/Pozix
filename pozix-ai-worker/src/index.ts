import { registrationFailure } from "./registrationFailure";
import { PASSWORD_HASH_ITERATIONS } from "./passwordHashPolicy";
import { DAILY_CHAT_TURN_LIMIT, DAILY_QUIZ_COMPLETION_LIMIT, DAILY_QUIZ_GENERATION_LIMIT, MAX_COMPLETION_TOKENS, MAX_REQUEST_BODY_BYTES, isValidRequestId } from "./usagePolicy";

const providers = {
  generalcompute: { upstream: "https://api.generalcompute.com/v1/chat/completions", model: "minimax-m2.7", secret: "GENERALCOMPUTE_API_KEY", tokenLimitField: "max_tokens" },
  openai: { upstream: "https://api.openai.com/v1/chat/completions", model: "gpt-6-luna", secret: "OPENAI_API_KEY", tokenLimitField: "max_completion_tokens" },
  openrouter: { upstream: "https://openrouter.ai/api/v1/chat/completions", model: "z-ai/glm-5.3-flash", secret: "OPENROUTER_API_KEY", tokenLimitField: "max_completion_tokens" },
} as const;
type Provider = keyof typeof providers;

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET,POST,DELETE,OPTIONS",
  "access-control-allow-headers": "authorization,content-type,x-pozix-generation-id,x-pozix-request-id",
  "access-control-max-age": "86400",
};
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), {
  status, headers: { ...cors, "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
});
const now = () => Math.floor(Date.now() / 1000);
const encode = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes));
const decode = (value: string) => Uint8Array.from(atob(value), (c) => c.charCodeAt(0));
const random = (size: number) => crypto.getRandomValues(new Uint8Array(size));
async function digest(value: string) {
  return encode(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))));
}
async function passwordHash(password: string, salt: Uint8Array) {
  const material = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
  const result = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt: Uint8Array.from(salt).buffer, iterations: PASSWORD_HASH_ITERATIONS }, material, 256);
  return encode(new Uint8Array(result));
}
function localDate() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
}
function validUsername(value: unknown): value is string {
  return typeof value === "string" && /^[a-zA-Z0-9_.-]{3,32}$/.test(value);
}
async function authenticate(request: Request, env: Env) {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "").trim();
  if (!token || token.length < 32) return null;
  return env.DB.prepare("SELECT u.id,u.display_name,u.username FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>? LIMIT 1")
    .bind(await digest(token), now()).first<{ id: string; display_name: string; username: string }>();
}
async function issueSession(env: Env, userId: string) {
  const token = encode(random(32));
  const stamp = now();
  await env.DB.prepare("INSERT INTO sessions(token_hash,user_id,expires_at,created_at) VALUES(?,?,?,?)")
    .bind(await digest(token), userId, stamp + 30 * 86400, stamp).run();
  return token;
}
async function authRoute(request: Request, env: Env, path: string) {
  const body = await request.json().catch(() => null) as Record<string, unknown> | null;
  if (!body) return json({ error: { message: "Invalid JSON body" } }, 400);
  if (path === "/v1/auth/register") {
    const name = typeof body.displayName === "string" ? body.displayName.trim() : "";
    const username = body.username;
    const password = body.password;
    if (name.length < 1 || name.length > 60 || !validUsername(username) || typeof password !== "string" || password.length < 10 || password.length > 128) {
      return json({ error: { message: "Enter a display name, a 3-32 character username, and a password of at least 10 characters." } }, 400);
    }
    const salt = random(16);
    const id = crypto.randomUUID();
    let hash: string;
    try {
      hash = await passwordHash(password, salt);
    } catch {
      return json({ error: { message: "Account security setup failed. Please try again." }, code: "password_hash_failed" }, 503);
    }
    try {
      await env.DB.prepare("INSERT INTO users(id,display_name,username,password_salt,password_hash,created_at) VALUES(?,?,?,?,?,?)")
        .bind(id, name, username, encode(salt), hash, now()).run();
    } catch (error) {
      const failure = registrationFailure(error);
      return json({ error: { message: failure.message }, code: failure.code }, failure.status);
    }
    return json({ token: await issueSession(env, id), displayName: name, username });
  }
  if (path === "/v1/auth/login") {
    if (!validUsername(body.username) || typeof body.password !== "string" || body.password.length > 128) return json({ error: { message: "Invalid username or password." } }, 400);
    const user = await env.DB.prepare("SELECT id,display_name,username,password_salt,password_hash FROM users WHERE username=? COLLATE NOCASE LIMIT 1")
      .bind(body.username).first<{ id: string; display_name: string; username: string; password_salt: string; password_hash: string }>();
    const salt = user ? decode(user.password_salt) : new Uint8Array(16);
    const candidate = await passwordHash(body.password, salt);
    if (!user || candidate !== user.password_hash) return json({ error: { message: "Invalid username or password." } }, 401);
    return json({ token: await issueSession(env, user.id), displayName: user.display_name, username: user.username });
  }
  return null;
}

async function route(request: Request, env: Env): Promise<Response> {
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
  const url = new URL(request.url);
  if (url.pathname === "/health") return json({ ok: true, service: "pozix-ai-gateway" });
  if (url.pathname === "/account-deletion" && request.method === "GET") return new Response(`<!doctype html><meta name="viewport" content="width=device-width"><title>Pozix account deletion</title><main style="font:16px system-ui;max-width:680px;margin:10vh auto;padding:24px"><h1>Delete your Pozix account</h1><p>This permanently removes your Pozix AI account, profile, sessions and quota records. Local quizzes and chat history on your device are not uploaded to this service and will not be affected.</p><form id="delete-form"><label>Username<br><input name="username" autocomplete="username" required></label><p><label>Password<br><input name="password" type="password" autocomplete="current-password" required></label></p><button>Delete account permanently</button><p id="status" role="status"></p></form><script>const form=document.getElementById('delete-form'),status=document.getElementById('status');form.addEventListener('submit',async e=>{e.preventDefault();if(!confirm('Permanently delete this Pozix AI account?'))return;const r=await fetch('/account-deletion',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(Object.fromEntries(new FormData(form)))});const d=await r.json();status.textContent=r.ok?'Account and associated server data deleted.':(d.error?.message||'Unable to delete account.');if(r.ok)form.reset()})</script></main>`, { headers: { "content-type": "text/html; charset=utf-8" } });
  if (url.pathname === "/account-deletion" && request.method === "POST") {
    const allowed = await env.AUTH_LIMITER.limit({ key: request.headers.get("cf-connecting-ip") ?? "unknown" });
    if (!allowed.success) return json({ error: { message: "Too many account requests. Wait a minute and retry." } }, 429);
    const body = await request.json().catch(() => null) as Record<string, unknown> | null;
    if (!body || !validUsername(body.username) || typeof body.password !== "string") return json({ error: { message: "Enter your account username and password." } }, 400);
    const account = await env.DB.prepare("SELECT id,password_salt,password_hash FROM users WHERE username=? COLLATE NOCASE LIMIT 1").bind(body.username).first<{ id: string; password_salt: string; password_hash: string }>();
    const candidate = await passwordHash(body.password, account ? decode(account.password_salt) : new Uint8Array(16));
    if (!account || candidate !== account.password_hash) return json({ error: { message: "Invalid username or password." } }, 401);
    await env.DB.prepare("DELETE FROM users WHERE id=?").bind(account.id).run();
    return json({ ok: true });
  }
  if (request.method !== "POST" && !(request.method === "GET" && url.pathname === "/v1/auth/me")) return json({ error: { message: "Not found" } }, 404);
  if (url.pathname === "/v1/auth/register" || url.pathname === "/v1/auth/login") {
    const allowed = await env.AUTH_LIMITER.limit({ key: request.headers.get("cf-connecting-ip") ?? "unknown" });
    if (!allowed.success) return json({ error: { message: "Too many sign-in attempts. Wait a minute and retry." } }, 429);
    const result = await authRoute(request, env, url.pathname);
    return result ?? json({ error: { message: "Invalid request" } }, 400);
  }

  const user = await authenticate(request, env);
  if (!user) return json({ error: { message: "Sign in to use Pozix AI." }, code: "unauthorized" }, 401);
  if (url.pathname.includes("/chat/completions")) {
    const allowed = await env.AI_LIMITER.limit({ key: user.id });
    if (!allowed.success) return json({ error: { message: "Too many AI requests. Wait a minute and retry." } }, 429);
  }
  if (url.pathname === "/v1/auth/me") return json({ displayName: user.display_name, username: user.username });
  if (url.pathname === "/v1/auth/logout") {
    const bearer = request.headers.get("authorization")!.replace(/^Bearer\s+/i, "").trim();
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash=?").bind(await digest(bearer)).run();
    return json({ ok: true });
  }
  if (url.pathname === "/v1/account/delete") {
    await env.DB.prepare("DELETE FROM users WHERE id=?").bind(user.id).run();
    return json({ ok: true });
  }

  const match = url.pathname.match(/^\/v1\/(generalcompute|openai|openrouter)\/chat\/completions$/);
  if (!match) return json({ error: { message: "Not found" } }, 404);
  const provider = match[1] as Provider;
  const config = providers[provider];
  const declaredLength = Number(request.headers.get("content-length") ?? 0);
  if (declaredLength > MAX_REQUEST_BODY_BYTES) return json({ error: { message: "Chat request is too large." } }, 413);
  const input = await request.json().catch(() => null) as Record<string, unknown> | null;
  if (!input || !Array.isArray(input.messages) || input.messages.length > 40 || new TextEncoder().encode(JSON.stringify(input)).length > MAX_REQUEST_BODY_BYTES) {
    return json({ error: { message: "Invalid or oversized chat request." } }, 400);
  }
  const wantsQuiz = request.headers.has("x-pozix-generation-id");
  const generationId = request.headers.get("x-pozix-generation-id");
  const requestId = request.headers.get("x-pozix-request-id");
  if (!isValidRequestId(requestId)) return json({ error: { message: "Missing or invalid AI request ID." } }, 400);
  if (wantsQuiz && (!generationId || !/^[a-zA-Z0-9-]{8,80}$/.test(generationId))) return json({ error: { message: "Invalid generation ID." } }, 400);
  const key = (env as unknown as Record<string, unknown>)[config.secret];
  if (typeof key !== "string" || !key) return json({ error: { message: `${config.secret} is not configured on the Worker.` }, code: "provider_unconfigured" }, 503);
  const usageDate = localDate();
  try {
    await env.DB.prepare("INSERT OR IGNORE INTO ai_request_usage(user_id,usage_date,request_id,request_type,created_at) VALUES(?,?,?,?,?)")
      .bind(user.id, usageDate, requestId, wantsQuiz ? "quiz" : "chat", now()).run();
    const existing = await env.DB.prepare("SELECT request_type FROM ai_request_usage WHERE user_id=? AND usage_date=? AND request_id=?")
      .bind(user.id, usageDate, requestId).first<{ request_type: string }>();
    if (existing) return json({ error: { message: "AI request ID has already been used." }, code: "duplicate_request" }, 409);
  } catch {
    const requestType = wantsQuiz ? "quiz" : "chat";
    const usageCount = await env.DB.prepare("SELECT COUNT(*) AS count FROM ai_request_usage WHERE user_id=? AND usage_date=? AND request_type=?")
      .bind(user.id, usageDate, requestType).first<{ count: number }>().catch(() => null);
    const limit = wantsQuiz ? DAILY_QUIZ_COMPLETION_LIMIT : DAILY_CHAT_TURN_LIMIT;
    if ((usageCount?.count ?? 0) >= limit) {
      const message = wantsQuiz
        ? `Daily AI completion safety limit reached (${limit} quiz completions).`
        : `Daily limit reached: ${limit} AI chat requests.`;
      return json({ error: { message }, code: "daily_quota_exceeded" }, 429);
    }
    return json({ error: { message: "Unable to reserve daily AI quota. Please retry." } }, 503);
  }
  if (wantsQuiz) {
    try {
      const result = await env.DB.prepare("INSERT OR IGNORE INTO quiz_usage(user_id,usage_date,generation_id,created_at) VALUES(?,?,?,?)")
        .bind(user.id, usageDate, generationId!, now()).run();
      if (result.meta.changes === 0) {
        const existing = await env.DB.prepare("SELECT 1 FROM quiz_usage WHERE user_id=? AND usage_date=? AND generation_id=?")
          .bind(user.id, usageDate, generationId!).first();
        if (!existing) return json({ error: { message: `Daily limit reached: ${DAILY_QUIZ_GENERATION_LIMIT} quiz generations per day.` }, code: "daily_quota_exceeded" }, 429);
      }
    } catch {
      const count = await env.DB.prepare("SELECT COUNT(*) AS count FROM quiz_usage WHERE user_id=? AND usage_date=?").bind(user.id, usageDate).first<{ count: number }>();
      const existing = await env.DB.prepare("SELECT 1 FROM quiz_usage WHERE user_id=? AND usage_date=? AND generation_id=?").bind(user.id, usageDate, generationId!).first();
      if ((count?.count ?? 0) >= DAILY_QUIZ_GENERATION_LIMIT && !existing) return json({ error: { message: `Daily limit reached: ${DAILY_QUIZ_GENERATION_LIMIT} quiz generations per day.` }, code: "daily_quota_exceeded" }, 429);
      return json({ error: { message: "Unable to reserve quiz quota. Please retry." } }, 503);
    }
  }
  input.model = config.model;
  input.stream = true;
  delete input.max_tokens;
  delete input.max_completion_tokens;
  input[config.tokenLimitField] = MAX_COMPLETION_TOKENS;
  const upstream = await fetch(config.upstream, {
    method: "POST", headers: { "authorization": `Bearer ${key}`, "content-type": "application/json", "accept": "text/event-stream" },
    body: JSON.stringify(input), signal: AbortSignal.timeout(125_000),
  }).catch(() => null);
  if (!upstream) return json({ error: { message: "AI provider could not be reached." } }, 502);
  if (!upstream.ok) {
    const details = await upstream.text();
    return new Response(JSON.stringify({ error: { message: `AI provider error (${upstream.status}): ${details.slice(0, 600)}` } }), {
      status: upstream.status, headers: { ...cors, "content-type": "application/json", "cache-control": "no-store" },
    });
  }
  return new Response(upstream.body, { status: 200, headers: { ...cors, "content-type": "text/event-stream; charset=utf-8", "cache-control": "no-cache, no-transform", "x-accel-buffering": "no" } });
}

export default {
  fetch(request: Request, env: Env): Promise<Response> {
    return route(request, env).catch(() => json({ error: { message: "Internal Worker error." } }, 500));
  },
};
