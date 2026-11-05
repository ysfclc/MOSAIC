# MOSAIC project state — session handoff

**Project:** MOSAIC / Personal Media OS  
**Repository:** ysfclc/MOSAIC  
**Branch:** main  
**Origin decision commit:** `a89d8047369fa5b4664241ec660a8146ca31bff6` (2026-11-05); subsequent handoff and task-ledger corrections are also committed on `main`. See the repository commit history for the current HEAD.  
**Current phase:** 1 — Native foundation and local playback  
**Architecture status:** LOCKED baseline; changes require an ADR in docs/DECISIONS.md  
**Source snapshot:** 2026-11-05 — integrated browser/bridge snapshot; documentation handoff refreshed

## Product contract

One native Android media hub whose primary goal is background playback from as many external sources as technically feasible and ad/tracker blocking across as many apps and surfaces as technically feasible. The user explicitly wants to cover YouTube, Spotify, Facebook, Instagram and other sources. The product must investigate source-specific paths rather than preemptively exempting major services. This is the target vision, not a claim that every source or ad is already supported.

## Founding purpose — background playback from other sources + ad blocking

The actual MOSAIC vision is to play media from other sources in the background and block ads wherever technically possible. The user specifically named YouTube, Spotify, Facebook, Instagram and "whatever" else. MOSAIC should unify source discovery/playback/control, keep media playing when the user leaves the app or turns off the screen, and pursue broad ad/tracker blocking inside the browser and across other apps where Android permits it.

Do not describe MOSAIC as a clipboard/copy-paste product. Copy/paste and share intents may be supporting conveniences, not the founding problem. The earlier ADR-013 and copy/paste-origin documentation were incorrect and are superseded/retracted by ADR-014.

The product goal is broad; implementation claims must remain evidence-based. For each source, investigate native Media3 playback, browser playback, official SDK/API options, Android MediaSession control and other viable routes. Clearly distinguish MOSAIC playing the audio itself from remotely controlling another app. For ad blocking, investigate both browser filtering and optional device-wide VPN/DNS filtering; document source compatibility, first-party/in-stream limitations and false positives rather than blanket-exempting named providers.

## Current implementation

- Multi-module Android project: :app, :core:media, :core:library, :core:design.
- Compose Home / Library / Player UI and animated visual system.
- MediaStore audio catalog with read-only indexing; Storage Access Framework open-file playback.
- Media3 ExoPlayer in MediaSessionService, with controller connection and queue playback.
- Provider-neutral MediaSourceAdapter and explicit capability model.
- PlayerArtwork now receives the playback revision from PlayerScreen; the SIGNAL label no longer references an out-of-scope value.
- CI installs the published Android SDK API 37 platform package platforms;android-37.2; compileSdk = 37, targetSdk = 36, minSdk = 26 remain unchanged.

## Verification — 2026-11-04

- Source verifier: **passed** in GitHub Actions run 41957296181.
- Python contract tests: 8/8 PASS (python3 -m unittest discover -s tools/tests -v). Includes regression coverage for the artwork revision parameter and SDK package name.
- Android build: PASS in the Vercel sandbox with Java 17, Gradle 9.3.1, Android SDK Platform 37.2 and Build Tools 36.0.0.
- Gradle tasks: :core:media:testDebugUnitTest, :app:testDebugUnitTest, :app:lintDebug, :app:assembleDebug — BUILD SUCCESSFUL.
- Debug APK: app/build/outputs/apk/debug/app-debug.apk (22 MB), SHA-256 a79b047f766b754b3eb097ec1c31d9fd4bcd0dd3c3ae603a3d4b487174d48078.
- APK metadata verified: application ID com.ysfclc.mosaic, version 0.1.0-phase1, compile SDK 37, target SDK 36, min SDK 26.
- Android Lint completed successfully with existing warnings (target SDK 36, dependency update suggestions, exported media service permission warning, and deprecated backup configuration). These warnings were not suppressed or silently changed.
- The first sandbox Gradle attempt lost its daemon under memory pressure. A second build completed successfully, followed by a clean cached/offline verification pass that also completed successfully.

## Remaining acceptance gates

