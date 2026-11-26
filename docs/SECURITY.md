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


## APK signing-key rotation — release blocker

The public repository tracks `tools/mosaic-debug-signing-key.b64`. Its certificate (SHA-256 `700b857678b343aed93f7a7280e7d777df73c3f5f5e5024d94c119a4578079d0`) signed earlier installs, including unauthorized prerelease #347. Treat that identity as compromised. It must never again be the sole signer for a supported MOSAIC build.

A fresh RSA-4096 signing identity has been generated outside Git. Its public certificate fingerprint is SHA-256 `58205766cd01f75e51a13f8d7aeae08b7619de58749dc2fd4a46cb32b39cb647`. The private JKS and its independent passwords are not committed. Configure these exact repository Actions secrets before dispatching `.github/workflows/signing-migration.yml`:

- `MOSAIC_RELEASE_KEYSTORE_BASE64`
- `MOSAIC_RELEASE_KEYSTORE_PASSWORD`
- `MOSAIC_RELEASE_KEY_ALIAS`
- `MOSAIC_RELEASE_KEY_PASSWORD`

The migration candidate workflow is manual-only, restricted to branch `security/signing-lineage-migration`, read-only for repository contents and incapable of creating a release. It verifies both keystore fingerprints, builds an unsigned release variant, creates an APK Signature Scheme v3 signing lineage from the known legacy certificate to the replacement certificate, signs with `--rotation-min-sdk-version 28`, verifies the APK for API 28 and API 34, and uploads a short-lived candidate plus the lineage file and checksums. The replacement key is the active signer on supported Android versions. The app's minimum SDK is raised from 26 to 28 because Android 8.x cannot verify v3 rotation; Android 9+ remains supported.

The legacy key file is retained temporarily only to generate the lineage. After the candidate workflow succeeds, persist the non-secret lineage file in the repository and remove `tools/mosaic-debug-signing-key.b64` from the current tree. A history rewrite is separate cleanup and does not revoke the exposed key.

Required safeguards:
- Never print, upload, or commit private key bytes, passwords, or temporary keystores.
- Decode the replacement keystore only to a runner-temporary file with restrictive permissions.
- Keep the expected replacement certificate fingerprint as a public integrity check.
- Verify APK signatures and the lineage before handing the candidate to the user.
- Install the candidate over the existing package on the Redmi Note 12 Pro 5G (Android 14/API 34), without uninstalling first; verify launch and preservation of local app data.
- Do not publish a release or call the candidate release-ready until the physical install-over-existing-app test and all release gates pass.

References: [Android apksigner and key rotation](https://developer.android.com/tools/apksigner), [APK Signature Scheme v3](https://source.android.com/docs/security/features/apksigning/v3), [APK Signature Scheme v3.1](https://source.android.com/docs/security/features/apksigning/v3-1), [GitHub Actions encrypted repository secrets](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions).
