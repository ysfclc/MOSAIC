# MOSAIC — Product Vision

**Status: LOCKED PRODUCT NORTH STAR**  
**Origin brainstorm:** 2026-10-30  
**Reconfirmed and corrected:** 2026-11-05 to 2026-11-06

## The idea

MOSAIC is a **personal media operating system for Android**.

It brings the media people already use into one coherent environment: discover it, open it, play it, keep it going, organize it, return to it and eventually understand and automate it. The point is not to build one more music player. The point is to reduce the fragmentation between the many apps, websites, files and services people use every day.

The product should feel like one living media space rather than a set of unrelated tools.

**The core promise:** whatever the source, MOSAIC should give the user the best continuous, unified media experience that Android and that source genuinely allow.

## The original product pillars

### 1. One media experience across sources

The intended source scope is broad and must not be quietly narrowed:

- YouTube
- Spotify
- Facebook
- Instagram
- Browser and web media
- Local audio and video
- Podcasts and RSS feeds
- Internet radio
- Direct audio/video streams
- Other Android apps and accessible media sources

MOSAIC should investigate each source through Android-native capabilities first where possible, supported browser playback, accessible media handoff, Android media sessions, and source-specific integrations only where they materially unlock a capability.

A source appearing in a list is not integration. Each source needs a real, working journey and an honest capability status.

### 2. Playback that follows the user

Playback is the centre of the product, not a tab hidden among utilities.

- Continue MOSAIC-owned playback while the user switches apps or locks the phone.
- Provide one beautiful, consistent player surface and mini-player.
- Support queue, timeline, play/pause, next/previous, resume, playback speed, sleep timer and system/lock-screen/Bluetooth controls where available.
- Preserve provenance: always know where an item came from.
- Keep MOSAIC-native playback distinct from browser-owned and external-app playback.
- Bring external session controls into the same Player experience when Android exposes them.

A single native queue can contain only media MOSAIC can actually play. Delegated external sessions can be surfaced and controlled, but must not be represented as audio imported into MOSAIC.

### 3. Ad and tracker protection

Ad Shield is a first-class product pillar, not a small browser setting.

- Filter ads and trackers inside MOSAIC Browser.
- Investigate and provide optional, user-consented device-wide filtering for separable ad/tracker domains.
- Give users clear protection status, allowlists, diagnostics and safe recovery.
- Measure source compatibility and false positives.
- Do not silently exempt a named service from investigation.
- Never claim universal ad removal: same-host, first-party and in-stream ads may be inseparable from content through ordinary DNS/host filtering.

Use platform-compliant, secure mechanisms. No DRM circumvention, credential theft, private API scraping, TLS interception or deceptive claims.

### 4. A unified personal library

The Library is the user's media memory, not merely a MediaStore screen.

- Bring together local items and supported source references with clear provenance.
- Preserve original files and folders; indexing must not move, rename or duplicate them.
- Support search, filters, collections, bookmarks, playback history and resume state as the data model matures.
- Keep local data useful without a cloud account.
- Make export and backup possible without trapping the user's metadata.

### 5. Intelligence and automation

These are later capabilities in the original vision, not reasons to distract from the playback core:

- Transcripts with timestamps
- Chapters and highlights
- Translation and summaries
- Semantic search across a personal media library
- Smart collections and playlists
- User-defined import and processing rules
- Optional local AI and controllable processing queues

AI must be optional, provenance-aware and local-first where practical. It must never fabricate transcript evidence.

## Product experience and information architecture

The approved direction combines a playback-first, complete-architecture and platform-first product. It should feel like a premium, futuristic operating environment: clean, beautiful, alive, calm and purposeful. Motion should communicate state and continuity, not decorate every surface.

### Home — the command centre

Home answers: what is happening, where can I continue, and what can I do next?

- A prominent now-playing/continue surface.
- Source shortcuts and recent/active source context.
- A clear route into Explore, Library, Player and Ad Shield.
- Useful status, never fake analytics or decorative phase counters.
- Empty states that guide the user into the product rather than exposing developer setup tasks.

### Explore — source discovery

Explore is the source-first gateway.

- Recognizable source entry points for requested services.
- Local, browser, podcast, radio and stream routes.
- Clear distinction between opening an external app and playing natively in MOSAIC.
- Capability labels and useful next actions.
- Browser is one source surface, not the whole Explore experience.

