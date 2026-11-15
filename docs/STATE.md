# State Ledger
_Last updated: 2026-11-15 by ChatGPT GPT-6 — native queue persistence and paused restore increment_

## 1. Product snapshot
MOSAIC is a local-first native Android personal media operating system. It unifies discovery, source access, playback continuity, a durable personal library and ad/tracker protection across local files, browser media, streams, podcasts/radio and third-party apps. The two primary pillars are cross-source background playback/control and broad ad/tracker blocking. Completion requires source-by-source real-device evidence, honest playback-ownership labels, measured filtering coverage/limits and a reliable unified media experience; CI success alone is not product acceptance. Canonical north star: `docs/MOSAIC_PRODUCT_VISION.md`; narrative requirements: `docs/PRODUCT_SPEC.md`; requirement/test traceability: `docs/SPEC.md`; architecture constraints: `docs/DECISIONS.md`.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android architecture | ✅ done | Kotlin/Compose multi-module project: `:app`, `:core:media`, `:core:library`, `:core:design`; one Media3 PlaybackService/session. |
| Home / Explore / navigation | 🟡 partial | Source-first command centre and Explore exist; main run #177 passed; physical usability/source handoff acceptance remains open. |
| Native local audio | 🟡 partial | Read-only MediaStore indexing, SAF open-file and Media3 service/queue exist; Redmi screen-off, permission recovery and Bluetooth acceptance not recorded. |
| Native HTTPS/HLS and podcast feeds | 🟡 partial | Validated HTTPS URL/HLS playback and bounded RSS/Atom feed parsing/queueing/local subscriptions exist with unit/contract tests; real endpoint and device acceptance pending. |
| Browser | 🟡 partial | Supporting WebView and direct HTTPS HTML5 audio/video handoff to Media3 exist; DRM/MSE/blob/cookie/header limitations remain; Browser-owned background playback not accepted. |
| External media sessions | 🟡 partial | Explicit notification-listener access discovers Android sessions and exposes advertised actions; source app owns the stream. Named-app/Redmi behavior remains unverified. |
| YouTube ad-skip helper | 🟡 partial | Opt-in YouTube-package-scoped accessibility helper is present and contract-checked; exact label variants/false positives require on-device testing. |
| Ad Shield browser filtering | 🟡 partial | Removed whole-page YouTube/Spotify exemptions. Known third-party ad/tracker domains are blocked on provider pages; Google ad infrastructure is blocked by default with a narrow YouTube top-level-page or embedded-frame Referer compatibility exception. New policy tests cover HTTP(S)-only filtering, domain boundaries, lookalike Referer rejection and provider compatibility, but are not yet executed; Redmi compatibility acceptance remains open. |
| Ad Shield device DNS filter | 🟡 partial | Opt-in DNS-only local VPN, allowlist, status, notification Stop and DNS self-test exist. Packet parsing now guards backing-buffer lengths, the tunnel loop stops on EOF, and system-resolver replies are checked against the query; tests are committed but not executed. IPv4 UDP only, lifecycle/routing coverage and Redmi acceptance remain open. |
| Named provider integrations | ⚪ not started / 🟡 partial | YouTube/Spotify/Facebook/Instagram remain targets; generic delegated MediaSession control exists, not native audio ingestion. |
| Durable unified Library | 🟡 partial | Read-only local audio index plus local playback history/resume (up to 50 safe references, Home resume entry points, 10-second/pause checkpoints); Library has Recent and clear-history controls; CI run #212 passed. Media3 now snapshots up to 100 safe native queue entries, current item and position and restores the queue paused after service restart; new codec tests are awaiting CI. Stable identity, collections and Room migrations remain open. |
| Documentation / traceability | 🟡 partial | Added this ledger and 11-row `docs/SPEC.md`; reconciled task/matrix/spec and updated README/legacy handoff. Branch CI has not executed tests/build due preflight runner assignment failure. |
| CI / build pipeline | 🟡 partial | Repository is public. GitHub Actions direct-build run #194 completed successfully on the active continuity branch, including source checks, Python tests, Android unit tests, lint, APK assembly, signing and checksum verification. Playback-history, resume and Library Recent/clear increment passed GitHub Actions run #212, including tests, lint, APK build, signing and checksum verification. The subsequent native queue persistence increment is committed on the continuity branch and awaits its own CI result. Vercel is not the active APK path. The development signing key remains exposed in the public repository and needs a controlled migration to GitHub Actions Secrets. |

