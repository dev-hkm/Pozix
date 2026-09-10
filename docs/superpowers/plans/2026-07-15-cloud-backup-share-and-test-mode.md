# Pozix Cloud Backup, Sharing and Test Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make import keyboard-safe, add encrypted password-protected Cloudflare backup and public quiz sharing, then complete resilient Test mode.

**Architecture:** A standalone Cloudflare Worker/D1 service owns opaque backup records and public share records. Android adds isolated cloud crypto/API/repository layers, while existing screens keep their current Compose theme and ViewModel patterns.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore, OkHttp, Kotlin serialization, Web Crypto in Cloudflare Workers, D1, Wrangler, Vitest.

## Global Constraints

- Keep Kotlin and Jetpack Compose only for Android UI.
- Keep current Pozix colour system, typography, cards, haptics and animations.
- Store no password in DataStore or Worker logs; encrypt private backup JSON on-device with PBKDF2-SHA-256 plus AES-GCM.
- Never hard-code deployment credentials or Cloudflare tokens.
- Create a Git commit after every completed task.

---

### Task 1: Establish a reproducible Android baseline and shared cloud contract

**Files:**
- Modify: `.gitignore`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `cloud-backup-worker/package.json`, `cloud-backup-worker/wrangler.toml`, `cloud-backup-worker/tsconfig.json`, `cloud-backup-worker/src/contracts.ts`, `cloud-backup-worker/test/contracts.test.ts`

**Produces:** Cloud request/response payload shapes, Worker project scaffold, Android network permissions, and a buildable test harness.

- [ ] Write failing Worker contract tests for valid token shape, backup body size, and share body schema.
- [ ] Run `npm.cmd test` in `cloud-backup-worker`; verify the tests fail before contract implementation.
- [ ] Implement request validation helpers and shared DTOs in `src/contracts.ts`.
- [ ] Run Worker tests; verify they pass.
- [ ] Add only required Android dependencies and keep generated/cached files ignored.
- [ ] Commit as `chore: establish cloud backup contract`.

### Task 2: Implement and deploy Cloudflare Worker plus D1 schema

**Files:**
- Create: `cloud-backup-worker/migrations/0001_initial.sql`, `cloud-backup-worker/src/index.ts`, `cloud-backup-worker/test/worker.test.ts`

**Consumes:** Task 1 contracts.
**Produces:** `/health`, private backup create/upload/restore/rotate endpoints and public share create/read endpoints.

- [ ] Write failing route tests covering create, invalid password, missing backup, replacement, rotation, share creation, expired share, and payload limit.
- [ ] Run `npm.cmd test`; verify the routes fail before implementation.
- [ ] Create D1 tables for `backups`, `shares`, and `rate_limits`, with expiry and lookup indexes.
- [ ] Implement parameterized D1 operations, constant-time verifier comparison, generic errors, payload limits, and rate-limit windows.
- [ ] Run `npm.cmd test`; verify all route tests pass.
- [ ] Create/bind D1, apply migration, deploy Worker, and verify `/health` plus real create/upload/restore/share requests without logging credentials.
- [ ] Commit as `feat: add encrypted backup and share worker`.

### Task 3: Add Android cloud crypto and transport layers

**Files:**
- Create: `app/src/main/java/com/hkm/pozix/data/cloud/CloudBackupModels.kt`, `BackupCrypto.kt`, `CloudBackupApi.kt`, `CloudBackupRepository.kt`
- Modify: `app/src/test/java/com/hkm/pozix/ExampleUnitTest.kt`, `app/build.gradle.kts`

**Consumes:** Task 1 wire contracts and deployed Worker base URL.
**Produces:** Token generation, password verifier/key derivation, encrypted backup payloads, share API calls, safe API failure mapping.

- [ ] Write failing Kotlin unit tests for token format, encryption/decryption round trip, and wrong-password rejection.
- [ ] Run the targeted unit test; verify it fails before implementation.
- [ ] Implement `BackupCrypto` using `SecureRandom`, PBKDF2WithHmacSHA256, AES/GCM/NoPadding and Base64 URL-safe encoding.
- [ ] Implement `CloudBackupApi` with bounded OkHttp requests and no sensitive logging.
- [ ] Implement `CloudBackupRepository` to export/import existing `BackupData` through the encrypted API.
- [ ] Run targeted tests and `:app:assembleDebug`; verify success.
- [ ] Commit as `feat: add encrypted cloud backup client`.

### Task 4: Integrate backup credentials and cloud actions in Settings

