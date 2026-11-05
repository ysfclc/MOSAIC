# Source adapter contract

## Purpose

Adapters isolate source-specific discovery, authentication, availability, playback resolution and capability reporting. The rest of MOSAIC consumes provider-neutral media records and a canonical playback model. The product target is broad background playback from external sources; source-specific limitations must be investigated and tested rather than encoded as blanket exclusions.

## Required descriptor

- Stable source ID and display name.
- Whether the source requires network.
- Capabilities for browse, search, MOSAIC-native playback, MOSAIC background playback, seek, speed control, offline copy, transcript and external-session control.
- User-visible authentication requirement and connection state.
- Playback ownership mode: MOSAIC, browser, or external source.
- Source-specific compatibility and diagnostic metadata where required.

## Required operations

`browse(query)` returns a list of `MediaItemRecord`; `resolve(mediaId)` returns a `ResolvedMediaSource` or `null`. Adapters may add pagination, refresh, collection and authorization operations in later interface revisions, but they must not mutate canonical library or player state directly.

## Capability rules

- Capability is evaluated per adapter and, where necessary, per item, account, app version and session.
- `NATIVE_PLAYBACK` means MOSAIC can technically resolve and play that item in its own shared Media3 player through an authorized/accessible media locator.
- `BACKGROUND_PLAYBACK` means that exact item can continue through MOSAIC's Media3 service when the UI is backgrounded.
- `EXTERNAL_CONTROLS` means MOSAIC can send supported commands to a source-owned Android MediaSession; it does not imply audio access or native playback.
- `OFFLINE_COPY` means an offline copy is permitted and technically supported. It is not inferred merely because bytes are reachable.
- `TRANSCRIPT` means transcript content is available or a permitted local transcription path exists.
- No capability is granted by guessing from a webpage URL, app name or metadata response.
- Ad filtering is a separate subsystem and must not be represented as a media playback capability. Track browser filtering and device-wide filtering independently.

## Playback ownership modes

- **MOSAIC:** a source adapter resolves media into a URI/MIME/headers tuple and the shared Media3 session owns playback.
- **BROWSER:** the media remains in the browser surface; background behavior and system controls require independent tests.
- **EXTERNAL:** the source app owns the audio session. MOSAIC may observe/control only Android-exposed session metadata and commands after explicit user authorization. It cannot claim the audio is being played by MOSAIC.
- **REFERENCE:** MOSAIC can retain/open a source reference but cannot demonstrate playback.

A unified queue may contain only items that MOSAIC can actually play. External sessions may appear in a separate "Now playing elsewhere" control surface; do not fake them as native queue items.

## Source investigation requirements

For YouTube, Spotify, Facebook, Instagram and other media sources, investigate:
- Official APIs/SDKs and their actual playback/remote-control scope.
- Source-owned Android background behavior and exposed MediaSession actions.
- Browser playback behavior inside and outside MOSAIC.
- Accessible media resources and whether use through MOSAIC's player is technically and contractually supported.
- Whether known ad/tracker domains can be blocked without breaking core content delivery.

Do not automatically exclude a source from investigation because its app currently restricts background playback or because its ad delivery is difficult to filter. Do not claim a capability before the corresponding device/source test.

## Ad-control contract

The ad shield is independent of source adapters:
- Browser filtering operates on requests made by MOSAIC's browser.
- Device-wide filtering is a separate, optional, user-enabled Android local VPN/DNS filter.
- Rules are source-aware, attributable and testable; there is no blanket provider exemption.
- Never block media/CDN hosts blindly. Record breakage and provide per-app/per-domain allowlisting.
- DNS filtering cannot inspect encrypted page content or distinguish same-host/in-stream ads from content. No TLS interception, DRM circumvention, credential theft or private API scraping is an acceptable shortcut.

## Phase 1 adapter

`LocalMediaSourceAdapter` reads Android MediaStore and advertises local browse/search/native/background/seek/speed/external-control support. The app uses the system URI as the playback locator; original files remain where Android indexed them.

## Planned adapters

- RSS: podcast feed/episode metadata and enclosure URL, with HTTP caching/conditional requests.
- Radio: stream URL, media type, ICY metadata and reconnect policy.
- Direct media: URI/MIME detection, safe resolution, source headers only where legitimately available.
- Browser: optional page/media discovery and handoff for accessible media. It is not the core app.
- Provider/external: source-specific discovery and exposed control APIs, with accurate playback ownership.