## 3. Currently in flight
- Playback history first vertical slice — implemented on branch `continuity/state-ledger-bootstrap-2026-11-12`; full CI run #204 passed. It records actual MOSAIC-owned URI playback only, skips URLs with query/fragment/userinfo, exposes recent items on Home, checkpoints progress every 10 seconds/on pause, and resumes through Media3 when selected.
- Native queue persistence — `PlaybackQueueStore` and `PlaybackService` now save up to 100 safe native queue references, current item and position on player events, every 10 seconds and on service shutdown; the service restores the queue paused. Codec tests are added but awaiting CI; Android process-death and Redmi acceptance remain open.
- Product/device acceptance — Redmi Note 12 Pro 5G Home/Explore/Player, external-session, native source and Ad Shield checks are still unrecorded.
- Browser source-aware filtering — P3-015 is IN PROGRESS; whole-page provider exemptions are removed, with a narrow YouTube top-level/Referer Google-player compatibility allowlist. New unit tests need a successful run and Redmi acceptance.
- Native stream/feed and Browser handoff — source implementations and tests exist; real streams, feeds and browser media still need device acceptance.
- Ad Shield reliability — hardened malformed packet length handling, tunnel EOF exit and system-resolver response matching; new tests are awaiting execution.

## 4. Known gaps & stubs
- Browser filter policy now blocks known third-party ad/tracker hosts on YouTube/Spotify pages and blocks Google ad infrastructure by default. Only YouTube top-level pages or embedded-frame Referer contexts retain a narrow Google player compatibility allowlist; verify on-device before narrowing it further.
- Ad Shield service lifecycle, VPN revocation, ordinary-traffic routing, upstream timeout/recovery and named-app compatibility do not have complete unit coverage; physical acceptance checklist `docs/AD_SHIELD_DEVICE_ACCEPTANCE.md` is unsigned.
- IPv4 UDP DNS interception does not cover DoH/custom DNS/IPv6 or same-host/in-stream advertising.
- External YouTube/Spotify/Facebook/Instagram native playback is not demonstrated; external MediaSession control remains delegated to the source app.
- Browser handoff cannot transfer DRM, MSE/blob, protected or cookie/header-dependent media; this is not general website extraction.
- Durable unified library remains incomplete: recent references and a bounded native queue/current-position snapshot are persisted locally. Stable cross-source identity, collections, Room/schema migrations, full Library integration and Android process-death/device acceptance remain open.
- No physical-device/ADB acceptance evidence is available in this session.

## 5. Environment & how to run
- Native Android project; open repository root in Android Studio compatible with AGP 9.1.1.
- Pinned baseline: Java 17, Gradle 9.3.1, AGP 9.1.1, compile SDK 37 (CI package `platforms;android-37.2`), Build Tools 36.0.0, target SDK 36, min SDK 26.
- Build: `./gradlew :app:assembleDebug`.
- Android unit tests: `./gradlew :core:media:testDebugUnitTest :app:testDebugUnitTest`.
- Lint: `./gradlew :app:lintDebug`.
- Python checks: `python3 tools/verify_source.py` and `python3 -m unittest discover -s tools/tests -v`.
- Expected APK: `app/build/outputs/apk/debug/app-debug.apk`. CI APKs use a persistent development signing identity, not a production release key.
- Vercel APK build: `vercel.json` invokes `scripts/vercel-build-apk.sh`; it installs Java 17 and Android SDK, runs checks/tests/lint/assemble, verifies the existing development signing identity and publishes the APK in `dist/`. The first deployment was canceled before build after its unexpected production target was detected. A Vercel API create-deployment request was rejected with api-deployments-free-per-day (100/day; retry after 24 hours). A separate Vercel Sandbox creation attempt returned 402 because the Hobby team's snapshot-storage limit is exceeded. No APK is produced or verified yet.
- Vercel project: `mosaic-apk` (`prj_17TuuD9O7qZQcjqJoPTxlydWt0sV`), linked to `ysfclc/MOSAIC`; production branch is `main`. No production deployment was created in this session.

