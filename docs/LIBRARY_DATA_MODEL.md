# Unified Library data model

Status: **design approved for implementation; Room schema not yet created.**

This model implements the personal media memory described in `MOSAIC_PRODUCT_VISION.md`. It is source-neutral: it must not hard-code YouTube, Spotify, podcasts, local files or any other individual use case into the canonical Library.

## Invariants

1. **Original media is never moved, renamed or copied by indexing.** The database stores metadata and references only.
2. **Identity is source-scoped.** A provider-local ID is not globally unique. Canonical identity is `MediaIdentity.stableId(sourceId, sourceItemId)`, currently encoded as `mosaic-media-v1:<base64url(sourceId)>:<base64url(sourceItemId)>`.
3. **Locations are separate from identity.** A media item may have multiple locations or a refreshed location without becoming a different item.
4. **No credentials in durable locators.** Do not persist user-info, signed query strings, fragments or ephemeral authorization headers. Store a stable source item ID and resolve a fresh authorized locator when needed.
5. **Source provenance is mandatory.** Keep the source ID and source-local item ID; never infer source from a title or host name.
6. **Local-first.** No account, backend or network connection is required to read the Library, history, collections or queue.
7. **Playback ownership stays explicit.** Only media that MOSAIC can actually play enters the native Media3 queue. External MediaSession items remain delegated and are not silently imported as native playback.
8. **Deletion is conservative.** A missing or revoked location does not delete the media identity, history or collections. Prefer marking a location unavailable or requiring reauthorization.

## Proposed Room tables

### `media_items`

Canonical item metadata and user state.

| Column | Purpose |
|---|---|
| `mediaId` (PK) | Source-scoped stable ID from `MediaIdentity` |
| `sourceId` | Adapter/source namespace |
| `sourceItemId` | Original ID inside that source |
| `kind` | Stable string enum: AUDIO, VIDEO, PODCAST_EPISODE, RADIO_STREAM, WEB_CAPTURE, AUDIOBOOK, etc. |
| `title`, `creator`, `collectionTitle` | Display metadata |
| `durationMs`, `mimeType`, `artworkUri` | Optional media metadata |
| `addedAtEpochMs`, `updatedAtEpochMs` | Metadata lifecycle |
| `lastPlayedAtEpochMs`, `resumePositionMs` | Playback continuity |
| `isBookmarked`, `isHidden` | User-owned Library state |
| `availability` | PRESENT, MISSING, REAUTH_REQUIRED, UNKNOWN |

Indexes: unique `(sourceId, sourceItemId)` when sourceItemId is present; indexes for title, kind, last-played time and bookmark state. Store enum values as strings, never ordinals.

### `media_locations`

One or more locators for an item. Foreign key to `media_items` with cascade only when a media item is deliberately hard-deleted.

Fields: `locationId` (PK), `mediaId`, `sourceId`, `uri`, `mimeType`, `role` (PRIMARY, ALTERNATE, ARTWORK), `hasPersistedGrant`, `availability`, `lastVerifiedAtEpochMs`.

Do not store signed URLs or request headers. SAF permission grants belong to the location and must be revalidated when used.

### `media_collections` and `media_collection_items`

User-created collections and ordered membership.

- Collection: `collectionId` (PK), `name`, `description`, `createdAtEpochMs`, `updatedAtEpochMs`, `sortOrder`.
- Membership: composite PK `(collectionId, mediaId)`, `addedAtEpochMs`, `sortOrder`; foreign keys cascade on explicit collection or item deletion.

### `playback_history`

Append-only recent-play events and resume snapshots.

Fields: `eventId` (PK), nullable `mediaId`, title/creator/URI snapshot for safe fallback, `playedAtEpochMs`, `resumePositionMs`. Keep history bounded by a retention policy, not by deleting the canonical media item.

### `playback_queue_state` and `playback_queue_entries`

Persist the MOSAIC-owned queue independently of the library view.

- Queue state: `queueId` (PK; initially `primary`), `currentIndex`, `positionMs`, `updatedAtEpochMs`.
- Entry: composite PK `(queueId, position)`, nullable `mediaId`, safe URI/title/creator/MIME snapshot, `sourceId`.

Queue entries retain enough safe metadata to show the queue if a Library row is missing. Never store credentials or signed URLs.

### `migration_markers`

Idempotency markers for one-time imports from legacy app-private preferences. Marker key is the PK; value records import completion/version and timestamp.

## Module ownership and dependency direction

Create `:core:persistence` as the shared Room boundary. It owns entities, DAOs, database, exported schemas and migration tests; it must not depend on UI, `:core:media` or `:core:library`.

- `:core:media` → `:core:persistence` for native queue persistence.
- `:core:library` → `:core:media` and `:core:persistence` for source adapters and catalog/library operations.
- `:app` → all required modules for UI and one-time legacy-state import.

This avoids a dependency cycle: the playback service must not depend on a library module that already depends on media.

## Migration and import policy

1. First Room schema version creates the tables above and exports the schema JSON to version control.
2. Add explicit, tested Room migrations for every later schema change. **Never use destructive migration fallback.**
3. Import existing `mosaic_playback_history`, `mosaic_playback_queue` and sleep-timer preferences in an idempotent transaction, using `migration_markers`.
4. Preserve the original preferences until the Room transaction commits and a subsequent read-back check succeeds. A failed import must be safely retryable.
5. Preserve legacy IDs as aliases or resolve them through `MediaIdentity`; do not silently create duplicate items because an old ID format differs.
6. Only after verified import should reads/writes move to Room. Keep a compatibility read path for one release if required; do not dual-write indefinitely.
7. Export and backup user metadata separately from original media files.

## Acceptance tests

- Same source-local ID under two different sources produces two different media IDs.
- Same source/item pair produces the same media ID across app restarts.
- A locator refresh does not create a duplicate media item.
- Revoked/missing locations preserve history and collection membership.
- Collection membership ordering and uniqueness survive reopen.
- Queue order, current index and resume position survive database reopen.
- Legacy history/queue import is idempotent, retryable and never clears preferences before commit.
- Every schema migration has a test from the immediately previous schema.
- No test or production path copies, moves or renames original media.
