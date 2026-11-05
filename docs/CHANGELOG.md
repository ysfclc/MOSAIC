# Changelog

## 2026-10-30 — Phase 1 source baseline

- Established MOSAIC Android multi-module architecture and source adapter/capability contract.
- Added Media3 background playback service and controller connection.
- Added read-only MediaStore audio indexing and Storage Access Framework open-file route.
- Added Home, Library and Player Compose screens with a dark futuristic motion system.
- Added project state, product spec, phased roadmap, task board, architecture/decision notes, source verifier and ZIP-import build workflow.
- Android Gradle compilation and Redmi device validation remain pending GitHub Actions and physical-device verification.

## 2026-11-04 — Phase 1 Kotlin and CI SDK repair

- Fixed the revision scope error by forwarding the existing playback revision into PlayerArtwork.
- Updated the GitHub Actions Android API 37 SDK package to the published platforms;android-37.2 package and aligned README setup instructions.
- Added Python regression tests for both fixes.
- Verified source checks, 8 Python tests, Android unit-test tasks, Android Lint, and debug APK assembly in a Vercel sandbox using Java 17, Gradle 9.3.1 and Android SDK 37.2.
- Debug APK SHA-256: a79b047f766b754b3eb097ec1c31d9fd4bcd0dd3c3ae603a3d4b487174d48078.
- GitHub import workflow, wrapper bootstrap and physical-device acceptance remain pending.

## 2026-11-04 — Browser and ad-control foundation preview

- Added a native Browser destination with address/search, navigation, refresh and YouTube/Spotify shortcuts.
- Added a conservative request-level ad/tracker host filter with a user toggle and blocked-request counter.
- Exempted YouTube/Spotify pages and assets and avoided Google ad infrastructure used by provider players.
- Added five ad policy unit tests; all pass.
- Added INTERNET permission and bumped preview version to 0.2.0-browser-preview.
- Clarified cross-source product requirements, ad-control boundaries, two-way handoff and provider-owned playback in PRODUCT_SPEC, PHASES, SOURCE_ADAPTER_CONTRACT and ADR-010 through ADR-012.
- This is not a universal/system-wide ad blocker. Provider integrations and physical device validation remain outstanding.

## 2026-11-04 — External media bridge preview

- Added optional Android NotificationListenerService access for discovering active external MediaSessions.
- Added external session metadata and supported play/pause/next controls in the Browser destination.
- Added Android share-link action for the current browser page.
- The bridge does not inspect notification text, access external audio bytes or override provider-owned background rules.
- Build, lint and 5 browser policy unit tests pass; device-level session-control verification remains pending.
- Added an Android text/plain share target; shared URLs/text open directly in MOSAIC Browser.
## CI-verified Phase 1 import — run 41957296181

- ZIP validation and source integration passed.
- Source verifier and Python tests passed.
- Gradle wrapper generated and checksum-pinned.
- Android unit tests, lint and debug APK assembly passed.
- Physical Redmi Note 12 Pro 5G acceptance remains pending.
- Workflow: https://github.com/ysfclc/MOSAIC/actions/runs/41957296181


## 2026-11-05 — Product vision lock and direct-edit CI

- Locked MOSAIC's two primary pillars: cross-source background playback/control and broad ad/tracker blocking across apps and websites (ADR-014/ADR-015).
- Corrected the architecture, provider capability matrix, adapter contract, phase plan, README and session handoff. Fully withdrew ADR-013's incorrect copy/paste founding-purpose claim.
- Removed provider-wide ad-filter exemptions as a required architectural/test invariant; current app code still contains legacy exemptions pending source-aware replacement and compatibility tests.
- Added a direct repository build path for app/core/tools/docs/build changes while retaining the separate validated ZIP importer.
- Verified direct CI in [run 42033924158](https://github.com/ysfclc/MOSAIC/actions/runs/42033924158): source verifier, Python contract tests, Android unit tests, lint and debug APK assembly passed.
- Debug APK SHA-256: `69cb441baf1a502c6dcbac5bbeba9fda06eb9604c5b319c514608159c7f62881`.
- No playback or ad-blocking application feature was marked complete by this documentation/CI work. Physical Redmi device acceptance remains pending.
- Next: move external MediaSession controls from Browser into the primary Player (P3-007), then build the cross-app VPN/DNS Ad Shield (P3-008).


## 2026-11-05 — External media controls moved into Player

- Moved external Android MediaSession discovery out of Browser and into app-level state.
- Added a primary Player section for media playing in other apps, with source app label, title, artist and live playing/paused state.
- Added previous/next and seek controls only when the source session advertises those actions; play/pause remains capability-gated.
- Kept source ownership explicit: these commands control the original app and do not transfer audio into MOSAIC or override its background-playback restrictions.
- Removed the external playback panel from Browser.
- CI passed: [run 42034925797](https://github.com/ysfclc/MOSAIC/actions/runs/42034925797), including Android unit tests, lint and debug APK assembly. Real-app/Redmi acceptance remains pending.
- Debug APK SHA-256: `74a55e78f58e9cded6f43c261676756b9014bd7e91c970be65e9d4b7703be152`.
