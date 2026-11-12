# State Ledger
_Last updated: 2026-11-12 by ChatGPT GPT-6 — session 1 (continuity bootstrap and reconciliation)_

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
| Ad Shield browser filtering | 🔴 broken against locked requirement | Conservative host filter exists, but blanket YouTube/Spotify page/asset and Google ad-infrastructure exclusions conflict with ADR-014/015 and PRODUCT_SPEC.md §11; P3-015 remains open. |
| Ad Shield device DNS filter | 🟡 partial | Opt-in DNS-only local VPN, allowlist, status, notification Stop and DNS self-test exist; IPv4 UDP only, lifecycle/routing unit coverage is incomplete, and Redmi acceptance is pending. |
| Named provider integrations | ⚪ not started / 🟡 partial | YouTube/Spotify/Facebook/Instagram remain targets; generic delegated MediaSession control exists, not native audio ingestion. |
| Durable unified Library | ⚪ not started | Read-only local audio index exists; durable media identity, history, collections, resume/migrations remain TODO. |
| Documentation / traceability | 🟡 partial | Added this ledger and 11-row `docs/SPEC.md`; reconciled task/matrix/spec and updated README/legacy handoff. Branch CI has not executed tests/build due preflight runner assignment failure. |
| CI / build pipeline | 🟡 partial | Main run #177 passed all direct-build checks. Branch runs #178–#183 failed before runner assignment (`runner_id: 0`, zero steps); Android build/test jobs were skipped. Do not treat branch documentation as CI-verified. |

## 3. Currently in flight
- Continuity and requirements reconciliation — complete as a documentation change on branch `continuity/state-ledger-bootstrap-2026-11-12`; requires a successful CI run before merge.
- Product/device acceptance — Redmi Note 12 Pro 5G Home/Explore/Player, external-session, native source and Ad Shield checks are still unrecorded.
- Browser source-aware filtering — P3-015 remains TODO until provider exclusions are replaced by tested compatibility rules; no blind rule deletion.
- Native stream/feed and Browser handoff — source implementations and tests exist; real streams, feeds and browser media still need device acceptance.

## 4. Known gaps & stubs
- `AdBlockPolicy.kt` still exempts YouTube/Spotify pages/assets and Google ad infrastructure; this conflicts with the locked no-blanket-exemption requirement. Existing tests assert the exemption. Resolve with source-aware compatibility rules and regression tests, not by blindly blocking provider/CDN hosts.
- Ad Shield service lifecycle, VPN revocation, ordinary-traffic routing, upstream timeout/recovery and named-app compatibility do not have complete unit coverage; physical acceptance checklist `docs/AD_SHIELD_DEVICE_ACCEPTANCE.md` is unsigned.
- IPv4 UDP DNS interception does not cover DoH/custom DNS/IPv6 or same-host/in-stream advertising.
- External YouTube/Spotify/Facebook/Instagram native playback is not demonstrated; external MediaSession control remains delegated to the source app.
- Browser handoff cannot transfer DRM, MSE/blob, protected or cookie/header-dependent media; this is not general website extraction.
- Durable unified library (identity, history, queue persistence, collections, resume and migrations) remains unimplemented.
- No physical-device/ADB acceptance evidence is available in this session.

## 5. Environment & how to run
- Native Android project; open repository root in Android Studio compatible with AGP 9.1.1.
- Pinned baseline: Java 17, Gradle 9.3.1, AGP 9.1.1, compile SDK 37 (CI package `platforms;android-37.2`), Build Tools 36.0.0, target SDK 36, min SDK 26.
- Build: `./gradlew :app:assembleDebug`.
- Android unit tests: `./gradlew :core:media:testDebugUnitTest :app:testDebugUnitTest`.
- Lint: `./gradlew :app:lintDebug`.
- Python checks: `python3 tools/verify_source.py` and `python3 -m unittest discover -s tools/tests -v`.
- Expected APK: `app/build/outputs/apk/debug/app-debug.apk`. CI APKs use a persistent development signing identity, not a production release key.
- This session's `/testbed` mount was empty; no local checkout, Gradle build or local test run was available. Branch CI also failed before runner assignment.

## 6. Next three increments
1. Restore a runnable GitHub Actions validation path for the continuity branch (inspect runner/preflight provisioning; retry only after runner assignment works) and require source verifier, Python tests, Android unit tests, lint and APK assembly to pass before merge.
2. Implement P3-015 as a narrow source-aware Browser filtering slice: define source/context-specific behavior, remove blanket exemptions only with compatibility evidence, and replace the tests that currently bless the exemption.
3. Complete the Redmi Note 12 Pro 5G acceptance for local audio, HTTPS/HLS, podcast feed, Browser handoff, YouTube/Spotify/social sessions and Ad Shield; record per-source outcomes in `docs/PROVIDER_CAPABILITY_MATRIX.md` and `docs/AD_SHIELD_DEVICE_ACCEPTANCE.md`.

## 7. Open questions for the human
- [ ] Is the Redmi Note 12 Pro 5G available for hands-on acceptance? Assume unavailable until confirmed; do not claim device acceptance without recorded results.

## Session Handoff

**Done this session**
- Continuity ledger and product-capability reconciliation — added `docs/STATE.md` and `docs/SPEC.md`; corrected stale source/task statements and updated continuity pointers on branch `continuity/state-ledger-bootstrap-2026-11-12`.

**State changed**
- `docs/STATE.md` — canonical session ledger and this handoff.
- `docs/SPEC.md`, `docs/PRODUCT_SPEC.md` — 11 requirement IDs, evidence links and current capability summary.
- `docs/TASKS.md`, `docs/PROVIDER_CAPABILITY_MATRIX.md` — corrected partial implementation statuses and explicit ad-filtering deviation.
- `docs/CHANGELOG.md`, `README.md`, `PROJECT_STATE.md` — dated history and canonical ledger pointers.

**Verified**
- Build: not run for this branch. Main baseline run #177 passed `:core:media:testDebugUnitTest`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and signing/checksum checks.
- Tests: main baseline source verifier and 16 Python contract tests passed in run #177. Branch runs #178–#183 failed preflight before runner assignment (`runner_id: 0`, zero steps); Android test/build jobs were skipped. No branch-specific test pass is claimed.

**Not done / next**
1. Restore runner assignment and obtain a fully green validation run for this branch before merging.
2. Work on P3-015 Browser source-aware filtering with compatibility tests.
3. Perform Redmi device/source and Ad Shield acceptance when the phone is available.

**Open questions**
- Redmi Note 12 Pro 5G availability for hands-on acceptance; default assumption is unavailable.

**Risks / landmines**
- Browser policy blanket exemptions contradict ADR-014/015; existing tests encode that deviation.
- CI branch runs have not executed a single step due pre-runner failures; this is not evidence of a code/test failure, but the branch must not be merged as verified.
- Named provider playback and all physical-device behavior remain unproven; delegated external controls must not be described as native playback.
