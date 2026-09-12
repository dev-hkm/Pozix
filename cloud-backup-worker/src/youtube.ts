const VIDEO_ID_PATTERN = /^[A-Za-z0-9_-]{11}$/;
const MAX_SEGMENTS = 12_000;
const MAX_TEXT_CHARS = 240_000;

export type YoutubeTranscriptRequest = {
  videoId: string;
  languages?: string[];
  mode?: "native";
};

export type YoutubeTranscriptSegment = {
  id: string;
  startMs: number;
  endMs: number;
  text: string;
};

export type YoutubeTranscriptResponse = {
  videoId: string;
  canonicalUrl: string;
  language: string;
  availableLanguages: string[];
  provider: "supadata";
  origin: "native_caption";
  fetchedAt: number;
  segments: YoutubeTranscriptSegment[];
};

export class YoutubeTranscriptError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status = 502
  ) {
    super(message);
    this.name = "YoutubeTranscriptError";
  }
}

export function canonicalYoutubeUrl(videoId: string): string {
  return `https://www.youtube.com/watch?v=${videoId}`;
}

export function validateYoutubeRequest(value: unknown): YoutubeTranscriptRequest {
  if (typeof value !== "object" || value === null) {
    throw new YoutubeTranscriptError("INVALID_URL", "videoId is required", 400);
  }
  const body = value as Record<string, unknown>;
  if (typeof body.videoId !== "string" || !VIDEO_ID_PATTERN.test(body.videoId)) {
    throw new YoutubeTranscriptError("INVALID_URL", "Unsupported YouTube video id", 400);
  }
  if (body.mode !== undefined && body.mode !== "native") {
    throw new YoutubeTranscriptError("UNSUPPORTED_MODE", "Only native captions are supported", 400);
  }
  const languages = body.languages === undefined
    ? ["en"]
    : Array.isArray(body.languages)
      ? body.languages.filter((item): item is string => typeof item === "string")
        .map(item => item.trim().toLowerCase())
        .filter(item => /^[a-z]{2,3}(?:-[a-z]{2,4})?$/.test(item))
        .slice(0, 5)
      : [];
  if (languages.length === 0) {
    throw new YoutubeTranscriptError("INVALID_LANGUAGE", "At least one valid language is required", 400);
  }
  return { videoId: body.videoId, languages, mode: "native" };
}

export async function fetchNativeYoutubeTranscript(
  request: YoutubeTranscriptRequest,
  apiKey: string,
  fetcher: typeof fetch = fetch
): Promise<YoutubeTranscriptResponse> {
  if (!apiKey.trim()) {
    throw new YoutubeTranscriptError("PROVIDER_NOT_CONFIGURED", "SUPADATA_API_KEY is not configured", 503);
  }

  const url = new URL("https://api.supadata.ai/v1/transcript");
  url.searchParams.set("url", canonicalYoutubeUrl(request.videoId));
  url.searchParams.set("mode", "native");
  url.searchParams.set("text", "false");
  url.searchParams.set("chunkSize", "1200");
  url.searchParams.set("lang", request.languages?.[0] || "en");

  let response = await fetcher(url, {
    headers: {
      "Accept": "application/json",
      "X-API-Key": apiKey
    }
  });
  let body = await readJson(response);

  if (response.status === 202) {
    const jobId = textValue(body, "jobId") || textValue(body, "id");
    if (!jobId) {
      throw new YoutubeTranscriptError("PROVIDER_ERROR", "Transcript job did not return an id");
    }
    for (let attempt = 0; attempt < 5 && response.status === 202; attempt++) {
      await delay(600);
      response = await fetcher(`https://api.supadata.ai/v1/transcript/${encodeURIComponent(jobId)}`, {
        headers: { "Accept": "application/json", "X-API-Key": apiKey }
      });
      body = await readJson(response);
    }
  }

  if (response.status === 206) {
    throw new YoutubeTranscriptError("NO_TRANSCRIPT", "No native transcript is available for this video", 404);
  }
  if (response.status === 401 || response.status === 403) {
    throw new YoutubeTranscriptError("PROVIDER_NOT_CONFIGURED", "Transcript provider rejected the gateway key", 503);
  }
  if (response.status === 429) {
    throw new YoutubeTranscriptError("RATE_LIMITED", "Transcript provider rate limit reached", 429);
  }
  if (!response.ok) {
    throw new YoutubeTranscriptError(
      "PROVIDER_ERROR",
      textValue(body, "error") || textValue(body, "message") || `Transcript provider returned HTTP ${response.status}`,
      502
    );
  }

  const content = body && typeof body === "object" && Array.isArray((body as Record<string, unknown>).content)
    ? (body as Record<string, unknown>).content as unknown[]
    : null;
  if (!content) {
    throw new YoutubeTranscriptError("INVALID_RESPONSE", "Transcript provider returned no timestamped segments");
  }

  let totalChars = 0;
  const segments: YoutubeTranscriptSegment[] = [];
  content.slice(0, MAX_SEGMENTS).forEach((item, index) => {
    if (typeof item !== "object" || item === null) return;
    const value = item as Record<string, unknown>;
    const text = typeof value.text === "string" ? value.text.replace(/\s+/g, " ").trim() : "";
    if (!text) return;
    const startMs = toMilliseconds(value.offset);
    const durationMs = toMilliseconds(value.duration);
    const endMs = Math.max(startMs + 1, durationMs > 0 ? startMs + durationMs : startMs + 1_000);
    totalChars += text.length;
    segments.push({ id: `s${index + 1}`, startMs, endMs, text });
  });

  if (segments.length === 0) {
    throw new YoutubeTranscriptError("NO_TRANSCRIPT", "No readable native captions were found for this video", 404);
  }
  if (totalChars > MAX_TEXT_CHARS) {
    throw new YoutubeTranscriptError("TRANSCRIPT_TOO_LARGE", "Transcript exceeds the safe quiz size", 413);
  }

  const language = textValue(body, "lang") || request.languages?.[0] || "en";
  const availableLanguages = Array.isArray(body && (body as Record<string, unknown>).availableLangs)
    ? ((body as Record<string, unknown>).availableLangs as unknown[]).filter((item): item is string => typeof item === "string")
    : [];
  return {
    videoId: request.videoId,
    canonicalUrl: canonicalYoutubeUrl(request.videoId),
    language,
    availableLanguages,
    provider: "supadata",
    origin: "native_caption",
    fetchedAt: Date.now(),
    segments
  };
}

async function readJson(response: Response): Promise<unknown> {
  return response.json().catch(() => null);
}

function textValue(value: unknown, key: string): string | null {
  if (typeof value !== "object" || value === null) return null;
  const candidate = (value as Record<string, unknown>)[key];
  return typeof candidate === "string" && candidate.trim() ? candidate.trim() : null;
}

function toMilliseconds(value: unknown): number {
  const number = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(number) || number < 0) return 0;
  return Math.round(number);
}

function delay(milliseconds: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
}
