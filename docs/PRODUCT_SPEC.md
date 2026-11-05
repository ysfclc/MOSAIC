# Product specification — MOSAIC

## Product statement

A native, local-first personal media environment that lets a person collect, organize and play content from multiple legitimate sources through one consistent interface, then optionally turn that content into searchable personal knowledge.

## Origin and founding problem — preserve this intent

MOSAIC began as a practical answer to fragmented personal media and the friction of moving something useful between apps. The user wanted a simple, repeatable way to take something encountered elsewhere — especially a copied URL, text snippet or media reference — bring it into MOSAIC, keep it for later, and then open, play, organize or share it from the place that made sense. The reverse direction matters too: content or references held in MOSAIC must be easy to copy, open or share back out to another app. This is a two-way movement of user-selected content and context, not a one-way browser bookmark feature.

The founding interaction must be obvious and low-friction:

1. **Copy → paste into MOSAIC:** the user copies a link or text in another app, opens MOSAIC and explicitly pastes it into a capture/paste entry point. MOSAIC identifies what was pasted, previews it, and lets the user save or act on it. The initial supported payloads are plain text and URLs; media files and richer metadata use Android's document/open/share mechanisms.
2. **Share → MOSAIC:** when the source app offers Android Share, the user can send a URL, text or supported file to MOSAIC. It enters the same capture flow as paste, rather than creating a disconnected feature.
3. **MOSAIC → other apps:** the user can copy a saved value, open its link in a compatible app, or share supported content/files outward using Android intents and appropriate URI grants.
4. **Keep it for later:** captured items should ultimately persist in a user-controlled inbox/library with source provenance, capture time, status and a clear next action. A URL pasted into the app is not yet a durable, searchable media library; that persistence work remains tracked in the roadmap.

The product grew from this capture-and-continue problem into a broader personal media environment: local media, podcasts, streams, browser-discovered content and permitted provider integrations should be collected and experienced through one coherent native app. The app is not intended to replace every provider or to pretend protected streams can be imported. It should preserve the user's ability to move references and user-owned files between apps while respecting source ownership and capability limits.

**Important distinction:** MOSAIC's paste/capture flow is a user-invoked content workflow. It is not VaultOS's separate device-wide clipboard-history/automatic clipboard-capture product. Do not silently turn MOSAIC into a clipboard manager or add background clipboard polling. Read clipboard content only in response to an explicit user action (for example, tapping Paste), subject to Android's clipboard privacy behavior. Android share/open intents remain first-class alternatives.

This founding use case is product-critical and must remain visible in Home/Browser/Inbox design, acceptance tests and future session handoffs. Do not demote it to a minor browser shortcut.

## Primary job to be done

“When I find something worth listening to or watching, I want to keep it accessible, continue it in the context I prefer, and organize it without being forced into a particular provider's app, account or subscription for capabilities I can own locally.”

## User promise

- Local media remains playable without an account or cloud.
- The app itself does not insert advertisements.
- Background playback is native for sources that permit it.
- The library is unified without destroying original folders.
- Sources advertise capabilities honestly.
- The user can export their metadata and choose backup destinations.

## User groups

1. **Personal media owner:** large local collection, wants indexing and reliable playback.
2. **Cross-source listener:** uses music, podcasts, internet radio and educational media.
3. **Browser-first collector:** saves articles, pages, videos and audio links for later.
4. **Knowledge-oriented listener:** wants transcripts, chapters, translation and summaries.

Phase 1 specifically serves the personal media owner and validates the playback foundation.

## Product areas

- Home: recent/continue state, library entry points and source health.
- Library: unified items, filters, collections, search and provenance.
- Player: one queue, timeline, transport, speed, timer, metadata, playback route.
- Browser: tabs, bookmarks, capture and permitted media handoff (later).
- Inbox: share-target capture and processing queue (later).
- Intelligence: transcript, translation, summary and semantic search (later; local-first).
- Settings: permissions, storage, source controls, backup, privacy and diagnostics.