## 6. Next three increments
1. Trigger and validate a Vercel Preview build from the current branch after the deployment API quota clears; confirm the build runs source checks, Python tests, Android unit tests, lint, APK assembly, signing fingerprint and checksum.
2. Run the expanded P3-015 Browser policy tests in the Vercel build and perform Redmi YouTube/Spotify compatibility checks; adjust the narrow Google-player allowlist only with evidence.
3. Complete the Redmi Note 12 Pro 5G acceptance for local audio, HTTPS/HLS, podcast feed, Browser handoff, YouTube/Spotify/social sessions and Ad Shield; record per-source outcomes in `docs/PROVIDER_CAPABILITY_MATRIX.md` and `docs/AD_SHIELD_DEVICE_ACCEPTANCE.md`.

## 7. Open questions for the human
- [ ] Is the Redmi Note 12 Pro 5G available for hands-on acceptance? Assume unavailable until confirmed; do not claim device acceptance without recorded results.

## Session Handoff

**Done this session**
- Confirmed the Vercel `mosaic-apk` project is linked to private repository `ysfclc/MOSAIC`; explicitly enabled Preview deployments and confirmed no live or Preview APK deployment exists.
- Hardened the source-aware Browser filter: only HTTP(S) requests are considered, host suffix boundaries are tested, and lookalike YouTube Referer hosts cannot activate the compatibility exception.
- Hardened DNS Ad Shield packet handling: invalid backing-buffer lengths are rejected, the packet reader exits on EOF, stale persisted running state is cleared before startup checks, and system-resolver replies must match the original DNS transaction/question.
- Implemented the first P3-015 source-aware Browser filtering slice: removed blanket provider-page exemptions, blocked known third-party ad/tracker hosts on provider pages, and added a narrow YouTube-only Google player compatibility allowlist that recognizes top-level and embedded-frame Referer context.
- Added deterministic Android SDK license acceptance to the build script.
- Saved Vercel project build settings (build command, install command, output directory and Node 22).

**State changed**
- vercel.json — Vercel build command, static output directory and APK download headers.
- scripts/vercel-build-apk.sh — Java 17/Android SDK bootstrap, source/Python/Android checks, lint, debug APK assembly, signing-certificate verification, checksum and static download page.
- docs/VERCEL_APK_BUILD.md — build flow, artifact contract and release boundary.
- docs/STATE.md, docs/CHANGELOG.md, README.md — updated build target and continuity.
- AdBlockPolicy.kt, AdBlockPolicyTest.kt, BrowserScreen.kt — source-aware filter behavior, regression tests and truthful Browser status copy.
- tools/tests/test_project_contract.py — Browser source/Referer and Ad Shield DNS safety integration contracts.
- docs/TASKS.md, docs/SPEC.md and docs/CHANGELOG.md — recorded the new Ad Shield reliability increment.
- docs/SPEC.md, docs/TASKS.md, docs/PROVIDER_CAPABILITY_MATRIX.md — updated requirement evidence and P3-015 status.

**Verified**
- Vercel project created: mosaic-apk (prj_17TuuD9O7qZQcjqJoPTxlydWt0sV), linked to private repo; no existing Vercel project was modified.
- Deployment dpl_AswuWhBgWLhh1NT4nid8Pb5Eh23X was canceled before build because its metadata unexpectedly reported production for the continuity branch. It never went live and produced no APK.
- A direct Vercel deployment API request hit the Hobby team's api-deployments-free-per-day limit (100/day; retry after 24 hours). The existing `mosaic-apk-builder` Sandbox has one 829,834,562-byte snapshot; its private Git clone attempt failed because the sandbox has no repository credentials, and Java/Gradle were not preinstalled. Creating a new V4 Sandbox returned 402 (`Snapshots Storage` Hobby limit exceeded). No snapshot was deleted and no plan upgrade was attempted. No APK was built.
- The Android build script now uses a finite 100-response SDK license input to avoid yes/SIGPIPE failure with pipefail.
- P3-015 Kotlin tests and the Python Browser integration contract were expanded and source-reviewed; none were executed because the Vercel deployment quota and GitHub Actions runner issue remain unresolved.
- Ad Shield packet codec tests and Python source-contract coverage were expanded; none were executed.
- Confirmed the repository has no standalone `docs/RISKS.md`; the security baseline is `docs/SECURITY.md` and project risks are recorded in this ledger.
- Vercel Preview deployments are enabled, but Git pushes still did not create a deployment. The Vercel Sandbox build alternative returned 402 (`Snapshots Storage` Hobby limit exceeded); no plan upgrade or snapshot deletion was attempted.
- Latest GitHub Actions branch run #192 (commit 8fce7d8bb800eed12e9e0b133ff06c8ae56ad22b) failed in the preflight job before any build/test steps ran; downstream Android jobs were skipped. The job had no logs/steps, consistent with runner provisioning failure. No branch CI pass is claimed.

