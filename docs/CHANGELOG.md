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


## 2026-11-12 — Vercel APK pipeline status

- Updated Android SDK license acceptance to feed a finite set of affirmative responses, avoiding a possible yes/SIGPIPE failure with pipefail enabled.
- Saved Vercel project build settings for mosaic-apk (build command, install command, dist output and Node 22).
- Vercel deployment dpl_AswuWhBgWLhh1NT4nid8Pb5Eh23X for commit c946457f184d4a2777416f872014106d5a109e2d remains queued with no build events and is not live. Its metadata reports a production target for a continuity-branch commit; this must be resolved before release.
- The Vercel API deployment limit (api-deployments-free-per-day, 100/day, retry after 24 hours) blocked a direct deployment request. No APK has been built or verified by this pipeline.


## 2026-11-12 — Vercel deployment safety gate

- Canceled the initial queued Vercel deployment before build because its metadata unexpectedly reported a production target for the continuity branch. It never went live and produced no APK.
- The Hobby team's deployment API limit (100/day; retry after 24 hours) prevented creating a replacement preview deployment in this session. The next build must target Preview from the latest branch commit; production remains on main.


## 2026-11-12 — Browser source-aware filtering increment

- Removed the whole-page YouTube/Spotify filter exemption. Known third-party ad/tracker hosts are now filtered on provider pages too.
- Added Google ad-infrastructure domains to the blocklist. A narrow compatibility exception remains only on YouTube top-level pages because the existing implementation documented a player-delivery dependency; the same hosts are blocked on ordinary and Spotify pages.
- Replaced exemption tests with source-context regression cases for provider-page third-party filtering, YouTube compatibility, Google ad infrastructure, provider content/media hosts, unknown hosts and malformed URLs.
- Updated Browser status text so provider pages are no longer labelled filter-exempt and the temporary YouTube compatibility exception is disclosed.
- Kotlin/Gradle tests have not yet executed; Vercel deployment quota and GitHub Actions runner provisioning remain blockers. Physical Redmi compatibility testing is still required.


## 2026-11-12 — Source-aware filter CI status

- Latest branch workflow run #189 for commit `4961715e547ae7be2d38191e7eaf4ef2988816d8` failed in the preflight job before any steps ran; Android test/build jobs were skipped. The new Kotlin policy tests remain unexecuted.
- Hoisted the YouTube page-domain compatibility set to a single immutable value rather than allocating it on each intercepted request.


## 2026-11-12 — CI preflight retry

- Latest branch workflow run #190 for commit `8cd744da9c8636f4478244868aec3415986b5d94` again failed in the preflight job with no steps; Android build/test jobs were skipped. The source-aware Kotlin tests remain unexecuted.


## 2026-11-12 — Embedded YouTube compatibility context

- Scoped the temporary Google player compatibility allowlist to either a YouTube top-level page or a request whose Referer is a YouTube page, so embedded YouTube players are not treated as ordinary third-party ad traffic.
- Browser now passes the request Referer URL into the pure policy function; regression tests cover top-level, embedded-frame, Spotify and ordinary-page cases.
- The Android test suite remains unexecuted because GitHub Actions preflight fails before runner assignment and Vercel's deployment API quota blocks a build.

- Latest workflow run #191 for commit `7ff0710c1730a1f49baa4199b1990bdb27eaa590` again failed before runner assignment; Android tests and APK build did not run.

- Latest workflow run #192 for commit `8fce7d8bb800eed12e9e0b133ff06c8ae56ad22b` failed in preflight before runner assignment; no Kotlin/Gradle checks ran.


## 2026-11-12 — Browser filter hardening and deployment diagnostics

- `AdBlockPolicy` now explicitly ignores non-HTTP(S) request URLs before host matching.
- Added regression cases for uppercase/trailing-dot normalization, strict domain suffix boundaries, lookalike YouTube Referer hosts and non-HTTP(S) schemes.
- Added a Python source-contract test proving Browser passes both top-level host and Referer context into the source-aware policy.
- Confirmed `mosaic-apk` is linked to the private MOSAIC repository. Deployment history still contains only the canceled production-target deployment; no Preview build exists and no APK is available.
- GitHub Actions run #192 failed before any job steps/logs were created; Android tests/build were skipped. No test/build success is claimed.
- The repository has no standalone `docs/RISKS.md`; `docs/SECURITY.md` and the State Ledger are the current security/risk references.


## 2026-11-12 — Ad Shield DNS reliability increment

- Hardened the IPv4 DNS packet parser against caller-supplied lengths larger than the backing buffer.
- The VPN packet loop now exits on tunnel EOF instead of repeatedly spinning on a negative read result.
- Android `DnsResolver.rawQuery` responses are checked against the original transaction ID, response flag, opcode, question count and question bytes before being forwarded to the TUN interface. Mismatched responses fail closed with SERVFAIL.
- Added Kotlin tests for out-of-range packet lengths, truncated buffers, fragmented IPv4 packets and inconsistent UDP lengths.
- Added Python source-contract checks for the tunnel EOF and upstream response guards.
- Tests are committed but not executed: GitHub Actions preflight still fails before job steps; Vercel's deployment API quota and Hobby Sandbox snapshot-storage limit block a Vercel build in this session.

- Ad Shield startup now clears stale persisted `running` state before VPN permission, Private DNS and tunnel-establishment checks, preventing a failed restart from leaving the UI stuck on a false active state.
- Browser policy documentation now accurately describes the YouTube top-level/Referer compatibility scope rather than implying a top-level-only exception.

- Vercel Sandbox diagnostics: the only existing `mosaic-apk-builder` snapshot is 829,834,562 bytes; the sandbox cannot clone the private repository without credentials and has no Java/Gradle toolchain. A new V4 Sandbox was rejected with 402 because Hobby snapshot storage is over limit. No snapshot was deleted and no paid upgrade was made.
\n\n## 2026-11-15 — Native queue persistence and paused restore\n\n- Added a bounded local-only queue snapshot codec/store in `core/media`. It preserves native queue item IDs, titles, creators, MIME types, current index and resume position, with a 100-item bound.\n- Queue persistence excludes non-HTTPS network schemes, signed/query-bearing URLs, fragments and credential-bearing locators; it retains eligible `content://`, `file://` and stable HTTPS references. Invalid rows are ignored and the selected item is remapped safely.\n- The single Media3 `PlaybackService` now snapshots its MOSAIC-owned queue on player events, every 10 seconds and before service destruction. It restores the queue and position paused when the service is recreated, so app startup does not unexpectedly restart audio. External app MediaSessions remain outside this queue.\n- Added `PlaybackQueueCodecTest` coverage for metadata/position round-trip, unsafe URL exclusion, selected-item fallback, malformed rows and the 100-item bound.\n- Code commits: `865ce00` (queue store/codec), `56b0218` (queue codec tests), `3aa94cc` (Media3 service restore), `c881961` (source cleanup), `a4c7abf` (malformed-row test correction), `5f9b194` (restore metadata cleanup), `e4c750b` (safe current-item fallback), `f238f73` (fallback regression test).\n- Verification: CI is pending. No build/test/lint/APK success or physical-device process-death acceptance is claimed for this increment.\n