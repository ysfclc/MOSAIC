# Testing and acceptance strategy

## Automated checks

The workflow must run these before committing an integration:

1. ZIP integrity and safe-extraction checks (CRC, path safety, no symlinks/special files/duplicates/secrets).
2. `python3 tools/verify_source.py`.
3. Python tests in `tools/tests`.
4. `:core:media:testDebugUnitTest`.
5. `:app:testDebugUnitTest`.
6. `:app:lintDebug`.
7. `:app:assembleDebug`.

The workflow only commits the extracted source and import note after all required checks succeed.

## Device acceptance — Redmi Note 12 Pro 5G

- Install debug APK; cold launch; rotate/reopen if supported by app UX.
- Scan local audio; verify names/artists and that no files are copied/moved/renamed.
- Revoke permission in Android settings; verify the app recovers without crash; use Open Document as the narrower alternative.
- Play a local track; navigate Home/Library/Player; lock screen; wait at least 10 minutes; verify playback continues.
- Verify media notification title/artist/play-pause/next/previous.
- Connect wired/Bluetooth headset; test media button behavior.
- Unplug or disconnect current audio route; verify `setHandleAudioBecomingNoisy(true)` pauses appropriately.
- Receive a phone call or play another audio app; verify Android audio focus behavior and recovery.
- Test file-picker playback for common audio formats; unsupported codecs should fail visibly, not crash. Video playback is not a Phase 1 acceptance item.
- Test force-stop separately: Android intentionally terminates foreground services on force-stop; do not treat force-stop survival as a requirement.
- Check logs for fatal exceptions and repeated service reconnection.

## Performance and UX

- No animation loop should allocate per frame.
- Library scan runs off the main thread.
- UI remains usable during indexing.
- Empty, permission-denied, loading, unsupported and playback-error states are visible.
- Verify text scaling and touch targets; reduced-motion preference remains a tracked follow-up.

## Verification language

- “Gradle build passed” means the GitHub Actions build completed successfully.
- “Device tested” means a physical device test has been performed and recorded.
- Never treat an APK artifact as proof of lock-screen/background reliability.
