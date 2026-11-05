# MOSAIC project state — session handoff

**Project:** MOSAIC — cross-source background media player + Ad Shield  
**Repository:** ysfclc/MOSAIC · **Branch:** main  
**Vision lock:** ADR-014 and ADR-015 in docs/DECISIONS.md  
**Architecture:** native Android, Kotlin, Jetpack Compose, Media3  
**Target device:** Redmi Note 12 Pro 5G  
**Current phase:** Phase 1 playback foundation; Phase 2 cross-source playback is next.

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
- Device-wide VPN/DNS ad filtering is not implemented.
- Unified native queue for supported external media is not implemented.

## Toolchain and last known verification

- Android Gradle Plugin: 9.1.1; Gradle: 9.3.1; Java: 17.
- Android compile SDK: 37 (published package `platforms;android-37.2`); target SDK: 36; minimum SDK: 26; Build Tools: 36.0.0.
- Last successful Player implementation build: [run 42034925797](https://github.com/ysfclc/MOSAIC/actions/runs/42034925797), commit `cc064cb1be7fd6b7d1d6b18a2a09f0d62af0b4a1`. Source verifier, Python contract tests, Android unit tests, lint and debug APK assembly all passed.
- Direct edits to app/core/tools/docs/build configuration now trigger a separate build/test/lint/APK path. Manual workflow dispatch offers `direct` or `import`; the original ZIP importer remains intact.
- Latest Player debug APK: 22,114,601 bytes; SHA-256 `74a55e78f58e9cded6f43c261676756b9014bd7e91c970be65e9d4b7703be152`.
- No successful physical-device test is recorded. Do not claim screen-off, lock-screen, headset or Bluetooth acceptance on the Redmi.

## Product delivery sequence

1. Preserve and verify the local Media3 foundation; complete pending Redmi device acceptance when the device is available.
2. Build source adapters and background playback paths: RSS, radio, direct media, browser handoff and source-owned external sessions. Investigate YouTube, Spotify, Facebook and Instagram individually.
3. Build Ad Shield: source-aware browser rules plus user-enabled device-wide VPN/DNS/host filtering, diagnostics, allowlists, safe recovery and compatibility tests.
4. Improve provider integrations and unified playback UX without pretending external sessions are native MOSAIC queue items.
5. Continue durable library, video and intelligence work only after the playback and Ad Shield foundations are sound.

## Immediate next engineering task

**P3-008 — implement the device-wide DNS-only Ad Shield.** ADR-016 locks the architecture: an explicitly enabled Android VpnService routes only DNS queries to a local virtual resolver, filters known ad/tracker domains, and forwards allowed queries through a protected upstream socket. Do not tunnel ordinary app traffic. Add a persistent foreground notification, VPN-conflict warning, Private DNS coverage warning, blocked-domain counter and safe stop/revoke behavior. Unit-test the DNS packet codec before wiring the TUN service.

P3-007's Player controls are built and CI-verified; real-app/Redmi acceptance remains a separate pending device check.

## Required handoff discipline

- Update docs/TASKS.md in the same change as implementation status.
- Update PROJECT_STATE.md with exact commit/run IDs, test results and remaining limitations.
- Never mark a source or ad-filter capability DONE without real evidence.
- Never claim a build or device test that was not run.
- Keep work in small, independently verifiable steps; no unrelated redesign or ZIP handoff unless requested.
