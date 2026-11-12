# Product specification — MOSAIC

> Canonical product north star: [MOSAIC Product Vision](MOSAIC_PRODUCT_VISION.md). This specification implements that broader personal media OS vision; it must not narrow MOSAIC to a local player, browser or ad-blocking utility. Requirement IDs and implementation/test/documentation coverage are indexed in [SPEC.md](SPEC.md).

## Product statement

A native Android personal media operating system whose central goal is to unify discovery, playback continuity, library, source access and ad/tracker protection across as many apps and surfaces as Android and each source technically allow. Local media, browser media, streams, podcasts, radio and third-party services should feel like one coherent media experience; source-specific limitations must be investigated and tested, not assumed away or used to quietly narrow the vision.

## Origin and founding problem — cross-source background playback and ad blocking

MOSAIC exists because media is fragmented across apps and websites, background playback is restricted or inconsistent, and advertising interrupts the experience. The user's intended product is straightforward and ambitious: **play media from other sources in the background, and block ads wherever possible**. The user explicitly wants this to cover sources such as YouTube, Spotify, Facebook, Instagram and other media services — not just files already stored on the phone.

The core experience is one place to find or open media, start playback, continue listening or watching while using other apps or with the screen off, and control playback through one coherent interface. MOSAIC should pursue the broadest technically feasible ad and tracker blocking, including inside its own browser and, where feasible, across other apps on the device.

This is the product vision. It must not be rewritten as a clipboard manager, a bookmark inbox, a toy browser, or merely a local-file player. Sharing links into or out of MOSAIC may be useful supporting functionality, but copy/paste is not the founding purpose.

### How source support should be approached

- Investigate each source independently: native/direct media URLs, RSS/podcasts, internet radio, browser playback, official SDK/API integrations, and Android's external media-session controls.
- Prefer playback through MOSAIC's shared native playback service when the media can be accessed and played that way.
- Where a source remains in its own app or browser, determine whether MOSAIC can keep it playing, control it, or provide a better supported playback path. Do not confuse remote play/pause controls with MOSAIC actually receiving and playing the audio.
- Maintain a source capability matrix with clear evidence for foreground playback, background playback, lock-screen controls, queue support, ad filtering and known limitations.
- YouTube, Spotify, Facebook, Instagram and other named sources are requirements to investigate, not blanket exemptions from the product vision. Do not declare them supported until tested; do not exclude them without recording technical and policy evidence.

### Ad-control approach

- The goal is to block ads and trackers as broadly as technically feasible, both in MOSAIC Browser and, if viable, across other apps.
- Evaluate browser request filtering and an optional user-consented Android local VPN/DNS filter for device-wide domain blocking. Record per-app coverage, VPN conflicts, false positives and battery impact.
- Do not silently exempt a provider simply because it is a major service. Test the effect of filtering on both ads and core playback, and provide user-visible controls and diagnostics.
- Be honest about technical limits: first-party, same-domain and in-stream ads may be inseparable from media delivery through ordinary DNS/host filtering. Do not claim universal blocking until a source/device test matrix proves it.
- Any implementation must be reviewed for Android platform rules, source terms and applicable law. Do not use DRM circumvention, credential theft, private API scraping or TLS interception as shortcuts. These constraints require source-by-source engineering research; they do not change the user's stated product goal.

### Important distinction

MOSAIC's primary problem is cross-source background media playback and broad ad blocking. It is not VaultOS's device-wide clipboard-history product. Share/open intents can be supported as convenient ways to bring media into MOSAIC, but they are supporting features rather than the founding workflow.

## Primary job to be done

“When I find something worth listening to or watching, I want to keep it accessible, continue it in the context I prefer, and organize it without being forced into a particular provider's app, account or subscription for capabilities I can own locally.”

## User promise

- Local media remains playable without an account or cloud.
- The app itself does not insert advertisements.
- Background playback is a first-class goal across sources: use MOSAIC's native player where media is accessible, and source-owned continuation/control where it is not.
- The app remains useful with the screen off, while switching apps, and through Android lock-screen/notification/headset controls.
- Ad Shield aims to block known ad/tracker traffic in MOSAIC Browser and across other apps through an optional user-enabled local VPN/DNS filter.
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

