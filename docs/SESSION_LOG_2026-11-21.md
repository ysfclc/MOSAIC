# MOSAIC session checkpoint — 2026-11-21

## Fresh-start baseline

- Repository: `ysfclc/MOSAIC`
- Working branch: `work/room-schema-v1-contract-2026-11-21`
- Base revision: `d88c9d4b1fe23a694860a36d016598e51709d1b2`
- Base CI: [direct build #300](https://github.com/ysfclc/MOSAIC/actions/runs/43732954642), successful on the exact base revision.
- Product north star: `docs/MOSAIC_PRODUCT_VISION.md` (locked; unchanged).
- Architecture baseline: `docs/ARCHITECTURE.md`, ADR-017 and `docs/STATE.md`.

## Product invariants to preserve

MOSAIC remains a local-first Android personal media OS. Cross-source playback continuity and broad ad/tracker protection remain its two primary pillars. The unified Library is the user's media memory, preserving source provenance and original files. Never conflate MOSAIC-owned playback with Browser Player or source-owned external sessions. No mandatory account/cloud dependency, media duplication, DRM circumvention, credential scraping, TLS interception, or claims unsupported by device evidence.

## Current persistence baseline

- `:core:persistence` exists with Room 2.8.4, KSP schema export enabled, and `MosaicDatabase` version 1.
- v1 includes media items/locations, collections/memberships, playback history, queue state/entries, timer state and migration markers.
- A source-neutral `LibraryRepository` writes source records into Room and preserves bookmarks/resume state.
- Home and Library read Room-backed records.
- Legacy playback preferences remain the source of truth for history, queue and sleep timer; startup import/mirror is atomic, read-back checked and non-destructive.
- Generated Room v1 schema was present in successful CI build #300's report artifact but is not checked into `core/persistence/schemas`.
- No migration from Room v1 to v2 exists yet. Do not change database entities/version in this increment.

## This increment — Room v1 schema contract

1. Recover the exact Room-generated v1 schema from CI run #300, whose artifact belongs to base SHA `d88c9d4b1fe23a694860a36d016598e51709d1b2`.
2. Check it into Room's configured schema directory without hand-editing generated JSON.
3. Add a small deterministic schema-contract test that validates database version, entity/table set, primary keys, foreign-key delete behavior and critical unique indexes against the checked-in generated schema.
4. Ensure source verification/CI checks fail if the v1 schema is missing or malformed.
5. Update task/state documentation with the exact evidence and remaining gates.

## Acceptance criteria

- The committed schema is byte-for-byte the generated Room v1 JSON from build #300.
- Schema version and identity hash are retained exactly.
- Contract tests pass and catch missing schema, unexpected table changes, destructive FK changes, or removal of source identity uniqueness.
- No production entity, DAO, importer, repository, playback, UI or Ad Shield behavior changes.
- No destructive Room migration fallback is introduced.
- CI must verify Python checks, Android unit tests, lint and APK build on this branch before the increment is called verified.
- Physical-device acceptance remains separate and is not claimed.

## Verification baseline and limitations

- CI #300 passed 58 Android unit tests, source verification, Python contract tests, lint, APK assembly, checksum and signing checks.
- Its report artifact includes `core/persistence/schemas/com.ysfclc.mosaic.core.persistence.MosaicDatabase/1.json`.
- The target Redmi Note 12 Pro 5G is not connected to this session. No device acceptance is implied.
- Existing public-repository development signing-key exposure is a separate security remediation; do not mix key rotation into this persistence increment.

## Rollback

This is documentation/schema-contract-only. Revert the increment commit(s) to remove the schema artifact, contract test and documentation updates. No user database is modified and no migration is required.


## Work completed in this increment

- Added `docs/ROOM_SCHEMA_CONTRACT.md` documenting the Room v1 entity set, referential-integrity rules, migration policy and generated identity hash.
- Added `tools/tests/test_room_schema_contract.py` with six deterministic source-contract tests covering database version/schema export, registered entities, source-scoped uniqueness, cascade behavior, snapshot retention and queue/timer entities.
- Updated the P2-001 task note to record the new coverage without claiming the generated JSON is committed.
- CI run #302 passed source verification and Python tooling, including the new tests. Android unit tests, lint and APK build were still in progress at checkpoint.
- **Outstanding:** the generated Room JSON from run #300 has not yet been committed under `core/persistence/schemas`; therefore this increment is not complete and must not be described as a finished schema check-in. No production Kotlin behavior or database version was changed.