### Player — the centrepiece

- One premium player with artwork/video, metadata, progress, queue and transport.
- Mini-player remains available while navigating.
- MOSAIC-owned, browser-owned and external sessions are clearly labelled.
- External sessions appear in the same player environment when accessible, without pretending their audio ownership changed.
- Advanced controls appear when the source supports them.

### Library — the personal media memory

- Unified, searchable and organized.
- Local originals remain untouched.
- Source and playback provenance stay visible.
- Empty, permission and loading states are designed as part of the product.

### Browser — a supporting source surface

- Helps access web media and pages.
- Can hand off directly playable media to the shared player when technically accessible.
- Has its own request filtering.
- Must not become a toy browser or the product's primary identity.

### Ad Shield — protection centre

- Separate browser and device-wide protection status.
- Explicit VPN consent and clear conflict/recovery behavior.
- Allowlist, blocked-domain diagnostics and coverage limitations.
- No false promise that DNS filtering removes every in-stream ad.

### Intelligence and Automation — later layers

These build on a reliable media identity, history, library and processing model. They should feel integrated into the same media OS, not bolted-on chatbot or settings screens.

## Product principles

1. **Source-independent, not source-blind.** One coherent experience, with real per-source capability awareness.
2. **Playback-first.** The primary user journey is media discovery and continuity, not configuration.
3. **Android-native first.** Use Android services, media sessions, intents, notifications and other public system capabilities where they are the right tool.
4. **Local-first ownership.** No mandatory account, subscription or cloud dependency for core playback and library features.
5. **No MOSAIC-inserted ads.** MOSAIC does not monetize the user's media experience by inserting advertising.
6. **Beautiful and usable.** Premium visual design, readable hierarchy, responsive layouts and purposeful motion are product requirements.
7. **Honest capability.** A button, metadata card or successful page load is not proof of playback integration.
8. **Secure by default.** Narrow permissions, explicit user consent, no invasive traffic interception, no hidden collection.
9. **Incremental without losing the whole.** Build one tested slice at a time, but each slice must advance the complete vision.

## Playback ownership model

Every playable item/session must use one explicit mode:

| Mode | Who owns playback | What MOSAIC can promise |
|---|---|---|
| MOSAIC Player | MOSAIC Media3 service | Native queue/session and background continuity after device acceptance |
| Browser Player | MOSAIC WebView/browser engine | Browser playback and only the background behavior actually verified |
| External Player | Original app | Only the Android-exposed session controls and continuation the source provides |
| Link only | No active player | Open/save reference only; not counted as playback support |

This distinction is essential. Android `MediaSessionManager` can expose metadata and transport actions; it does not transfer another app's audio into MOSAIC or override that app's background policy.

## Explicit non-goals and anti-patterns

MOSAIC must not drift into:

- A clipboard manager or copy/paste product.
- A generic local music player with a few unrelated panels.
- A toy browser that happens to have a play button.
- A generic dashboard full of fake metrics, phase labels or unconnected status cards.
- A collection of isolated feature demos with no shared media journey.
- A product that claims YouTube, Spotify or social-media audio is natively playing in MOSAIC when it is not.
- A mandatory cloud account, subscription or remote AI dependency.

## Definition of meaningful progress

A feature is meaningful only when it completes a user journey, integrates into the shared media model/player where appropriate, has real states and error handling, is tested, and is reflected in the source capability matrix and project ledger.

A CI APK proves that the code builds. It does not prove source compatibility, background continuation, ad filtering or usability on the Redmi Note 12 Pro 5G. Those require physical-device acceptance.

## Delivery sequence

1. Lock this vision and make Home/Explore reflect it.
2. Make the source-first journeys and unified Player genuinely usable.
3. Validate MOSAIC-native playback, browser handoff and external-session control on the target device.
4. Complete Ad Shield compatibility, diagnostics and safe recovery.
5. Expand the durable Library and media identity/history.
6. Add Intelligence and Automation only after the media core supports them.

This document is the product north star. Architecture, tasks, UI and future sessions must reconcile against it. Changes to the vision require an explicit user decision; implementation shortcuts do not silently redefine the product.
