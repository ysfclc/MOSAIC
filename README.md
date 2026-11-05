# MOSAIC — Personal Media OS

**Phase 1 · Native foundation and local playback**  
**Repository:** ysfclc/MOSAIC  
**Package:** `com.ysfclc.mosaic`  
**Status:** Integrated source snapshot passed GitHub Actions run 41957296181; physical-device acceptance remains pending. A newer incoming ZIP was not imported (run 41962171501 failed preflight); see PROJECT_STATE.md.

MOSAIC is a local-first Android media environment. It brings playback, browsing, a unified library, source adapters, optional intelligence and automation into one coherent native application. It is not a streaming service and it does not bypass provider advertising, DRM, paywalls or access controls.

## Why MOSAIC exists: capture, keep and move content

MOSAIC began with a simple cross-app problem: when you find something useful in another app, you should be able to copy its URL/text or use Android Share to bring it into MOSAIC, keep it for later, and then copy, open or share it back out. This two-way copy/paste-and-share workflow is a core product requirement, not just a browser shortcut. The broader product unifies local media and legitimate external sources around that capture-and-continue flow. Paste is explicit and user-triggered; MOSAIC is not a background clipboard-history manager (that is a separate VaultOS project). See docs/PRODUCT_SPEC.md.

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

The separate workflow installs Java 17, Android SDK packages and Gradle 9.3.1, bootstraps the verified wrapper, then runs:

```bash
./gradlew --no-daemon :core:media:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The expected debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

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

## Provider boundaries

MOSAIC can provide an uninterrupted native experience for local files and sources whose technical and legal terms permit it. Third-party services control their own ads, subscriptions, DRM and playback rules. YouTube/Spotify integration must use supported interfaces and must not promise ad removal or unauthorized background extraction.

## Browser preview and provider limitations

The Browser destination is an early implementation. Its request filter blocks a conservative set of third-party ad/tracker hosts on ordinary pages and displays a blocked-request counter. It does not remove all ads, first-party ads, in-stream ads or provider-served advertisements. YouTube and Spotify pages/assets are deliberately exempt.

Shared text/URLs can already enter through Android's share sheet and open in Browser; Browser links can be shared out. Full file inbox and file handoff remain planned. Optional notification access enables authorized external media-session control. Official YouTube playback must retain its standard player and ads and cannot be backgrounded through the YouTube API. Spotify can be controlled through its official Android App Remote integration when available and authorized; Spotify remains responsible for its own audio stream and background playback. See PRODUCT_SPEC.md, DECISIONS.md and PHASES.md.
