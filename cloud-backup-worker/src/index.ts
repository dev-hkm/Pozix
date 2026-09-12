import {
  isValidBackupPayload,
  isValidBackupToken,
  validateSharePayload
} from "./contracts";
import {
  fetchNativeYoutubeTranscript,
  validateYoutubeRequest,
  YoutubeTranscriptError
} from "./youtube";

export interface Env {
  POZIX_BACKUPS: D1Database;
  SUPADATA_API_KEY?: string;
}

type BackupBody = { verifier: string; salt: string; ciphertext: string };
const json = (value: unknown, status = 200, cacheControl = "no-store") => Response.json(value, { status, headers: { "Cache-Control": cacheControl } });
const body = async <T>(request: Request): Promise<T | null> => { try { return await request.json() as T; } catch { return null; } };
const equal = (a: string, b: string) => { if (a.length !== b.length) return false; let r=0; for(let i=0;i<a.length;i++) r |= a.charCodeAt(i)^b.charCodeAt(i); return r===0; };
const shareId = () => crypto.randomUUID().replaceAll("-", "");
const rateLimited = async (request: Request, env: Env) => {
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  const pathname = new URL(request.url).pathname;
  const group = pathname.startsWith("/v1/shares") ? "shares" : "backups";
  const bucket = Math.floor(Date.now() / 60_000);
  const key = `${ip}:${group}:${bucket}`;
  const row = await env.POZIX_BACKUPS.prepare(
    "INSERT INTO rate_limits(key,count,expires_at) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET count=count+1 RETURNING count"
  ).bind(key,(bucket+2)*60_000).first<{count:number}>();
  return (row?.count || 0) > 60;
};
const cleanupRateLimits = (env: Env) =>
  env.POZIX_BACKUPS.prepare("DELETE FROM rate_limits WHERE expires_at < ?").bind(Date.now()).run();

const worker: ExportedHandler<Env> = {
  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(cleanupRateLimits(env));
  },
  async fetch(request, env, ctx): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      try {
        await env.POZIX_BACKUPS.prepare("SELECT 1 AS ok").first();
        return json({ status: "ok", database: "ok" });
      } catch {
        return json({ status: "error", database: "unavailable" }, 503);
      }
    }
    if (["POST","PUT"].includes(request.method) && await rateLimited(request, env)) return json({error:"Too many requests"},429);
    if (request.method === "POST" && url.pathname === "/v1/youtube/transcript") {
      let input: unknown;
      try { input = await request.json(); } catch { return json({ code: "INVALID_JSON", error: "Request body must be JSON" }, 400); }
      try {
        const parsed = validateYoutubeRequest(input);
        const cacheLanguage = parsed.languages?.[0] || "en";
        const cacheKey = new Request(
          `${url.origin}/__youtube_transcript_cache/${parsed.videoId}/${encodeURIComponent(cacheLanguage)}`,
          { method: "GET" }
        );
        const cache = typeof caches !== "undefined"
          ? (caches as unknown as { default: Cache }).default
          : null;
        const cached = cache ? await cache.match(cacheKey) : undefined;
        if (cached) return cached;

        const transcript = await fetchNativeYoutubeTranscript(parsed, env.SUPADATA_API_KEY || "");
        const response = json(transcript, 200, "public, max-age=86400");
        if (cache) {
          const put = cache.put(cacheKey, response.clone());
          if (ctx?.waitUntil) ctx.waitUntil(put);
        }
        return response;
      } catch (error) {
        if (error instanceof YoutubeTranscriptError) {
          return json({ code: error.code, error: error.message }, error.status);
        }
        return json({ code: "PROVIDER_ERROR", error: "Transcript provider failed" }, 502);
      }
    }
    if (request.method === "POST" && url.pathname === "/v1/shares") {
      const input = await body<{quizJson:string}>(request);
      const validation = validateSharePayload(input);
      if (!validation.ok) return json({error:validation.message},400);
      const id=shareId(), now=Date.now(), expires=now+30*86400000;
      await env.POZIX_BACKUPS.prepare("DELETE FROM shares WHERE expires_at < ?").bind(now).run();
      await env.POZIX_BACKUPS.prepare("INSERT INTO shares(id,quiz_json,expires_at,created_at) VALUES(?,?,?,?)").bind(id,input!.quizJson,expires,now).run();
      return json({id,expiresAt:expires},201);
    }
    const share = url.pathname.match(/^\/v1\/shares\/([A-Za-z0-9]{32})$/);
    if (request.method === "GET" && share) {
      const row=await env.POZIX_BACKUPS.prepare("SELECT quiz_json,expires_at FROM shares WHERE id=?").bind(share[1]).first<{quiz_json:string;expires_at:number}>();
      if (!row || row.expires_at < Date.now()) return json({error:"Share unavailable"},404);
      return json({quizJson:row.quiz_json,expiresAt:row.expires_at});
    }
    const token = url.pathname.match(/^\/v1\/backups\/(hkm-[A-Za-z0-9]{36})(?:\/salt|\/restore)?$/)?.[1];
    if (request.method === "POST" && url.pathname === "/v1/backups") {
      const input=await body<BackupBody & {token:string}>(request);
      if (!input || !isValidBackupToken(input.token) || !isValidBackupPayload(input)) return json({error:"Invalid backup"},400);
      const existing=await env.POZIX_BACKUPS.prepare("SELECT token FROM backups WHERE token=?").bind(input.token).first<{token:string}>();
      if(existing) return json({error:"Token already exists"},409);
      await env.POZIX_BACKUPS.prepare("INSERT INTO backups(token,verifier,salt,ciphertext,updated_at,created_at) VALUES(?,?,?,?,?,?)").bind(input.token,input.verifier,input.salt,input.ciphertext,Date.now(),Date.now()).run();
      return json({ok:true},201);
    }
    if (token && request.method === "GET" && url.pathname.endsWith("/salt")) {
      const row=await env.POZIX_BACKUPS.prepare("SELECT salt FROM backups WHERE token=?").bind(token).first<{salt:string}>();
      return row ? json({salt:row.salt}) : json({error:"Backup not found"},404);
    }
    if (token && request.method === "PUT" && url.pathname === `/v1/backups/${token}`) {
      const input=await body<BackupBody>(request); if(!isValidBackupPayload(input)) return json({error:"Invalid backup"},400);
      const old=await env.POZIX_BACKUPS.prepare("SELECT verifier,salt FROM backups WHERE token=?").bind(token).first<{verifier:string;salt:string}>();
      if(!old || !equal(old.verifier,input!.verifier) || !equal(old.salt,input!.salt)) return json({error:"Invalid credentials"},401);
      await env.POZIX_BACKUPS.prepare("UPDATE backups SET ciphertext=?,updated_at=? WHERE token=?").bind(input!.ciphertext,Date.now(),token).run(); return json({ok:true});
    }
    if (token && request.method === "POST" && (url.pathname.endsWith("/restore") || url.searchParams.get("action")==="restore")) {
      const input=await body<{verifier:string}>(request); const row=await env.POZIX_BACKUPS.prepare("SELECT verifier,salt,ciphertext FROM backups WHERE token=?").bind(token).first<BackupBody>();
      if(!row || !input || !equal(row.verifier,input.verifier)) return json({error:"Invalid credentials"},401); return json({salt:row.salt,ciphertext:row.ciphertext});
    }
    return Response.json({ error: "Not found" }, { status: 404 });
  }
};

export default worker;
