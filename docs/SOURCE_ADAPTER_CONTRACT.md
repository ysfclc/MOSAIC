# Source adapter contract

## Purpose

Adapters isolate source-specific discovery, authentication, availability and resolution. The rest of MOSAIC consumes provider-neutral records and an explicit capability description.

## Required descriptor

- Stable source ID and display name.
- Whether the source requires network.
- Capabilities for browse, search, native playback, background playback, seek, speed control, offline copy, transcript and external controls.
- User-visible authentication requirement and connection state (introduced with network adapters).

## Required operations

`browse(query)` returns a list of `MediaItemRecord`; `resolve(mediaId)` returns a `ResolvedMediaSource` or `null`. An adapter may add pagination, refresh, collection and authorization operations in a later interface revision, but it must not mutate canonical library or player state directly.

## Capability rules

- Capability is evaluated per adapter and, where necessary, per item/session.
- `NATIVE_PLAYBACK` means the app is allowed and technically able to play the resolved item in the MOSAIC player.
- `BACKGROUND_PLAYBACK` means that exact source/item can legally and technically continue through the MOSAIC service when the UI is backgrounded.
- `OFFLINE_COPY` means an offline copy is permitted by source terms and technically supported. It is not inferred merely because bytes are reachable.
- `TRANSCRIPT` means transcript content is available or a permitted local transcription path exists.
- No capability is granted by guessing from a webpage URL.

## Phase 1 adapter

`LocalMediaSourceAdapter` reads Android MediaStore and advertises local browse/search/native/background/seek/speed/external-control support. The app uses the system URI as the playback locator; original files remain where Android indexed them.

## Future adapters

- RSS: podcast feed/episode metadata and enclosure URL, with HTTP caching/conditional requests.
- Radio: stream URL, media type, ICY metadata and reconnect policy.
- Browser: page reference and accessible media element; native handoff only when supported/allowed.
- YouTube: official APIs and embedded playback boundaries; no extraction/ad bypass.
- Spotify: official app-control/metadata interfaces; no copying Spotify audio into MOSAIC.

## Cross-app bridge semantics

The adapter contract distinguishes media that MOSAIC can play from media that MOSAIC can only reference or remotely control.

- Native adapters return a permitted URI/MIME/headers tuple for the shared Media3 session.
- Browser adapters may return a native handoff only when the page exposes a permitted direct media resource and the site/source allows that use.
- Provider remote adapters return provider identity, current metadata, supported remote commands and connection state; they do not return a provider audio URI.
- Share-in adapters accept Android ACTION_SEND / ACTION_VIEW payloads and create an inbox reference. They do not silently download protected content.
- Share-out uses Android intents with URI grants for user-owned content or ordinary links.
- External-session adapters identify the package/session, supported actions and whether user notification access is enabled. A session being visible does not grant access to its audio bytes.

Capability reporting keeps these dimensions independent: NATIVE_PLAYBACK, BACKGROUND_PLAYBACK, EXTERNAL_CONTROLS, OFFLINE_COPY, SHARE_IN, SHARE_OUT and REMOTE_PLAYBACK_CONTROL. Do not infer one from another.
