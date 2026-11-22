# MOSAIC session log — 2026-11-22

## Incremental MediaStore item refresh

- Continued `work/media-store-delta-indexing-2026-11-22` from the Room v1 schema-verification branch.
- Added provider-neutral `MediaSourceChange.ItemChanged` and `Invalidate` events; legacy `Flow<Unit>` adapters remain compatible and default to full invalidation.
- The MediaStore observer now classifies only exact supported external-audio item URIs as item changes. Null, collection, malformed, unsupported-volume, query/fragment and other ambiguous notifications invalidate the full source.
- Added targeted `MediaStoreCatalog.findAudioById()` using `_ID = ?`, wired the local adapter's item lookup, and changed local item resolution from full catalog scans to targeted queries.
- `LibraryRepository.observeAndSync()` now refreshes a single record for a successful item lookup. Missing or failed targeted lookup falls back to a full snapshot. Existing complete-snapshot reconciliation remains the only deletion path, so incomplete/permission-failed snapshots cannot mark items missing.
- Added regression coverage for exact URI classification, item refresh without a second full browse, bookmark/resume/last-played preservation, and incomplete-snapshot fallback without hiding an existing item.
- Updated `docs/MEDIASTORE_INCREMENTAL_INDEXING.md`, P2-003 in `docs/TASKS.md`, and the library status/in-flight section in `docs/STATE.md`.
- CI verification is pending after the source commit. A local clone/build could not run because this environment could not resolve `github.com`; the existing latest branch CI run #330 is successful but predates this change.
- No physical-device testing was performed. Redmi checks for URI semantics, observer lifecycle, permission revocation/restoration, process-death and screen-off behavior remain outstanding.
- No direct deletion signal was added: MediaStore `ContentObserver` notifications do not prove deletion. Targeted lookup absence alone never marks a record missing.

## Next

- Review the new direct-build CI results and fix any failure before calling this slice verified.
- Continue delta handling only where item identity is reliable; retain full invalidation for collection/bulk changes.
- Keep the public development-signing-key migration tracked separately; do not rotate the key or break installed-app update compatibility without a controlled replacement and verification plan.
