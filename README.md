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

The current Browser is an early supporting WebView with a conservative third-party host filter. It is not a full browser product and not the whole ad-blocking solution. The current policy contains blanket provider exemptions that must be replaced with tested source-aware rules. Device-wide Android VPN/DNS filtering is not yet implemented. DNS/host filtering cannot reliably remove first-party, same-domain or in-stream ads; MOSAIC must show measured coverage and known gaps rather than promise universal removal.

Optional notification access currently enables discovery/control of supported external Android media sessions; this does not transfer audio into MOSAIC. Native background playback from the named third-party sources and cross-app Ad Shield remain unimplemented. See PRODUCT_SPEC.md, ARCHITECTURE.md, PROVIDER_CAPABILITY_MATRIX.md and DECISIONS.md.