- P1-010, P1-011 and P1-012: DONE for the integrated source snapshot; successful GitHub Actions run 41957296181 performed import, wrapper/checksum bootstrap, source verification, tests, lint and APK build.
- Latest upload commit `0b65d868444010a74041b395931da37e4530c14f` did not import: run 41962171501 failed in preflight because the push event did not resolve a changed archive path. The ZIP at incoming/mosaic.zip is therefore not integrated; do not treat that upload as current source.
- Direct documentation commits made 2026-11-05 are on main. Current workflow triggers only for the two incoming ZIP paths or manual dispatch; ordinary direct source/documentation edits do not trigger this build workflow. Add a safe direct-edit CI path without breaking ZIP import (tracked as GOV-001).
- P1-013 and P1-014: physical Redmi Note 12 Pro 5G acceptance remains pending (screen-off playback, notification, lock screen, headset/Bluetooth, audio focus, app startup, permissions, SAF, and local indexing).
- Phase 2 must remain parked until Phase 1 device acceptance is completed.

## Build record

See notes/builds/2026-11-04-vercel-build.md for sandbox/toolchain details, Gradle outcomes, APK checksum, lint warnings, and exact acceptance limits.

## Handoff integrity

- The integrated source snapshot is on main and was verified by GitHub Actions run 41957296181.
- The 2026-11-05 correction records the actual cross-source background playback and broad ad-blocking vision in PRODUCT_SPEC.md, TASKS.md, PROJECT_STATE.md, README.md and DECISIONS.md (ADR-014). Earlier copy/paste-origin notes are explicitly retracted. No application source code was changed in this documentation correction.
- The newer incoming ZIP commit `0b65d868` remains unprocessed due to the preflight failure; preserve it for a separately tracked workflow repair/import task.
- No claim of physical-device validation is made.

## Browser, ad filtering and cross-source playback status — 2026-11-05

The actual product goal is broad background playback from other sources and broad ad blocking. ADR-014 supersedes the earlier blanket provider exemptions and the incorrect copy/paste-origin decision.
- MOSAIC Browser currently has a basic WebView with a conservative third-party request/host filter. It is not a universal ad blocker.
- Current AdBlockPolicy explicitly exempts YouTube/Google video and Spotify-related domains. This is existing code behavior, not a user-approved final product requirement; revisit it as part of source-by-source ad-filter research and compatibility testing.
- ExternalMediaSessionBridge discovers external Android MediaSessions after explicit notification-access opt-in and exposes play/pause/next where supported. This is remote control, not audio transfer or native playback inside MOSAIC.
- MOSAIC's own Media3 PlaybackService supports its local/accessible media sources in the background. Broad native background playback from YouTube, Spotify, Facebook, Instagram and other external sources has not been implemented or accepted.
- Device-wide VPN/DNS ad filtering is not implemented. It must be investigated, not described as complete or permanently parked.
- Yesterday's ZIP import run 41926489353 passed ZIP validation/extraction and source integration into the runner workspace, then failed source verification: active workflow SDK package list uses android-36 while the source verifier expects android-37.2. Build/test/APK and repository integration steps were skipped. No feature changes from that ZIP are confirmed on main.

Current browser preview build:
- Version: 0.2.0-browser-preview (versionCode 2).
- Browser and ad filter policy unit tests: 5/5 pass.
- :app:lintDebug and :app:assembleDebug: pass; lint warnings remain and are recorded in the build report.
- Live website/device compatibility and actual playback behaviour have not yet been validated on the Redmi device.
- P1-013/P1-014 remain open; Phase 2/3 release gates are not claimed complete.

- External playback bridge preview: optional Android notification-listener settings entry, active MediaSession discovery, metadata display and supported play/pause/next commands. It does not inspect notification text or copy audio bytes.
- Browser outbound link share: current page URL can be sent through Android ACTION_SEND.
- Latest debug APK SHA-256: 1dfa125f0a7230f5bf1015e6371c471777e8f5400f089ba9e8407e558f8ef187 (22 MB; version 0.2.0-browser-preview).
- Latest verification: app unit tests 5/5 pass; Python contract tests 10/10 pass; :app:lintDebug and :app:assembleDebug pass. Lint warnings remain, including JavaScript-enabled WebView review and existing project warnings.

Latest Browser/bridge build record: notes/builds/2026-11-04-browser-preview-build.md.
- Inbound share preview: Android text/plain ACTION_SEND opens the shared URL/text directly in MOSAIC Browser; durable inbox and file-share handling remain pending. This is only a partial implementation of the founding copy/paste-and-keep workflow: an explicit paste UI, durable unified inbox and outbound round-trip acceptance are still TODO.
- Latest inbound share path verified at compile time: Android SEND text/plain -> MainActivity -> MosaicApp -> BrowserScreen initial URL/search. Device-level share-sheet testing remains pending.