- Home: current playback, source entry points, Ad Shield status and quick controls.
- Player: the primary surface — current item, unified queue for MOSAIC-playable media, timeline, transport, speed, timer, metadata and playback ownership.
- Sources: entry points and connection/status for local media, podcasts, radio, direct streams and named external providers.
- Ad Shield: separate browser and device-wide controls, protection status, blocked-domain diagnostics, allowlists and VPN state.
- Library: unified items, filters, collections, search and provenance.
- Browser: a supporting source surface for web media and links, not a standalone browser-first product.
- Intelligence: transcript, translation, summary and semantic search (later; local-first).
- Settings: permissions, storage, source controls, privacy and diagnostics.

## Phase 1 acceptance

See `docs/TASKS.md` and `docs/TESTING.md`. Phase 1 is accepted only after CI build/lint/unit tests and real Redmi screen-off playback tests pass. Compiling an APK does not itself prove device behavior.

## Cross-source playback and ad-control requirements (product-critical)

The product target is broad cross-source playback in the background and broad ad blocking. The roadmap must work toward this target rather than preemptively limiting MOSAIC to local files or exempting major services.

### Background playback
- MOSAIC's own Media3 PlaybackService is the primary playback engine for local files and any accessible/authorized media stream that can be played natively.
- Add and test adapters for RSS/podcasts, internet radio, direct audio/video URLs and browser-discovered media where technically accessible.
- Investigate YouTube, Spotify, Facebook, Instagram and other sources individually. Explore official integrations, supported browser playback, available Android media sessions and other permitted routes. Record what actually works on the target device.
- External MediaSession controls are useful but are not equivalent to importing a source's audio into MOSAIC's own player. The UI and capability matrix must distinguish native playback from delegated/external playback.
- Background playback must be tested with screen off, app switching, lock screen, notifications, Bluetooth/headsets, audio focus and process lifecycle.
- Do not mark a source supported based only on opening its page or seeing its metadata.

### Ad and tracker blocking
- MOSAIC must provide a visible, user-controllable ad/tracker shield in its own browser.
- Investigate an optional Android VPNService-based local DNS/filtering mode for blocking known ad/tracker domains across other apps, with explicit consent, clear VPN-conflict handling and per-app diagnostics where possible.
- Do not hard-code blanket exemptions for YouTube, Spotify or other named services without source-specific testing and a documented reason.
- Track blocked requests/domains, allowlists, false positives, breakage and source compatibility. Make limitations understandable to the user.
- Host/DNS filtering cannot reliably remove every first-party, same-domain or in-stream ad. This is a technical limitation to measure, not a reason to describe the product as browser-only ad blocking.

### Source capability labels
- MOSAIC PLAYER: media is played by MOSAIC's native player; background capability is independently tested.
- BROWSER PLAYER: media remains in MOSAIC Browser; background behavior and media controls are independently tested.
- EXTERNAL PLAYER: another app owns playback; MOSAIC may control its exposed Android MediaSession when authorized.
- LINK ONLY: MOSAIC can open or save a reference but has not demonstrated playback.
- Ad filtering is a separate capability dimension. A source can support playback without ad filtering, or filtering without native playback.

### Two-way media handoff
1. Into MOSAIC: Android share/open intents can bring links and supported user-selected files into the app.
2. Out of MOSAIC: MOSAIC can open/share links and user-owned files through Android intents and expose its own session to system controls.
3. External control: with explicit notification-access permission, MOSAIC may discover and control commands exposed by another app's Android MediaSession. This does not itself transfer audio bytes into MOSAIC.
4. One queue: only sources actually playable by MOSAIC can join its native queue. Delegated sessions must be labelled honestly until a supported native playback path exists.

### Current implementation status
The current source snapshot contains read-only local audio indexing and Media3 playback; validated HTTPS stream input including supported HLS; bounded HTTPS RSS 2.0 / Atom feed loading with secure parsing, local feed subscriptions and episode queueing; direct HTTPS HTML5 audio/video detection in Browser with a handoff into the shared Media3 player and a native video surface; delegated Android MediaSession controls; an opt-in YouTube-only visible ad-skip helper; browser third-party request filtering; and an opt-in DNS-only local VPN Ad Shield. These are foundations, not completion of the cross-source background playback or broad ad-blocking vision. Named external providers have not been demonstrated as MOSAIC-native streams, and real Redmi/device acceptance is not recorded. Browser filtering still has legacy blanket YouTube/Spotify exemptions contrary to this section's source-aware requirement; see P3-015 in docs/TASKS.md and the deviation recorded in docs/PROVIDER_CAPABILITY_MATRIX.md. See docs/SPEC.md for per-requirement evidence.
