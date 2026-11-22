# MediaStore incremental indexing design

**Status:** Design checkpoint for the next implementation increment  
**Branch:** `work/media-store-delta-indexing-2026-11-22`

## Current behavior

- `MediaSourceAdapter.observeChanges()` emits `Flow<Unit>`; it communicates invalidation, not item identity.
- `MediaStoreChangeMonitor` observes the external audio MediaStore URI recursively and conflates notifications.
- `LibraryRepository.observeAndSync()` treats every notification as a reason to run a full browse/snapshot.
- Full snapshots are intentionally guarded: only a blank-query, complete provider snapshot can mark unseen records missing. Permission errors and incomplete results do not reconcile deletions.
- Repository updates preserve user state (bookmark, playback position and last-played timestamp) by stable source-local identity.

## Why not implement deltas from the current signal

The observer callback can include a URI, but not all notifications are guaranteed to be item-specific; collection-level changes, null URIs, bulk changes, and deletion notifications require different handling. Treating every URI as a precise item mutation or treating a failed item query as a deletion can incorrectly hide media or erase locations.

## Proposed contract (not yet implemented)

1. Add a provider-neutral change type with explicit `ITEM_UPSERT`, `ITEM_DELETE`, and `INVALIDATE` semantics, including an optional source-item ID/locator.
2. MediaStore observer maps a notification to an item change only when its URI is a supported item URI with a parseable ID; otherwise it emits `INVALIDATE`.
3. Item upsert performs a targeted query by ID. A successful query with a record updates only that record; an empty or failed query is not automatically interpreted as deletion.
4. Deletion may mark one item missing only when the provider has positive deletion evidence. Permission exceptions, null cursors, and ambiguous URIs must never delete.
5. `INVALIDATE` continues to trigger a full snapshot, with the existing complete-snapshot reconciliation guard.
6. All database mutations remain serialized and transactional; update paths preserve bookmark/playback fields.
7. Add tests for item upsert, explicit delete, null/collection URI fallback, provider error, permission failure, notification conflation, and stable user-state preservation before switching production behavior.

## Acceptance criteria

- A targeted update does not issue a full-library query.
- An ambiguous notification falls back to a full snapshot.
- An incomplete snapshot never marks unseen media missing.
- A failed targeted query never implies deletion.
- A deletion changes only the identified record and its locations.
- User-owned state survives all upsert paths.
- Existing adapters remain source-compatible during migration (default invalidation behavior).

## Scope guard

This is a design checkpoint, not a claimed implementation. Physical device testing remains required for MediaStore URI behavior, observer lifecycle, permission revocation/restoration, and screen-off scenarios.
