# Changelog

## 2026-11-12 — Continuity ledger and capability reconciliation

- Added `docs/STATE.md` as the concise canonical session ledger and `docs/SPEC.md` as the requirement-ID traceability registry; `docs/PRODUCT_SPEC.md` remains the narrative product specification.
- Reconciled `docs/TASKS.md` and `docs/PROVIDER_CAPABILITY_MATRIX.md` with the source snapshot: validated HTTPS/HLS streams, bounded RSS/Atom feed loading and queueing, direct HTTPS HTML5 Browser-to-Media3 handoff, the Media3 video surface, and delegated Android media sessions are represented as partial source capabilities, not device-accepted features.
- Recorded the known mismatch between the locked source-aware filtering requirement (ADR-014/015, PRODUCT_SPEC.md §11) and current Browser policy/tests, which still preserve YouTube/Spotify and Google-ad-infrastructure exclusions. P3-015 remains open; no filtering behavior was changed in this documentation increment.
- Baseline evidence: main commit `15e53c718db3c0e409324f36a0d5c00d16fddc3a`, GitHub Actions run [#177](https://github.com/ysfclc/MOSAIC/actions/runs/42261946226) passed the source verifier, 16 Python contract tests, Android unit tests, lint, APK assembly, signing/checksum checks and private prerelease publication.
- Branch CI runs #178–#182 failed in preflight before any runner was assigned (`runner_id: 0`; no job steps executed); retry attempt 2 of #182 failed the same way. The Android build/test jobs were skipped. Therefore this documentation increment is **not CI-verified**; this is an Actions runner/preflight blocker, not an observed source/test failure.

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


## 2026-11-05 — Ad Shield DNS-only VPN architecture locked

- Added ADR-016 for a user-consented Android VpnService that routes only DNS traffic through a local virtual resolver; ordinary app traffic must not enter a full VPN tunnel.
- Allowed DNS queries are forwarded through a socket protected from the VPN; blocked domains receive a local DNS failure response.
- Required explicit VPN consent, one-VPN conflict warning, persistent foreground notification with Stop action, Private DNS/DoH coverage warnings, blocked-domain diagnostics and safe stop/revoke recovery.
- DNS packet codec and coverage tests are tracked separately before the TUN service is wired.
- No device-wide filter implementation is claimed yet.


## 2026-11-12 — Vercel APK build path

- Created the Vercel Hobby project `mosaic-apk`, linked to the private `ysfclc/MOSAIC` repository; project creation did not trigger a deployment or change production.
- Added `vercel.json` and `scripts/vercel-build-apk.sh` to bootstrap Java 17 and Android SDK, run source/Python/Android tests and lint, assemble the debug APK, verify the persistent development signing certificate and publish APK/checksum/build metadata as Vercel static output.
- Added `docs/VERCEL_APK_BUILD.md` with the preview-to-production delivery flow and artifact contract.
- First Vercel Preview build is pending. No APK from this pipeline is claimed until build logs, signature, checksum and deployed download are verified.
