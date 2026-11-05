# MOSAIC — Browser and external-media bridge preview build

Date: 2026-11-04
Repository: ysfclc/MOSAIC
Branch/source: corrected Phase 1 source plus Browser/bridge preview; no GitHub commit pushed
Version: 0.2.0-browser-preview (versionCode 2)

## Product scope recovered

The original scope is Player + Browser + Library + Intelligence + Automation, with local media, podcasts/open streams, browser media, YouTube and Spotify. This build starts implementing the missing Browser and cross-app control surface without claiming provider capabilities that their official interfaces do not grant.

## Implemented

1. Browser destination in native Compose navigation:
   - WebView with address/search, back, forward, refresh and provider shortcuts.
   - JavaScript and DOM storage enabled for normal modern sites; file and content access disabled.
   - Visible request-filter status and blocked-request counter.
2. Conservative host-level ad/tracker request filter:
   - Blocks a small bundled list of third-party advertising/tracker hosts on ordinary pages.
   - User can toggle the filter.
   - YouTube/Spotify pages and provider assets are explicitly exempt.
   - Google/DoubleClick ad infrastructure is intentionally not blocked because it can participate in YouTube player delivery.
3. Two-way bridge preview:
   - Share current browser URL through Android ACTION_SEND.
   - Accept incoming text/plain ACTION_SEND from other Android apps; shared URLs open in Browser and plain text is searched.
   - Optional Android NotificationListenerService access; MOSAIC can list active external MediaSessions and expose supported play/pause/next commands.
   - Notification text is not read; external audio bytes are not accessed.
4. Product and architecture documents:
   - Product requirements expanded for ad control, provider-owned background playback and both directions of media handoff.
   - ADR-010 through ADR-012 record scoped ad control, two-way bridge and YouTube/Spotify integration boundaries.
   - Added PROVIDER_CAPABILITY_MATRIX.md.
   - Task board and project state updated; Phase 1 physical-device acceptance remains open.
5. Source verifier and Python contract tests updated for the Browser's intentional INTERNET permission and explicit notification-listener bridge.

## Important capability limits

- Browser filtering is request/host-level, not a full EasyList-class engine. It cannot guarantee removal of all ads, cosmetic placeholders, first-party ads or same-host/in-stream ads. WebView interception has redirect and scheme limitations.
- Provider pages/assets are exempt. MOSAIC does not remove YouTube/Spotify ads, alter their players, extract audio, bypass DRM/paywalls or unlock subscriptions.
- YouTube official API playback must remain visible and retain ads/player behaviour; it cannot be made a hidden background player through the API. If the user's YouTube app/account independently supports background playback, the official app owns it; MOSAIC may control its exposed Android MediaSession after notification-access opt-in.
- Spotify Android App Remote controls the installed Spotify app and reads permitted player state; Spotify owns the audio stream and background lifecycle. The Spotify Android SDK is beta and integration is still pending.
- External MediaSession discovery depends on each app exposing a session and the user explicitly enabling notification access.
- MOSAIC can share the current page URL. Durable inbound share-to-library, file handoff and provider adapters remain future tasks.
- Device-wide DNS/VPN ad filtering is parked as a separate opt-in feature; it cannot remove same-domain/in-stream ads and uses Android's VPN slot.

## Verification

- Source verifier: PASS (tools/verify_source.py).
- Python contract suite: 10/10 PASS (tools/tests).
- Android app unit tests: 5/5 PASS (AdBlockPolicyTest; 0 failures, 0 errors).
- Gradle tasks:
  - :app:testDebugUnitTest — PASS.
  - :app:lintDebug — PASS with warnings; no lint errors.
  - :app:assembleDebug — PASS.
- Gradle: 9.3.1; Java 17; Android SDK platform 37.2; Build Tools 36.0.0.
- APK metadata: package com.ysfclc.mosaic; versionCode 2; versionName 0.2.0-browser-preview; min SDK 26; target SDK 36; compile SDK 37.
- APK signature: verified (v2 scheme).
- APK SHA-256: 1dfa125f0a7230f5bf1015e6371c471777e8f5400f089ba9e8407e558f8ef187.
- APK size: 22 MB.

## Lint warnings retained

- Target SDK 36 is below the latest available target.
- Dependency update suggestions for core-ktx, activity-compose, lifecycle-runtime-ktx and Compose BOM.
- WebView JavaScript is enabled for ordinary modern sites; the implementation disables file/content access and does not expose a JavaScript bridge. This warning still needs a security review.
- Exported media service and legacy backup configuration warnings remain.
- KTX URI conversion suggestions remain.

## Not completed

- No Android emulator/physical device was available in this sandbox; Browser rendering, share-sheet delivery, provider playback and external MediaSession controls need verification on the Redmi Note 12 Pro 5G.
- No GitHub import workflow run or push was performed.
- Gradle Wrapper bootstrap remains pending the canonical GitHub importer.
- Phase 1 physical-device acceptance remains open. Phase 2 release gate is not accepted.
