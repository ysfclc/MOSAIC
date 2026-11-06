# MOSAIC project state — session handoff

**Project:** MOSAIC — cross-source background media player + Ad Shield  
**Repository:** ysfclc/MOSAIC · **Branch:** main  
**Vision lock:** ADR-014 and ADR-015 in docs/DECISIONS.md  
**Architecture:** native Android, Kotlin, Jetpack Compose, Media3  
**Target device:** Redmi Note 12 Pro 5G  
**Current phase:** Native HTTPS/HLS playback, secure podcast feeds, local feed subscriptions and direct HTML5 browser-media handoff are CI-verified. A native Media3 video surface is implemented and awaiting its integrated CI run. Physical-device/source acceptance remains open.

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
- Follow-up Ad Shield hardening: upstream UDP sockets now fail over if binding to the selected underlying network fails, are connected to the selected resolver to reject replies from other endpoints, and the VPN's underlying-network list is refreshed even when it becomes empty. Upstream DNS responses are checked for transaction ID, response flag, opcode, question count and exact question bytes before forwarding. Regression tests and source-verifier checks passed in [GitHub Actions run 42094128331](https://github.com/ysfclc/MOSAIC/actions/runs/42094128331), commit `e0f51143c400556d697b9047502b59b2fe6a7de8`. The run passed source verification, Python contract tests, Android unit tests, lint, APK assembly, checksum and persistent signer verification. APK artifact ID: `13445249944`. Physical-device acceptance remains open.
- Unified native queue for supported external media is not implemented.

## Android-native cross-app control implementation (new)

