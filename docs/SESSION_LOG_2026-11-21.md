# MOSAIC session checkpoint — 2026-11-21

## Fresh-start baseline

- Repository: `ysfclc/MOSAIC`
- Working branch: `work/media-library-change-observer-2026-11-21`
- Base revision: `73c825e46ea306f826f32f47131859fffc82fff` on `work/room-schema-v1-contract-2026-11-21`.
- Base CI: direct build #305 passed Android unit tests, lint, APK build, APK verification, report upload and private prerelease publication.
- Product north star: `docs/MOSAIC_PRODUCT_VISION.md` (locked; unchanged).
- Architecture baseline: `docs/ARCHITECTURE.md`, ADR-017 and `docs/STATE.md`.

## Product invariants

MOSAIC remains a local-first Android personal media OS. Preserve long-content reliability, cross-source playback continuity and broad ad/tracker protection. The unified Library is the user's media memory and must preserve source provenance and original files. Never conflate MOSAIC-owned playback with Browser Player or source-owned external sessions. No mandatory account/cloud dependency, media duplication, DRM circumvention, credential scraping, TLS interception or claims unsupported by device evidence.

## Existing library baseline

- Room v1 and source-neutral `LibraryRepository` are implemented.
- Repository sync uses stable source-scoped IDs, safe locator filtering, 400-record batches, transactional upserts and preservation of bookmark/resume/last-played state.
- Complete adapter snapshots mark unseen records missing; partial/permission-denied snapshots do not. A later complete snapshot restores matching identities.
- Home/Library read Room-backed records.
- Legacy playback history, queue and timer preferences remain the source of truth pending deliberate cutover.
- Generated Room v1 JSON remains outstanding; do not change Room entities or database version in this increment.

## This increment — adapter change observation

1. Add an optional change-observation stream to the generic source adapter contract. Existing adapters remain source-compatible through a no-op default.
2. Implement a lifecycle-safe Android MediaStore ContentObserver for the device audio collection.
3. Let LibraryRepository expose a single observe-and-sync stream that performs an initial sync and then coalesces change notifications into source snapshots.
4. Ensure incomplete/permission-denied snapshots never trigger missing-item reconciliation.
5. Add deterministic contract/unit coverage and update task/state documentation.
6. Do not add background polling, foreground services, network access or new database schema.

## Acceptance criteria

- Observer registration is scoped to Flow collection and always unregistered on cancellation.
- Notifications are conflated so bursts do not enqueue unbounded full scans.
- Sync remains serialized by Flow collection and reuses existing transaction/safety behavior.
- Generic adapters need not implement observation.
- CI passes Python/source verification, Android unit tests, lint and signed APK build.
- Physical-device acceptance remains separate and is not claimed.

## Rollback

Revert the adapter observation, MediaStore monitor, repository stream, tests and documentation changes. No database migration or user-data mutation is introduced by this increment.

## Prior increment carry-forward

- Room v1 generated schema JSON is still not checked into `core/persistence/schemas`; this remains an explicit P2-001 blocker.
- CI run #305 passed end-to-end; the report artifact contains the generated Room schema.
- Future migrations, Room-backed history/queue/timer cutover and Redmi device/process-death acceptance remain open.
