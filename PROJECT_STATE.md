# MOSAIC project state — session handoff

**Project:** MOSAIC — cross-source background media player + Ad Shield  
**Repository:** ysfclc/MOSAIC · **Branch:** main  
**Vision lock:** ADR-014 and ADR-015 in docs/DECISIONS.md  
**Architecture:** native Android, Kotlin, Jetpack Compose, Media3  
**Target device:** Redmi Note 12 Pro 5G  
**Current phase:** Player control foundation verified; Ad Shield first implementation built and CI-verified; physical-device acceptance and native source playback remain open.

## Locked product vision

MOSAIC solves two connected problems: media is fragmented across apps and websites, and ads interrupt the experience. It has two equally central pillars:

1. **Cross-source background playback and control.** Play media from other sources in the background wherever technically feasible. Target YouTube, Spotify, Facebook, Instagram, browser media, podcasts, radio, direct streams and local files. Continue playback while switching apps or turning the screen off. Prefer MOSAIC's shared Media3 player when an accessible route exists; otherwise investigate source-owned background continuation and Android-exposed MediaSession controls. Label playback ownership honestly.
2. **Broad ad/tracker blocking.** Block ads wherever technically possible, not only inside MOSAIC Browser. Build a user-controlled browser filter and an explicitly enabled Android local VPN/DNS/host filter for cross-app coverage. Do not blanket-exempt named providers; use source-aware rules and test playback compatibility.

The browser is a supporting source surface, not the product. Copy/paste, bookmarks and clipboard history are not the founding problem or primary product requirement. ADR-013 is withdrawn in full. ADR-014 and ADR-015 are the locked source of truth.

Respect platform, security and content-access boundaries. These boundaries do not remove providers from research. DNS/host filtering cannot reliably distinguish same-host or in-stream ads from content; measure and explain gaps rather than claiming universal removal.

## Current repository implementation

- Multi-module Android project: :app, :core:media, :core:library, :core:design.
- Compose Home / Library / Player UI and dark visual system.
- MediaStore audio catalog with read-only indexing; Storage Access Framework open-file playback.
- One Media3 ExoPlayer in MediaSessionService, controller connection and local queue.
- Provider-neutral MediaSourceAdapter and explicit capability model.
- Basic WebView browser with conservative third-party host filter; not a complete browser or the core product.
- AdBlockPolicy currently has blanket YouTube/Google-video and Spotify-domain exemptions. This is legacy behavior to replace with tested source-aware rules; do not blindly remove exemptions until rules are tested against media delivery.
- ExternalMediaSessionBridge is now polled at app level and surfaced in the primary Player, not Browser. Player cards show source app label/title/artist and expose play/pause, previous/next and seek only when the external session advertises those actions. Notification-listener access remains explicit; notification text is not read.
- This is source-owned remote control, not native audio playback and not a way to force an app to continue when it disallows background playback. Real-app/Redmi verification remains pending.
- Native background playback from YouTube, Spotify, Facebook, Instagram and other external sources is not yet demonstrated.
- First device-wide DNS-only Ad Shield slice is implemented and CI-verified: explicit VPN consent, split-route IPv4 DNS interception, curated ad/tracker rules, editable hostname allowlist, persistent notification with Stop, conflict warning, status/error/blocked-domain display, and Private DNS preservation via Android's system resolver on Android 10+. Android 9 with Private DNS enabled fails closed.
- Current filter only parses IPv4 UDP DNS. Apps using DoH/custom resolvers and same-domain or in-stream ads can bypass it; IPv6 DNS interception and named-app compatibility testing remain open.
- Unified native queue for supported external media is not implemented.

## APK installation and update compatibility

The earlier CI workflow assembled debug APKs using each hosted runner's temporary Android debug keystore. The signing certificate therefore changed between builds, so Android rejected subsequent APKs as updates. This was a delivery-pipeline defect, not intended app behavior.