- The primary Player already uses Android `MediaSessionManager` plus explicit Notification Listener access to discover external sessions and send only the transport actions each source advertises. This is the no-provider-API route for Spotify, YouTube and other apps that expose a session; the source app still owns the stream.
- Added `AdSkipAccessibilityService`, an opt-in Android AccessibilityService restricted by its service configuration to the YouTube package. It searches visible UI nodes for exact ad-specific labels in English, Polish and Turkish and clicks the nearest clickable ancestor. Generic “Skip” labels are deliberately excluded to avoid skipping tracks or videos. It does not extract audio, mute/strip ad media, or force YouTube background continuation.
- Added a Player panel that links to Android Accessibility settings and reports whether the helper is enabled. Notification Listener access and Accessibility access remain separate, explicit user grants.
- This slice passed source verification, Python contract tests, Android unit tests, lint, APK assembly, checksum and stable signing in [GitHub Actions run 42110725075](https://github.com/ysfclc/MOSAIC/actions/runs/42110725075), commit `010c85899bdcfe3669a44ce4c27e7d78e844f046`. It is not device-accepted. Test on Redmi with YouTube foreground, ad-skip UI variants, false-positive cases, app switching and screen-off behavior. The feature may do nothing when YouTube changes labels or renders controls inaccessible to Android accessibility.
- The broader Ad Shield remains a separate local VPN/DNS capability; it can block separable host-based ad/tracker requests but cannot reliably remove same-host or in-stream ads.

## Native HTTPS stream, radio and podcast playback slice (new)

- Added a Player URL entry that sends validated HTTPS audio URLs to the app-owned Media3 `MediaSessionService`; this is genuine MOSAIC-owned playback, not a remote control of another app.
- Added the Media3 HLS extension so HTTPS HLS radio streams (`.m3u8`) can be handled by ExoPlayer alongside supported progressive audio formats.
- Added a pure-JVM URL validator and tests: accepts HTTPS URLs, rejects HTTP/cleartext, non-network schemes, missing hosts and embedded credentials. HTTP-only radio stations are deliberately not enabled because no scoped cleartext policy has been designed.
- Added bounded HTTPS feed loading and a secure RSS 2.0 / Atom parser. DOCTYPE and external entities are disabled, feed size is capped at 2 MB, only HTTPS enclosures are admitted, and selecting an episode places the feed's playable episodes into the shared Media3 queue.
- Podcast feed URLs are saved in device-local SharedPreferences after a successful load. The Player lists saved feeds for one-tap reload and allows removing subscriptions; no account or cloud sync is involved.
- The native stream/podcast/feed-subscription slice passed source verification, Python contract tests, Android unit tests, lint, APK assembly and persistent signer verification in [run 42159518077](https://github.com/ysfclc/MOSAIC/actions/runs/42159518077), commit `52b839f55c5cc2b2dce52b656e188246bece9496`. A private prerelease was created at [MOSAIC development build #137](https://github.com/ysfclc/MOSAIC/releases/tag/mosaic-dev-build-137). APK SHA-256: `6a61bd8892d475e901685a235d6aa650337f0b3359452c5c93d091200dcf607d`.
- On-device verification remains pending: test at least one HTTPS MP3/AAC stream, one HLS radio URL and one real podcast feed, then screen-off, lock-screen, notification and Bluetooth controls on the Redmi.

## Browser direct-media handoff (new)

- Added a user-triggered Browser action that inspects HTML5 `audio` and `video` elements for a direct HTTPS `currentSrc` / `src`, then offers to send the URL to MOSAIC's Media3 player.
- The handoff re-validates the URL with the same HTTPS-only validator. It does not inject a JavaScript bridge, scrape arbitrary network traffic, or extract protected media.
- Explicit limitations: DRM-protected playback, Media Source Extensions streams, `blob:` URLs, segmented app-internal playback and sources requiring unprovided cookies/headers are not transferable. A visible HTML5 media element is required; this is not universal website extraction.
- Integrated browser handoff and source-contract regression checks are awaiting the latest integrated CI run. Device acceptance is still required.


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

P3-010 now includes direct HTTPS streams, HLS radio, RSS/Atom podcast queueing and locally saved podcast feeds. These native playback/feed features passed CI run 42159518077 and were published as private prerelease #137. Browser direct HTML5 media handoff is implemented with explicit DRM/MSE/blob limitations and is awaiting the latest integrated CI run. Next priority: validate real station/feed/browser media URLs and continue P3-008 Ad Shield and P3-018 YouTube/Spotify acceptance on-device. P3-018 native external session controls/ad-skip helper passed CI run 42110725075 but still needs real YouTube/Spotify tests on the Redmi. P3-008 Ad Shield physical acceptance also remains open.

The follow-up DNS-routing hardening passed [GitHub Actions run 42094128331](https://github.com/ysfclc/MOSAIC/actions/runs/42094128331) on commit `e0f51143c400556d697b9047502b59b2fe6a7de8`. Source verification, Python contract tests, Android unit tests, lint, APK assembly, APK checksum and persistent signing-certificate checks all passed. APK artifact ID: `13445249944`. Physical-device acceptance remains unperformed.

**P3-008 — complete device acceptance for Ad Shield.** The first implementation is built and CI-verified, but it remains IN PROGRESS until the Redmi Note 12 Pro 5G checks pass: ordinary DNS resolution, known blocked domain, allowlist override, upstream timeout, unrelated app traffic, VPN stop/revoke recovery, foreground notification and lock-screen behavior. Then expand P3-013 to named-app playback/ads and DoH/Private DNS coverage.

P3-007 Player controls and the first P3-008 Ad Shield slice are both CI-verified. Real-app and physical-device acceptance remain pending. P3-014 is CI-verified; the remaining update-path check is installing the stable-key APK over the current app after the one-time signature migration.

## Required handoff discipline

- Update docs/TASKS.md in the same change as implementation status.
- Update PROJECT_STATE.md with exact commit/run IDs, test results and remaining limitations.
- Never mark a source or ad-filter capability DONE without real evidence.
- Never claim a build or device test that was not run.
- Keep work in small, independently verifiable steps; no unrelated redesign or ZIP handoff unless requested.
