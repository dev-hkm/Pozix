# Pozix AI Gateway

Separate Cloudflare Worker and D1 database for Pozix built-in AI accounts. It pins provider URLs/models server-side; the Android client never receives provider keys.

## Deploy

1. The D1 database `pozix-ai-accounts` is provisioned and its ID is in `wrangler.jsonc`.
2. Run `npx wrangler d1 migrations apply pozix-ai-accounts --remote`.
3. Run `npm install`, `npx wrangler types`, then `npx wrangler deploy`.
4. In Cloudflare Dashboard → Workers & Pages → `pozix-ai-gateway` → Settings → Variables and Secrets, add these as **secrets** (not plaintext vars): `GENERALCOMPUTE_API_KEY`, `OPENAI_API_KEY`, `OPENROUTER_API_KEY`.
5. Verify `https://pozix-ai-gateway.cloud-backup-worker.workers.dev/health` and update the Worker URL in the Android `PozixAiAccountRepository` only if the account subdomain differs.

The Worker intentionally returns `provider_unconfigured` until the relevant secret exists. Register/login accounts use display name, unique username, and a 10+ character password. Sessions expire after 30 days. New quiz generations share a limit of 3 per Vietnam calendar day; duplicate generation IDs do not spend quota again. Quiz JSON is validated and capped at 15 items by the app before it is saved.

Publish `https://<worker-host>/account-deletion` as the Play Console account deletion URL. Users can also delete their AI account from Settings in the app. Quiz/chat data is local and is not uploaded to the AI Worker.
