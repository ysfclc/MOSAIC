# Security and privacy baseline

- Local media is read through Android APIs. The library index is metadata; it is not a hidden media-copy operation.
- Broad audio permission is requested only when the user asks to scan the device. Open Document is offered as an alternative.
- URI grants are persisted only for files the user explicitly selects and only with read permission.
- No telemetry, analytics SDK, remote AI API, sign-in provider, or app-owned advertising SDK is included in Phase 1.
- Future browser content, feed metadata, transcripts and model outputs are untrusted input.
- API tokens, cookies, user profiles, private files and credentials must never be committed in ZIP snapshots.
- ZIP import workflow validates archive paths and file types before extraction and excludes Git history, signing materials, local config and secret-like content.
- Repository workflow uses `contents: write` only to integrate validated source and notes. GitHub Actions permissions must be set to read/write in repository settings.
- A workflow upload is public-repository content if the repository is public. Never upload confidential media, account exports or signing keys.


## Development APK signing migration — release blocker

The public repository currently tracks `tools/mosaic-debug-signing-key.b64`. Its certificate is the stable development signer used by earlier APKs, including the unauthorized build #347. Treat that signing identity as compromised; do not use it as the sole signer for future releases and do not silently replace it with an unrelated key.

The app currently has `minSdk 26`, while the intended test device is Android 14 (API 34). Android APK Signature Scheme v3/v3.1 supports proof-of-rotation through an `apksigner` lineage. The proposed migration is to create a fresh private key outside Git, store it in GitHub Actions Secrets, generate and review a signing lineage, and verify an update install on the API 34 target before unlocking APK publication. Android's documentation warns that rotation should not be relied on for Android 12/API 31 and earlier; if support for existing installs on those versions is required, the compatibility and reinstall trade-off must be explicitly decided before signing changes.

Required safeguards:
- Never print, upload, or commit private key bytes, passwords, or temporary keystores.
- Use GitHub Actions Secrets for the replacement keystore and its password/alias values; decode only to a runner-temporary file with restrictive permissions.
- Keep the expected new certificate fingerprint as a public integrity check.
- Verify the signed APK with `apksigner verify --print-certs`, compare the expected fingerprint, and verify the lineage.
- Test an in-place update on the Redmi Note 12 Pro 5G (Android 14/API 34), preserving app data. Do not publish an APK until that test and the release gates pass.
- Remove the tracked key from the current tree only as part of the coordinated migration. A history rewrite is separate cleanup and does not revoke a leaked key.

References: [Android apksigner and key rotation](https://developer.android.com/tools/apksigner), [APK Signature Scheme v3](https://source.android.com/docs/security/features/apksigning/v3), [APK Signature Scheme v3.1](https://source.android.com/docs/security/features/apksigning/v3-1), [GitHub Actions encrypted repository secrets](https://docs.github.com/en/rest/actions/secrets).
