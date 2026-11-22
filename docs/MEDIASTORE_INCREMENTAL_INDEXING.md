# MediaStore incremental indexing

**Status:** First item-level refresh slice implemented and CI-verified in [run #332](https://github.com/ysfclc/MOSAIC/actions/runs/45024226563) on `work/media-store-delta-indexing-2026-11-22`

## Current behavior

- `MediaSourceAdapter.observeChangeEvents()` adds provider-neutral `ItemChanged` and `Invalidate` events. Existing `Flow<Unit>` observers remain compatible and are mapped to full invalidations by default.
- `MediaStoreChangeMonitor` emits `ItemChanged` only for exact, supported external-audio item URIs with a positive numeric ID. Collection, null, malformed, query/fragment-bearing and unsupported-volume URIs remain invalidations.
- `LocalMediaSourceAdapter.findItem()` and `MediaStoreCatalog.findAudioById()` re-read a single row with `_ID = ?`; the targeted lookup does not enumerate the full library.
- `LibraryRepository.observeAndSync()` performs the initial full sync, then targeted item upserts for item-change signals. Targeted upserts preserve bookmark, resume position and last-played state through the existing Room `@Upsert` path.
- If the targeted lookup is missing or unavailable, the repository falls back to a full snapshot. Only a blank-query, complete snapshot reconciles missing items. An incomplete or permission-failed snapshot never marks items missing.
- `LocalMediaSourceAdapter.resolve()` now resolves a local item by targeted ID lookup rather than scanning the entire catalog.

## Deletion safety

Android's `ContentObserver` URI indicates that content changed; it does not reliably identify whether the operation was a deletion. The MediaStore observer therefore emits no direct delete event. A missing targeted lookup triggers the guarded full snapshot, and only a complete full snapshot can mark unseen items missing or clear their locations. No permission failure or empty targeted query alone is deletion evidence.

## Verification added

- Exact external and external-primary audio item URIs classify to targeted changes.
- Collection, null, malformed ID, unsupported-volume, query/fragment and non-content URIs classify to invalidation.
- Targeted repository refresh updates metadata without another full browse and preserves user bookmark/playback state.
- A failed targeted lookup falls back to an incomplete snapshot without hiding the existing item or clearing its location.
- Existing complete/partial snapshot reconciliation and adapter invalidation tests remain in place.

## Acceptance criteria

- A recognized item update performs a targeted `_ID = ?` lookup and does not issue a full-library query when the row is found.
- Ambiguous notifications fall back to full snapshots.
- Missing or failed targeted lookups do not themselves imply deletion.
- Incomplete/permission-denied snapshots never mark unseen media missing.
- Only a complete full snapshot can reconcile missing items.
- User-owned bookmark and playback state survive targeted upserts.
- Existing adapters remain source-compatible through default invalidation behavior.

## CI verification — run #332

- Source verification and all 34 Python contract/schema tests passed.
- `:core:persistence:testDebugUnitTest`, `:core:media:testDebugUnitTest`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` passed.
- Room schema check, persistent development-signer verification, APK checksum, artifact upload and prerelease publication passed.
- The first CI attempt (#331) stopped at a stale Python assertion expecting the legacy observer call; that assertion was updated to the new item-event contract before run #332.
- CI is not physical-device acceptance.

## Next

- Continue provider delta work only where an adapter can supply reliable item-level evidence; bulk changes stay invalidations.
- Add explicit deletion events only for providers with positive deletion evidence. MediaStore's current observer does not provide that evidence.
- Physical Redmi acceptance remains required for URI notification semantics, observer lifecycle, permission revocation/restoration, process-death and screen-off behavior.

The library remains read-only: original media files are never copied, moved or renamed.
