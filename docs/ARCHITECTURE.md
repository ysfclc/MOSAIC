# MOSAIC architecture baseline

**Status: LOCKED — baseline 1.0**  
**Date:** 2026-10-30

## 1. Architectural objective

MOSAIC is a local-first, native Android media environment with one canonical media model and one app-owned playback state. Sources are replaceable adapters. The browser, local library, feeds, streams and provider integrations must not each invent their own player or canonical data model.

## 2. Layer map

```text
┌──────────────────────────────────────────────────────────────┐
│                         Android UI                            │
│  Home · Library · Player · Browser (later) · Capture (later)  │
└──────────────────────────────┬───────────────────────────────┘
                               │ UI intents / observed state
┌──────────────────────────────▼───────────────────────────────┐
│                           MEDIA CORE                          │
│ Media identity · source registry · capabilities · queue      │
│ playback state · provenance · permission boundaries          │
└─────────────┬───────────────────┬───────────────────┬────────┘
              │                   │                   │
       ┌──────▼──────┐     ┌──────▼──────┐     ┌──────▼──────┐
       │ PLAYBACK    │     │ SOURCES     │     │ LIBRARY     │
       │ Media3      │     │ adapters    │     │ Room later  │
       │ service     │     │ local/RSS/  │     │ indexes and │
       │ controller  │     │ web/API     │     │ collections │
       └─────────────┘     └─────────────┘     └─────────────┘
              │                   │                   │
       ┌──────▼───────────────────▼───────────────────▼────────┐
       │             LOCAL DATA / FILE OWNERSHIP                │
       │ Original media · URI grants · metadata · cache · index │
       └───────────────────────────────────────────────────────┘

Optional upper layers, later phases:
  Browser adapter → capture inbox → processing queue → local AI
  Backup/export → user-selected external destination
  Optional remote sync → explicit opt-in, never required for playback
```

## 3. Gradle module boundaries

| Module | Owns | Must not own |
|---|---|---|
| `:app` | Activity, Compose navigation, screen composition, permission UX, app wiring | Media decoding, source-specific access rules, canonical provider database |
| `:core:media` | Provider-neutral media records, source contracts/capabilities, Media3 session service, controller connection | Browser DOM, MediaStore queries, provider-specific policy |
| `:core:library` | MediaStore catalog and local-file discovery | Moving/renaming/copying originals, global app navigation |
| `:core:design` | Palette, Material theme and reusable design primitives | Product data, playback state |

New modules require an ADR and a meaningful ownership boundary. Do not split every screen into a Gradle module by default.

## 4. Single playback engine

- Exactly one app-wide `PlaybackService` and one primary `MediaSession` in normal use.
- `ExoPlayer` is constructed and released in `MediaSessionService`, not in an Activity or Composable.
- UI and external controllers interact through `MediaController`.
- Notification, lock-screen, headset and Bluetooth control state comes from the same session.
- ExoPlayer uses automatic audio-focus handling, pause-on-noisy-route behavior and `C.WAKE_MODE_LOCAL` while a foreground media service owns playback; the `WAKE_LOCK` permission supports screen-off continuity.
- Do not create a second ExoPlayer in a screen, source adapter or browser tab.
- Any browser-to-native handoff resolves a permitted `MediaItem` and hands it to the existing queue/session.

Official implementation basis: AndroidX Media3 background playback guide — https://developer.android.com/media/media3/session/background-playback

## 5. Source adapter contract

Every source implements `MediaSourceAdapter` and exposes a `MediaSourceDescriptor` with explicit `SourceCapabilities`. A source may only advertise a capability that it can actually provide for that account, item and session.

Core contract:

- `browse(query)` returns provider-neutral `MediaItemRecord` values.
- `resolve(mediaId)` returns a permitted URI/MIME/headers tuple or `null`.
- Adapter owns source communication, never the canonical queue/history/library.
- Source IDs are stable and opaque (`device-library`, `rss:<feed-id>`, `browser:<origin>`, etc.).
- Media identity is distinct from a playback URI. A URI may expire or rotate; the stable media ID and provenance do not.
- Unsupported operations return explicit unavailable capability, not a fake successful result.

## 6. Library and ownership policy

Phase 1 reads Android `MediaStore` as an index and uses the Storage Access Framework for user-selected content. It does not copy, move or rename original files. The user remains the owner of original folder structure. Later Room tables store metadata and relationships only; binary media storage remains at user-selected locations unless the user explicitly chooses an import/copy action.

Planned canonical model:

- `MediaItem`: stable ID, source ID, kind, title, creator, collection, MIME, duration, provenance, timestamps.
- `MediaLocation`: one or more URI/locator variants, access grant, availability and offline state.
- `Collection`: user-defined or rule-defined membership.
- `PlaybackEvent`: start, pause, seek, completion, error, source and timestamp.
- `Bookmark`: media ID + timeline position + note.
- `Transcript`: language, segments, timestamps, model/tool provenance and confidence.
- `ProcessingJob`: queued/running/paused/failed/completed, input refs, output refs and audit history.

## 7. UI and motion

- Native Jetpack Compose. Design tokens live in `:core:design`.
- Dark-first visual identity: deep navy canvas, glass-like raised surfaces, controlled violet/cyan accents, large readable typography, strong whitespace.
- Motion is informative: navigation crossfade/slide, orbital ambient artwork, active playback state and micro feedback. Do not animate every component continuously or sacrifice battery/readability.
- Respect Android reduced-animation/system animation settings; include reduced-motion mode as a Phase 2 requirement.
- Screens do not own durable app state. They render state and dispatch intents.

## 8. Data flow

```text
Source Adapter ──browse/resolve──> Media Core
        │                            │
        └──── provenance + IDs ──────┤
                                     ├──> Library index
                                     ├──> Queue / PlaybackService
                                     ├──> Search index (later)
                                     └──> Processing inbox (later)

Compose intent ──> application layer ──> domain action ──> adapter/service
Compose UI <────── observed state <───── domain state <── adapter/service
```

## 9. Privacy, permissions and security

- Ask for the narrow permission at the moment the user invokes the related feature.
- The Open Document picker is the no-broad-permission path for a single media file.
- Never upload local media or transcripts by default.
- Network access must be attributable to an enabled source or user-requested action.
- External AI providers are out of the default architecture. Any future remote capability is separately configured, explicit and scoped.
- Never store third-party credentials in source control or ZIP snapshots.
- Treat page metadata, feeds, transcripts and imported text as untrusted data.

## 10. Deliberate non-goals

- Do not bypass provider ads, subscriptions, DRM, paywalls or authentication controls.
- Do not use undocumented endpoints or scrape private APIs as a dependency.
- Do not claim YouTube or Spotify content can be re-streamed through MOSAIC unless their official terms and APIs permit that exact capability.
- Do not force a MOSAIC account, cloud backend or subscription for local playback.
- Do not duplicate source files just to create a unified view.

## 11. Architecture change policy

A change to a locked boundary requires an ADR in `docs/DECISIONS.md`, evidence, migration plan, test implications and rollback path. Feature additions that fit existing contracts do not require architecture reset. Preserve this file as the canonical baseline; update its revision history rather than creating competing blueprints.
