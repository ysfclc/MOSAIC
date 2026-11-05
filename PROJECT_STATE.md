# MOSAIC project state — session handoff

**Project:** MOSAIC / Personal Media OS  
**Repository:** ysfclc/MOSAIC  
**Branch:** main (source ZIP fetched from the repository; no GitHub commit made by this task)  
**Current phase:** 1 — Native foundation and local playback  
**Architecture status:** LOCKED baseline; changes require an ADR in docs/DECISIONS.md  
**Source snapshot:** 2026-11-04 — corrected Phase 1 build handoff

## Product contract

One native Android experience over multiple legitimate media sources. Local-first, provider-independent, no mandatory account, no mandatory subscription for core local playback, and no ads inserted by MOSAIC. Provider restrictions remain provider-controlled; no DRM/paywall/ad circumvention or unauthorized stream extraction.

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

- P1-010: run the repository ZIP-import workflow end-to-end on GitHub.
- P1-011: generate and commit the Gradle wrapper and pinned wrapper checksum through the first successful import workflow.
- P1-012: local Android build is verified; GitHub Actions import/build result is still pending.
- P1-013 and P1-014: physical Redmi Note 12 Pro 5G acceptance remains pending (screen-off playback, notification, lock screen, headset/Bluetooth, audio focus, app startup, permissions, SAF, and local indexing).
- Phase 2 must remain parked until Phase 1 device acceptance is completed.

## Build record

See notes/builds/2026-11-04-vercel-build.md for sandbox/toolchain details, Gradle outcomes, APK checksum, lint warnings, and exact acceptance limits.

## Handoff integrity

- Source was fetched from incoming/MOSAIC-Phase-1-Repository.zip at GitHub blob e45cd6b123cca7b8376364518907fbbd3d43a0d8.
- The corrected source ZIP preserves the app source, project documentation, and GitHub workflow. Generated build caches and build outputs are excluded from the source ZIP; the APK is handed off separately.
- This task did not push or commit changes to GitHub. Upload the resulting full-source ZIP as incoming/mosaic.zip to invoke the repository's canonical importer.
- No claim of physical-device validation is made.

## Browser and cross-source requirements update — 2026-11-04

The original product scope is explicitly recorded in PRODUCT_SPEC.md and ADR-010 through ADR-012:
- MOSAIC Browser has a first implementation with request-level ad/tracker filtering for a conservative list of third-party domains.
- YouTube and Spotify pages/assets are exempt from the browser filter.
- The Browser is a first-party WebView screen with URL/search, back/forward/refresh, provider shortcuts and visible filter status.
- No universal/system-wide ad blocking is claimed. Device-wide DNS/VPN filtering is parked as an optional, separately consented task.
- Two-way media bridge is specified: inbound share/capture and authorized external session control; outbound share/open handoff and MOSAIC's own Android MediaSession.
- YouTube official playback remains visible and provider-owned; no ad removal, background player, audio extraction or offline copy.
- Spotify integration is planned through the official Android App Remote SDK, with Spotify retaining playback and background ownership.

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
- Inbound share preview: Android text/plain ACTION_SEND opens the shared URL/text directly in MOSAIC Browser; durable inbox and file-share handling remain pending.
- Latest inbound share path verified at compile time: Android SEND text/plain -> MainActivity -> MosaicApp -> BrowserScreen initial URL/search. Device-level share-sheet testing remains pending.
