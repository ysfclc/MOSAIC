# MOSAIC — Vercel sandbox build record

Date: 2026-11-04  
Repository source: ysfclc/MOSAIC / incoming/MOSAIC-Phase-1-Repository.zip  
Source ZIP blob SHA: e45cd6b123cca7b8376364518907fbbd3d43a0d8

## Changes

1. Fixed the unresolved Kotlin revision reference. PlayerScreen passes its revision parameter into PlayerArtwork, and PlayerArtwork now declares that parameter. The existing SIGNAL counter is retained.
2. Corrected the CI SDK package from nonexistent platforms;android-37 to published platforms;android-37.2. App compileSdk = 37 was not changed. Updated README local SDK setup instructions.
3. Added two Python contract regression tests and marked P1-015/P1-016 done in docs/TASKS.md.

## Environment

- Vercel sandbox region: fra1; 2 vCPU, 4 GB RAM.
- Java: OpenJDK 17.0.20.1.
- Gradle: 9.3.1 (distribution checksum verified against the official .sha256 file).
- Android SDK: platforms;android-37.2, Build Tools 36.0.0, platform-tools.

## Verification

- python3 tools/verify_source.py — PASS.
- python3 -m unittest discover -s tools/tests -v — 8 tests PASS.
- :core:media:testDebugUnitTest — PASS.
- :app:testDebugUnitTest — PASS (NO-SOURCE in current app module).
- :app:lintDebug — PASS with warnings listed below.
- :app:assembleDebug — PASS.
- Clean offline/cached verification: BUILD SUCCESSFUL in 51s; 170 actionable tasks (14 executed, 156 up-to-date).

## APK

- Path: app/build/outputs/apk/debug/app-debug.apk.
- Size: 22 MB.
- SHA-256: a79b047f766b754b3eb097ec1c31d9fd4bcd0dd3c3ae603a3d4b487174d48078.
- Package: com.ysfclc.mosaic, version 0.1.0-phase1, min SDK 26, target SDK 36, compile SDK 37.

## Lint warnings retained

- Target SDK 36 is below the latest available platform target.
- Dependency updates suggested for core-ktx, activity-compose, lifecycle-runtime-ktx and Compose BOM.
- Exported media playback service lacks an explicit permission attribute.
- Legacy fullBackupContent attribute is deprecated on Android 12+.

These warnings are recorded, not suppressed or auto-fixed as part of this narrow compiler/CI repair.

## Not completed

- GitHub Actions workflow/import execution: pending; no commit was pushed.
- Gradle wrapper bootstrap/commit: pending canonical GitHub import workflow.
- Physical device acceptance on Redmi Note 12 Pro 5G: pending.