The CI workflow now decodes one persistent **development-only** signing key from `tools/mosaic-debug-signing-key.b64`, configures Gradle to sign debug APKs with it, verifies the certificate fingerprint before artifact upload, and sets versionCode from the monotonically increasing GitHub Actions run number. This was verified in [run 42055276990](https://github.com/ysfclc/MOSAIC/actions/runs/42055276990), commit `311fa7ebb628e2282a60662cd13b7114cce51701`, versionCode `80`. Unit tests, lint, APK assembly and signing-certificate verification passed. APK SHA-256: `23778ffc59b9e3921ee98ab158a3d08c7ee0c5da155fbeb7993612e59717dd0a`. The key is not a production/release signing identity.

**Migration note:** the private signing key for already-installed ephemeral CI APKs cannot be recovered from their APKs. The first APK signed with the stable key may therefore require one final uninstall/reinstall. After that migration, ordinary MOSAIC CI updates should install over the existing app and retain app data. Do not install an older ephemeral-key artifact after migration.

## Toolchain and last known verification

- Android Gradle Plugin: 9.1.1; Gradle: 9.3.1; Java: 17.
- Android compile SDK: 37 (published package `platforms;android-37.2`); target SDK: 36; minimum SDK: 26; Build Tools: 36.0.0.
- Latest successful integrated build before Ad Shield: [run 42034925797](https://github.com/ysfclc/MOSAIC/actions/runs/42034925797), Player control foundation.
- Latest Ad Shield build: [run 42051683244](https://github.com/ysfclc/MOSAIC/actions/runs/42051683244), commit `66ff12aa623be7f6e4873d4e37ca24ddfdb24e60`. `:core:media:testDebugUnitTest`, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug` all passed.
- Ad Shield debug APK: 22,180,455 bytes; SHA-256 `a75d4d3b4bdc8c97436e12459833c361d5070a276ff89ed7e0d17d29c71471a3`. GitHub artifact ID `13425814399` (expires 2026-11-19).
- Direct edits to app/core/tools/docs/build configuration trigger a separate build/test/lint/APK path. Manual workflow dispatch offers `direct` or `import`; the original ZIP importer remains intact.
- No successful physical-device test is recorded. Do not claim screen-off, lock-screen, headset or Bluetooth acceptance on the Redmi.

## Product delivery sequence

1. Preserve and verify the local Media3 foundation; complete pending Redmi device acceptance when the device is available.
2. Build source adapters and background playback paths: RSS, radio, direct media, browser handoff and source-owned external sessions. Investigate YouTube, Spotify, Facebook and Instagram individually.
3. Build Ad Shield: source-aware browser rules plus user-enabled device-wide VPN/DNS/host filtering, diagnostics, allowlists, safe recovery and compatibility tests.
4. Improve provider integrations and unified playback UX without pretending external sessions are native MOSAIC queue items.
5. Continue durable library, video and intelligence work only after the playback and Ad Shield foundations are sound.

## Immediate next engineering task

**P3-008 — complete device acceptance for Ad Shield.** The first implementation is built and CI-verified, but it remains IN PROGRESS until the Redmi Note 12 Pro 5G checks pass: ordinary DNS resolution, known blocked domain, allowlist override, upstream timeout, unrelated app traffic, VPN stop/revoke recovery, foreground notification and lock-screen behavior. Then expand P3-013 to named-app playback/ads and DoH/Private DNS coverage.

P3-007 Player controls and the first P3-008 Ad Shield slice are both CI-verified. Real-app and physical-device acceptance remain pending. P3-014 is CI-verified; the remaining update-path check is installing the stable-key APK over the current app after the one-time signature migration.

## Required handoff discipline

- Update docs/TASKS.md in the same change as implementation status.
- Update PROJECT_STATE.md with exact commit/run IDs, test results and remaining limitations.
- Never mark a source or ad-filter capability DONE without real evidence.
- Never claim a build or device test that was not run.
- Keep work in small, independently verifiable steps; no unrelated redesign or ZIP handoff unless requested.