**Not done / next**
1. Resolve the Vercel Hobby snapshot-storage limit only with explicit approval, or wait for the deployment API quota reset; then trigger a Preview deployment and confirm target=preview before inspecting logs.
2. Fix build errors, run the expanded Browser and Ad Shield unit tests, and verify APK signature, checksum and deployed download.
3. Validate Browser YouTube/Spotify compatibility and in-place APK installation on the Redmi Note 12 Pro 5G.

**Open questions**
- Redmi Note 12 Pro 5G availability for hands-on acceptance; default assumption is unavailable.

**Risks / landmines**
- No APK from this Vercel pipeline exists yet; do not present any earlier APK as this build.
- The first deployment was canceled because Vercel reported a production target for a continuity-branch commit. Ensure the next deployment is preview and production remains on main.
- The APK is a development-signed debug build, not a production release.
- The persistent development signing key is exposed in the public repository. Move it to GitHub Actions Secrets and rotate it before treating future public builds as trusted releases; plan the one-time signer migration so ordinary updates do not unexpectedly fail.
- Named provider playback and physical-device behavior remain unproven; delegated external controls must not be described as native playback.


## 2026-11-14 — successful public GitHub Actions APK build

- Repository visibility: public.
- Built branch: `continuity/state-ledger-bootstrap-2026-11-12`.
- Source commit: `8fce7d8bb800eed12e9e0b133ff06c8ae56ad22b`.
- Workflow run: [#192, attempt 2](https://github.com/ysfclc/MOSAIC/actions/runs/43300807250) — SUCCESS.
- Source verification, Python contract tests, Android unit tests, lint, debug APK assembly, APK signature verification and SHA-256 verification passed.
- APK release: [MOSAIC development build #192](https://github.com/ysfclc/MOSAIC/releases/tag/mosaic-dev-build-192).
- APK size: 22,990,870 bytes.
- APK SHA-256: `ee537cadfa063f9b1736f1e48119c853b1c80a979a61dc90338307011605639d`.
- Artifact: `MOSAIC-Android-debug-APK` (GitHub artifact ID `14131795848`; expires 2026-11-28).
- Security follow-up: the persistent development signing key is present in the now-public repository. It is not a production key; move it to GitHub Actions Secrets and rotate it before treating future public builds as trusted releases. Rotation requires a one-time signer migration for existing installs.
- No physical Redmi device acceptance is claimed.


## 2026-11-14 — playback history first vertical slice

- Added `PlaybackHistoryStore` in the app's private SharedPreferences. It stores up to 50 media references/metadata entries, not media bytes.
- History is recorded only when MOSAIC's own Media3 session is actively playing. External app sessions are not misrepresented as MOSAIC-owned history.
- Entries with URL query strings, fragments or userinfo are intentionally excluded so signed URLs and obvious embedded credentials are not persisted.
- Home now displays the four most recent entries with one-tap playback.
- Added codec tests for Unicode and optional metadata round-trip, malformed-row tolerance, deduplication and the 50-entry cap.
- Task P2-002 is IN PROGRESS; durable queue/resume, unified identity, collections, Room migrations and Library integration remain open.
- Code commits: `8c4697a` (store), `5689057` (codec tests), `114941f` (Home UI), `5be23e2` (active-playback and URL-safety guard), `3d7d855` (deduplicate before applying history cap), `b313e36` (preserve live resume position during history updates), `a7fc8cc` (Library recent-history and clear-history UI).
- Verification: GitHub Actions run [#212](https://github.com/ysfclc/MOSAIC/actions/runs/43652261154) — SUCCESS on commit `a7fc8cce23e71f43de9ac1c1477937ac91fc1114`. Source verification, Python contract tests, Android unit tests (including all 5 history codec tests), lint, APK assembly, signing and checksum verification passed. APK release: [mosaic-dev-build-212](https://github.com/ysfclc/MOSAIC/releases/tag/mosaic-dev-build-212); size 23,007,254 bytes; SHA-256 `7266805b5f306a1eb59d4778d6b4c0b532ef7ef34ad6191a5ff7985fd2b6c4d9`. No physical Redmi device acceptance is claimed.
- No physical Redmi device acceptance is claimed.