**Files:**
- Modify: `SettingsRepository.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `res/values/strings.xml`, `res/values-vi/strings.xml`

**Consumes:** Task 3 repository.
**Produces:** First-time token/password setup, later password prompt, cloud upload/restore, token rotation, masked persisted token and restore confirmation.

- [ ] Write failing ViewModel tests for generated token persistence without password persistence, and restore confirmation after successful decryption.
- [ ] Run targeted tests; verify failure before integration.
- [ ] Add token-only DataStore accessors and cloud operation states.
- [ ] Add animated Compose credential sheet and progress/error cards following existing Settings styling.
- [ ] Wire Cloud Backup, Restore Cloud, and Create New Token actions; use existing restore confirmation after decrypting valid backup data.
- [ ] Run tests and `:app:assembleDebug`; verify success.
- [ ] Commit as `feat: add password protected cloud backup UI`.

### Task 5: Make Import inset-safe and support shared links

**Files:**
- Modify: `ImportViewModel.kt`, `ImportScreen.kt`, `res/values/strings.xml`, `res/values-vi/strings.xml`

**Consumes:** Task 3 share API.
**Produces:** IME-safe action layout and validated loading from a Pozix share URL.

- [ ] Write failing ViewModel tests for a valid share payload, invalid URL, expired/missing link response, and JSON parser failure.
- [ ] Run tests; verify failure before integration.
- [ ] Add share-link loading state and only accept the configured Worker share URL pattern.
- [ ] Change the screen to `Scaffold`/scroll composition with `WindowInsets.ime` bottom padding and bring focused editor/actions into view.
- [ ] Add a collapsed Import from link card and route success into the existing validation/preview/save state.
- [ ] Verify manually with an Android emulator keyboard and run `:app:assembleDebug`.
- [ ] Commit as `fix: make import keyboard safe and load shared quizzes`.

### Task 6: Add share controls to Library

**Files:**
- Modify: `SavedQuizSetsViewModel.kt`, `SavedQuizSetsScreen.kt`, `res/values/strings.xml`, `res/values-vi/strings.xml`

**Consumes:** Task 3 share API.
**Produces:** Per-card animated Share action that uploads validated JSON and copies/displays a share URL.

- [ ] Write failing ViewModel tests for successful link creation and a mapped share failure.
- [ ] Run tests; verify failure before integration.
- [ ] Add per-set share loading/success/error state and upload selected JSON via `CloudBackupRepository`.
- [ ] Add a compact icon action and link sheet that follows current card interactions, haptic feedback and clipboard behavior.
- [ ] Run tests and `:app:assembleDebug`; verify success.
- [ ] Commit as `feat: share quiz sets from library`.

### Task 7: Persist and complete Test mode

**Files:**
- Create: `app/src/main/java/com/hkm/pozix/data/model/ExamSession.kt`, `app/src/main/java/com/hkm/pozix/data/repository/ExamSessionRepository.kt`
- Modify: `ExamViewModel.kt`, `ExamPlayerScreen.kt`, `ExamResultScreen.kt`, `Navigation.kt`, `res/values/strings.xml`, `res/values-vi/strings.xml`

**Consumes:** Existing quiz parsing and current active quiz-set ID.
**Produces:** Recoverable exam session, duration presets/custom duration, exit confirmation, improved palette/player states and complete review results.

- [ ] Write failing unit tests for saving/restoring an answer session, elapsed time calculation, submit cleanup, and restart cleanup.
- [ ] Run tests; verify failure before implementation.
- [ ] Implement serializable session persistence keyed by quiz-set ID and atomically save it after answer/navigation/timer changes.
- [ ] Load a compatible unfinished session into a Continue/Restart state; discard incompatible/corrupt sessions safely.
- [ ] Add duration chips, bounded custom duration, labelled progress, boundary-safe navigation, and an animated exit confirmation.
- [ ] Improve palette counts and question states; ensure submission/result review uses persisted answer data and retry clears only the current session.
- [ ] Run unit tests, manual emulator test-mode scenarios, and `:app:assembleDebug`.
- [ ] Commit as `feat: complete recoverable test mode`.

### Task 8: End-to-end verification and final checkpoint

**Files:**
- Modify: `README.md`

- [ ] Run all Android unit tests and `:app:assembleDebug` with Android Studio JBR.
- [ ] Run Worker tests, deploy check, live health check, create/upload/restore, create/open share and expired-share checks.
- [ ] Verify import controls remain usable with the keyboard open and Test mode resumes after process recreation.
- [ ] Document setup, D1 binding, deployment endpoint, token/password recovery limitation, and sharing expiry.
- [ ] Commit as `docs: document cloud backup sharing and test mode`.