## Phase 1 acceptance

See `docs/TASKS.md` and `docs/TESTING.md`. Phase 1 is accepted only after CI build/lint/unit tests and real Redmi screen-off playback tests pass. Compiling an APK does not itself prove device behavior.

## Cross-source playback and ad-control requirements (product-critical)

MOSAIC is a media hub, not merely a local file player. The intended experience is to discover, collect, continue and control media across sources from one coherent native interface. The source capability contract is authoritative: unsupported provider capabilities must be shown as unavailable, not simulated.

### Ad control
- MOSAIC must never insert its own ads.
- MOSAIC Browser must provide an on/off ad and tracker request filter for ordinary web pages, with a visible blocked-request count, per-site/provider exemptions and a user allowlist.
- The initial filter is request/host-level. It is not a promise to remove every ad, cosmetic placeholder, first-party/in-stream ad or ad delivered from the same host as content.
- YouTube and Spotify player pages and assets are exempt from MOSAIC filtering. The app must not strip, replace or interfere with provider-served advertisements or standard player functions.
- A future optional local DNS/VPN filter may block known third-party ad/tracker domains across apps with explicit user consent. It must disclose VPN conflicts, compatibility limitations and the fact that same-domain/in-stream ads remain outside its reach.
- No TLS interception, user certificate installation, private API scraping or DRM/paywall bypass.

### Background playback and source ownership
- MOSAIC-owned sources (local files, permitted RSS enclosures, open radio streams and direct media URLs that the user is authorized to play) use the shared Media3 PlaybackService and can continue in the background.
- Provider-owned sources remain in their provider player when the provider does not grant native stream playback. MOSAIC may show metadata and issue authorized remote commands, but must not claim provider audio is playing inside the MOSAIC player.
- Background playback is a capability evaluated for each source, account, item and session. YouTube API playback must remain visible in its player and must not be converted into a hidden/background audio stream. Spotify Android App Remote controls the installed Spotify app; Spotify owns audio delivery and its background lifecycle.

### Two-way media bridge ("vice versa")
1. Into MOSAIC: Android share/open intents can send media links, files and metadata into the MOSAIC inbox. MOSAIC can discover/control an external active media session only after the user explicitly enables Android notification access and the source exposes a controllable MediaSession. This does not transfer protected audio bytes into MOSAIC.
2. Out of MOSAIC: MOSAIC can share a link or a user-owned/local file through Android intents and temporary read grants, open it in a compatible external app, and expose its own playback through the standard Android MediaSession for system/headset controls.
3. Remote control: where an official provider SDK exists, MOSAIC may control that provider's player after user authorization. Provider playback remains provider-owned.
4. No false universal queue: native-playable sources can share the MOSAIC queue. Provider-owned playback is represented as a delegated/external session; MOSAIC cannot promise gapless mixing or arbitrary queue insertion across unrelated providers.

### Source capability labels
- MOSAIC PLAYER: MOSAIC owns playback; background support is shown independently.
- EXTERNAL PLAYER: provider app owns playback; MOSAIC can show/control it only where authorized.
- BROWSER PLAYER: media remains in the browser page; native handoff is offered only when allowed and technically available.
- LINK ONLY: MOSAIC can save/open/share the reference but cannot play or control it itself.

### Account-owned provider background playback

- If a user's YouTube app/account supports background playback (for example, through the applicable YouTube Premium entitlement), the official YouTube app remains the player. MOSAIC may control its exposed Android media session after notification-access opt-in, but cannot grant that entitlement to a free account.
- Spotify's installed app remains the player for both account tiers where the Spotify app itself permits background playback. MOSAIC's Android App Remote integration is a remote-control surface, not an audio-streaming SDK.
- MOSAIC cannot route arbitrary local audio into Spotify or YouTube as if either were a generic audio sink. Outbound handoff means opening/sharing a supported link or user-owned file with an app that accepts it.
