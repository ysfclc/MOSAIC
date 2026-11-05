# MOSAIC — Media Player & Ad Shield

**Phase 1 · Native foundation and local playback**  
**Repository:** ysfclc/MOSAIC  
**Package:** `com.ysfclc.mosaic`  
**Status:** Integrated source snapshot passed GitHub Actions run 41957296181; physical-device acceptance remains pending. A newer incoming ZIP was not imported (run 41962171501 failed preflight); see PROJECT_STATE.md.

MOSAIC is an Android media player and ad-control hub. Its goal is to play media from as many sources as possible in the background and block ads/trackers across as many apps and surfaces as Android and each source technically allow. YouTube, Spotify, Facebook, Instagram and other external sources are explicit integration targets to investigate and test; the current build does not yet deliver universal support.

## Why MOSAIC exists: background playback and ad blocking

MOSAIC exists to break the fragmentation of media across apps and websites: play content from other sources in the background, keep control of playback in one coherent place, and block ads wherever technically possible. The intended source scope includes YouTube, Spotify, Facebook, Instagram, browser media, podcasts, radio, direct streams and local files. The target is broad; actual support must be demonstrated source by source on Android devices. A browser-only ad filter or external play/pause remote is not completion of this vision. See [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) and [ADR-014](docs/DECISIONS.md).

## Phase 1 shipped in this source snapshot

- Native Kotlin + Jetpack Compose application shell with an animated dark visual system.
- Home, Library and Player destinations with responsive transitions.
- Android MediaStore audio indexing, read-only and without copying or moving original files.
- Storage Access Framework file picker for directly opening a selected audio file without broad library permission.
- Media3 ExoPlayer hosted in a `MediaSessionService` for background playback.
- System media session, lock-screen/notification and Bluetooth control integration through Media3.
- Queue playback for indexed local audio.
- Architecture, product decisions, roadmap, tasks, verification and GitHub ZIP-import workflow notes.

## Build environment

| Component | Pinned baseline |
|---|---|
| Android Gradle Plugin | 9.1.1 |
| Gradle distribution | 9.3.1 (installed by CI) |
| Java | 17 |
| Android compile SDK | 37 |
| Android target SDK | 36 |
| Minimum SDK | 26 |
| Compose BOM | 2026.08.00 |
| Media3 | 1.11.1 |

AGP 9 provides built-in Kotlin support. The project intentionally does **not** apply the legacy `org.jetbrains.kotlin.android` plugin. The Compose compiler plugin is configured separately.

### Build in Android Studio

The initial source ZIP intentionally omits generated wrapper binaries. The Gradle 9.3.1 wrapper and official distribution SHA-256 are already present in this integrated repository snapshot. Then open the integrated repository in an Android Studio release compatible with AGP 9.1.1, install Android SDK package `platforms;android-37.2` (the published API 37 platform) and Build Tools 36.0.0, sync and run the `app` configuration.

### Build in CI

Direct pushes that change app/core code, tools, documentation or build configuration automatically run source verification, Python contract tests, Android unit tests, lint and debug APK assembly. The workflow also offers a manual `direct` build mode.

The separate ZIP-import mode remains available for `incoming/mosaic.zip` (and the legacy named archive path). It validates and safely extracts the uploaded source, preserves active workflows/history, then verifies and builds before committing an import note.

The Android build uses Java 17, Android SDK platform `platforms;android-37.2`, Build Tools 36.0.0 and Gradle 9.3.1. The expected debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## First-run usage

1. Open **Library**.
2. Choose **Scan device** and grant audio access, or choose **Open one media file** to play a file without granting broad library access.
3. Tap a track to start a queue.
4. Open **Player**, lock the screen or switch apps. Media3 owns playback in a service independent of the Compose screen.
5. Use the Android media notification, lock-screen controls or supported Bluetooth controls.

Phase 1 deliberately exposes audio playback only. A dedicated video surface, picture-in-picture, video library and video-specific browsing are planned for a later phase; the source model already includes a video media kind.

## Architecture is locked

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/DECISIONS.md`](docs/DECISIONS.md), and [`docs/PHASES.md`](docs/PHASES.md). Do not create a second playback engine, a provider-specific canonical library, or a cloud account requirement. Every new source must implement the shared adapter/capability contract and leave canonical library/playback state in the Media Core.

## Project continuity

- [`PROJECT_STATE.md`](PROJECT_STATE.md) is the session handoff file.
- [`docs/TASKS.md`](docs/TASKS.md) is the source-of-truth task board.
- [`docs/PHASES.md`](docs/PHASES.md) defines phase gates and sequencing.
- [`notes/README.md`](notes/README.md) indexes GitHub ZIP import notes created by the workflow.
- [`docs/CHANGELOG.md`](docs/CHANGELOG.md) records meaningful project changes.

Every successful ZIP import creates an immutable upload record under `notes/imports/<upload-commit-sha>.md`.

## Provider and platform boundaries

YouTube, Spotify, Facebook, Instagram and other sources are explicit targets for source-by-source background playback and ad-filtering investigation. Do not mark a capability supported until tested, but do not blanket-exempt a provider or remove it from the roadmap. Distinguish MOSAIC-owned playback from external-app sessions and controls. Respect platform and content-access boundaries.

## Ad Shield and Browser status

The Browser is an early supporting WebView with a conservative third-party host filter, not a full browser product or the whole ad-blocking solution.

The first cross-app Ad Shield slice is implemented and CI-verified in [run 42051683244](https://github.com/ysfclc/MOSAIC/actions/runs/42051683244). It uses an explicitly user-enabled Android `VpnService` with split-route IPv4 UDP DNS interception, curated ad/tracker rules, an editable hostname allowlist, visible status/errors/blocked-domain count, a persistent notification with Stop, VPN-conflict handling and Private DNS-preserving resolution on Android 10+. Android 9 with Private DNS enabled fails closed. Ordinary app traffic is not routed through a full VPN tunnel.

This is not universal ad removal. The current packet codec handles IPv4 UDP DNS; apps using DoH/custom resolvers can bypass it, and DNS filtering cannot reliably remove first-party same-domain or in-stream ads. IPv6 DNS coverage, real named-app compatibility, false-positive measurement and Redmi device acceptance remain open. The Ad Shield screen now includes a quick DNS self-test for normal resolution, blocked-host interception and allowlist behavior. Follow [the Redmi device acceptance checklist](docs/AD_SHIELD_DEVICE_ACCEPTANCE.md); CI success does not replace physical-device testing. Browser filtering and device-wide DNS filtering have separate scopes.

Optional notification access enables discovery/control of supported external Android media sessions; it does not transfer audio into MOSAIC or force a provider to allow background playback. Native background playback from named third-party sources remains under development. See PRODUCT_SPEC.md, ARCHITECTURE.md, PROVIDER_CAPABILITY_MATRIX.md and DECISIONS.md.

## Installing updates

MOSAIC CI debug APKs now use one persistent development signing identity and increasing version codes. The stable signing pipeline passed in [run 42055276990](https://github.com/ysfclc/MOSAIC/actions/runs/42055276990). The first stable-key build may require one final migration uninstall if an earlier CI build is installed; after that, install future APKs directly over the existing app. Normal updates should preserve app data. This development signing identity is not a production release key.
