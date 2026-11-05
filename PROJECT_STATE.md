# MOSAIC project state — session handoff

**Project:** MOSAIC / Personal Media OS  
**Repository:** ysfclc/MOSAIC  
**Branch:** main  
**Latest documentation commit:** `a89d8047369fa5b4664241ec660a8146ca31bff6` (2026-11-05; origin/copy-paste intent recorded directly in GitHub)  
**Current phase:** 1 — Native foundation and local playback  
**Architecture status:** LOCKED baseline; changes require an ADR in docs/DECISIONS.md  
**Source snapshot:** 2026-11-05 — integrated browser/bridge snapshot; documentation handoff refreshed

## Product contract

One native Android experience over multiple legitimate media sources. Local-first, provider-independent, no mandatory account, no mandatory subscription for core local playback, and no ads inserted by MOSAIC. Provider restrictions remain provider-controlled; no DRM/paywall/ad circumvention or unauthorized stream extraction.

## Founding purpose — copy/paste capture and two-way movement

MOSAIC started to solve the friction of moving useful content between apps. The user must be able to copy a URL or text in another app, explicitly paste it into MOSAIC, preview/save it for later, and then copy/open/share it back out. Android Share is an equivalent inbound route and must converge on the same capture flow. This is a core product requirement, not just a Browser shortcut. The eventual durable inbox must preserve source, capture time, type and status. See PRODUCT_SPEC.md and P3-009–P3-011 in TASKS.md.

Important boundary: explicit user-invoked paste only. Do not add background clipboard polling/history; that belongs to the separate VaultOS problem space.

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
- The 2026-11-05 origin/copy-paste documentation updates were committed directly to main across PRODUCT_SPEC.md, TASKS.md, PROJECT_STATE.md, README.md and DECISIONS.md (ADR-013). No application source code was changed in that documentation pass.
- The newer incoming ZIP commit `0b65d868` remains unprocessed due to the preflight failure; preserve it for a separately tracked workflow repair/import task.
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
- Inbound share preview: Android text/plain ACTION_SEND opens the shared URL/text directly in MOSAIC Browser; durable inbox and file-share handling remain pending. This is only a partial implementation of the founding copy/paste-and-keep workflow: an explicit paste UI, durable unified inbox and outbound round-trip acceptance are still TODO.
- Latest inbound share path verified at compile time: Android SEND text/plain -> MainActivity -> MosaicApp -> BrowserScreen initial URL/search. Device-level share-sheet testing remains pending.
