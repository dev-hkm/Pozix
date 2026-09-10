# Pozix Cloud Backup and Test Mode Design

## Goal

Make the import flow keyboard-safe, add a password-protected Cloudflare backup that needs no account, and complete Test mode without changing Pozix's existing Material 3 visual language.

## Product decisions

- Backup is deliberately accountless. The user creates one backup token and one password, then reuses that pair to back up and restore.
- The token is an identifier, not sufficient access. It is formatted as `hkm-` followed by 36 cryptographically random URL-safe letters and digits.
- The password never leaves the device in readable form. The Android client derives an AES-GCM key using PBKDF2-SHA-256 and a per-backup salt, encrypts the full backup JSON locally, and uploads only ciphertext.
- D1 stores the token identifier, password verifier, KDF salt, encrypted payload, payload size, and timestamps. The Worker cannot decrypt the backup.
- A single backup slot belongs to each token. Upload replaces its previous cloud copy after password verification. Token rotation creates a new credential and requires an upload to establish its backup.
- The user experience stays compact: one setup card in Settings, then Backup Cloud / Restore Cloud actions beside the existing local backup controls.

## Cloudflare architecture

Create a standalone Worker under `cloud-backup-worker/` with D1 binding `POZIX_BACKUPS`.

Routes:

- `POST /v1/backups`: create a token record after validating a 36-character token suffix, password verifier, salt, and encrypted payload.
- `PUT /v1/backups/:token`: replace ciphertext only after constant-time verifier comparison.
- `POST /v1/backups/:token/restore`: return ciphertext and salt only after verifier comparison.
- `POST /v1/backups/:token/rotate`: invalidate the old row only after verifier comparison and create the new row atomically.
- `GET /health`: deployment health check without data access.

All body sizes are capped at 1 MiB. Inputs are length- and shape-validated, SQL is parameterized, responses do not expose verifier data, and IP/token rate limiting is implemented with a small D1 request window. The deployed endpoint is compiled into the Android app as a non-secret base URL; no Cloudflare credentials are included in Android source.

## Android cloud backup flow

New cloud-only types sit in `data/cloud/` and do not change the existing local backup format:

- `CloudBackupCredentials(token, password)` is held only in ViewModel memory during a transaction. The app persists the token but never stores the password.
- `BackupCrypto` creates tokens, salts, PBKDF2 verifier/key material, AES-GCM ciphertext, and validates/decrypts incoming ciphertext.
- `CloudBackupApi` wraps the four HTTPS operations and maps network or server failures to safe user-facing messages.
- `CloudBackupRepository` coordinates export/import with the existing `SettingsViewModel` and validates decoded `BackupData` before a restore confirmation.

First cloud backup opens a credential sheet with generated token, password and confirmation. The token can be copied; password entry uses hidden text and requires confirmation. Later backup/restore asks only for password, with the saved token shown in a compact masked form. Losing both token and password intentionally means recovery is impossible.

## Import screen keyboard behavior

`ImportScreen` becomes an inset-aware scaffold. Its scrollable content receives both system-bar and IME bottom insets; while the JSON editor is focused, the action row remains reachable above the keyboard. Keyboard appearance must not hide Validate, Load, Save, or Skip actions on a typical 360x800dp viewport. Existing colours, cards, typography, paste, validation, and save behavior remain unchanged.

## Share quiz sets

Sharing uses the same Cloudflare Worker but stays separate from private cloud backup. From Library, a Share action uploads the selected validated quiz JSON and returns a compact public URL. Anyone opening that URL can import a read-only copy; it cannot expose the owner's cloud backup or mutate the original set. Shared sets expire after 30 days and can be regenerated from the card at any time.

`ImportScreen` gains a collapsed Import from link field. It accepts only the configured Pozix share URL, downloads the JSON, runs the existing `QuizJsonParser` validation, then uses the existing preview/save flow. Network, expired, malformed, and deleted links are shown as friendly inline errors. The Worker adds `POST /v1/shares` and `GET /v1/shares/:id`; D1 stores a random opaque share ID, JSON payload, creation time, and expiry time. Uploads remain capped at 1 MiB and share creation is rate limited.

## Test mode completion

Test mode remains separate from normal Quiz mode and maintains its current screen styling.

- Setup gains clear preset chips (5/10/20/30/45/60 minutes) plus a bounded custom duration field; shuffle settings remain available.
- An in-progress test is persisted after every answer, navigation action, and timer tick. Returning opens a Continue / Restart sheet rather than silently discarding the test.
- Back navigation opens a Save and Exit / Keep Taking Test confirmation.
- The question palette visibly distinguishes unanswered, answered, and current questions, includes answered count, and closes after selection.
- The player has an accessible progress label, stable timer warning state, and disabled Previous/Next controls at boundaries.
- Submission confirms unanswered count. Results show score, correct/wrong/unanswered breakdown, time used, grade, and an expandable answer review. Retrying resets only the active exam state.

## Verification

- Unit tests cover token shape, PBKDF2/AES-GCM round trips, invalid password handling, backup request validation, and exam persistence/state transitions.
- Worker tests cover every route's success, invalid shape, bad password, missing token, size limit, token rotation, share creation, expired share, and missing share behavior.
- `:app:assembleDebug` must pass using Android Studio's JBR. Worker tests and `wrangler deploy` must pass, followed by live health and a create/upload/restore endpoint check.
