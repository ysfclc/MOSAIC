# State Ledger
_Last updated: 2026-11-12 by ChatGPT GPT-6 — session 1 (continuity bootstrap)_

## 1. Product snapshot
MOSAIC is a local-first native Android personal media operating system. It aims to unify discovery, source access, playback continuity, a durable personal library and ad/tracker protection across local files, browser media, streams, podcasts/radio and third-party apps. The two primary pillars are cross-source background playback/control and broad ad/tracker blocking. Completion means source-by-source behavior is demonstrated on a real Android device, ownership and capability are labelled honestly, and Ad Shield's coverage/limits are measured; CI success alone is not product acceptance. Canonical vision: `docs/MOSAIC_PRODUCT_VISION.md`; canonical detailed spec: `docs/PRODUCT_SPEC.md`; architecture constraints: `docs/DECISIONS.md`.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android architecture | ✅ done | Kotlin/Compose multi-module project: `:app`, `:core:media`, `:core:library`, `:core:design`; see `settings.gradle.kts` and ADR-001/002/007. |
| Home / Explore / navigation | 🟡 partial | Source-first command centre and Explore exist; run #177 passed; physical usability/source handoff acceptance remains open. |
| Native local audio | 🟡 partial | MediaStore indexing, SAF open-file and Media3 service/queue exist; Redmi screen-off/device acceptance not recorded. |
| Native HTTPS/HLS and podcast feeds | 🟡 partial | Source state reports URL playback, HLS and bounded RSS/Atom parsing/subscriptions; tests exist, but source/device acceptance is pending and the matrix/task board need reconciliation. |
| Browser | 🟡 partial | Supporting WebView and conservative host filter exist; direct HTML5 media handoff is reported implemented, with DRM/MSE/blob/cookie limitations; not a complete browser. |
| External media sessions | 🟡 partial | Explicit notification-listener access discovers exposed Android sessions and controls advertised actions; source app retains playback ownership; real named-app acceptance is pending. |
| YouTube ad-skip helper | 🟡 partial | Opt-in, YouTube-package-scoped accessibility helper is present; verify exact labels and false-positive behavior on-device. |
| Ad Shield browser filtering | 🟡 partial | Conservative browser host policy exists; coverage and compatibility remain limited. |
| Ad Shield device DNS filter | 🟡 partial | Opt-in DNS-only local VPN, rule/allowlist, status, notification Stop and DNS self-test are reported CI-verified; IPv4 UDP-only and real-device/named-app acceptance remain open. |
| Named provider integrations | ⚪ not started / 🟡 partial | YouTube/Spotify/Facebook/Instagram are targets; current delegated MediaSession control is not native source playback. Spotify App Remote/native provider routes remain unproven. |
| Documentation / traceability | 🔴 broken | `docs/STATE.md` was missing; `docs/SPEC.md` is missing (detailed spec is `docs/PRODUCT_SPEC.md`); task/matrix entries are inconsistent with newer implementation notes. |
| CI / build pipeline | ✅ done | Pinned Java 17, Gradle 9.3.1, AGP 9.1.1, SDK 37 baseline; latest main workflow run #177 completed successfully (2026-11-06). |

## 3. Currently in flight
- Product experience reset / source-first journeys — CI run #177 is green; Redmi app-launch, return-to-MOSAIC, session and screen-off acceptance remains unrecorded.
- Phase 1 / Ad Shield acceptance — device checklist exists at `docs/AD_SHIELD_DEVICE_ACCEPTANCE.md`; no physical-device sign-off is recorded.
- Provider capability investigation — update `docs/PROVIDER_CAPABILITY_MATRIX.md` with implementation and device evidence without overstating native playback or ad removal.
- Native stream, podcast and browser handoff — code/tests are present in the tree; reconcile stale task/matrix statements before claiming completion.

## 4. Known gaps & stubs
- `docs/STATE.md` — absent in the baseline; this file bootstraps it.
- `docs/SPEC.md` — absent; `docs/PRODUCT_SPEC.md` currently serves as the detailed canonical specification. Add requirement IDs and a traceability matrix only after the current test suite and implementation are inspected.
- `docs/PROVIDER_CAPABILITY_MATRIX.md` — stale rows state that RSS/radio/direct URL/browser handoff are unimplemented despite corresponding code files and the handoff; reconcile against code and tests.
- `docs/TASKS.md` — product experience, native stream/podcast, browser handoff, and Ad Shield task statuses may lag implementation; inspect before changing status.
- External provider native playback (YouTube, Spotify, Facebook, Instagram) — not demonstrated; external MediaSession control is delegated control, not audio transfer.
- Device acceptance — Redmi Note 12 Pro 5G screen-off playback, provider behavior, Ad Shield DNS, VPN recovery and notification lifecycle are not evidenced as completed.
- Ad Shield limitations — IPv4 UDP DNS only; DoH/custom resolvers/IPv6 and same-host/in-stream ads can bypass; measure compatibility and false positives.
- No physical-device/ADB session or device-test evidence is available in this repository snapshot.

## 5. Environment & how to run
- Native Android project; open repository root in Android Studio compatible with AGP 9.1.1.
- Toolchain pinned in repo: Java 17, Gradle 9.3.1, AGP 9.1.1, compile SDK 37 (CI package `platforms;android-37.2`), Build Tools 36.0.0, target SDK 36, min SDK 26.
- Build: `./gradlew :app:assembleDebug`.
- Android unit tests: `./gradlew :core:media:testDebugUnitTest :app:testDebugUnitTest`.
- Lint: `./gradlew :app:lintDebug`.
- Python contract/source checks: inspect `requirements-dev.txt`, `tools/verify_source.py`, `tools/tests/test_project_contract.py` and `docs/TESTING.md` before selecting the exact local invocation.
- Expected APK: `app/build/outputs/apk/debug/app-debug.apk`. CI artifacts use a persistent development signing identity, not a production release key.

## 6. Next three increments
1. Reconcile the current implementation, unit/contract tests, `docs/TASKS.md` and `docs/PROVIDER_CAPABILITY_MATRIX.md` for native stream/podcast/browser-handoff claims; add requirement IDs and traceability once each path is confirmed.
2. Verify the latest source-first Home/Explore/Player behavior and Ad Shield on the Redmi Note 12 Pro 5G; record exact app/device versions and per-source evidence.
3. Complete the smallest source-specific integration with a measurable acceptance test (prioritize based on the reconciled task board and actual device findings); preserve the shared Media3 ownership model and explicit external-session labels.

## 7. Open questions for the human
- [ ] Is the Redmi Note 12 Pro 5G available for hands-on acceptance during the next session? Assume no remote device access until explicitly available.
- [ ] None of the architecture or vendor boundaries need reconsideration now; locked ADRs remain authoritative unless evidence requires a superseding ADR.
