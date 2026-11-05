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
