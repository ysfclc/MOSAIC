# Architecture decision record (ADR) log

This file is the canonical decision log. A decision marked **LOCKED** may only change through a superseding ADR with evidence and migration notes.

## ADR-001 — Native Android first (LOCKED)

- **Date:** 2026-10-30
- **Decision:** Kotlin + Jetpack Compose native Android app, initially targeting the Redmi Note 12 Pro 5G.
- **Why:** first-class Android playback service, storage permissions, media sessions, notifications, Bluetooth and file-picker integration.
- **Trade-off:** no iOS/desktop parity in early releases.

## ADR-002 — One Media Core and one primary player (LOCKED)

- **Decision:** one `MediaSessionService` owns the primary ExoPlayer and MediaSession; all UI/system controllers connect through MediaController.
- **Why:** consistent background lifecycle, notifications, lock-screen controls and external controls.
- **Rejected:** each feature/source creating its own player.
- **Migration rule:** browser or remote playback adapters must hand permitted media into the shared session or remain on their source player with explicit capability reporting.

## ADR-003 — Source adapter + explicit capability contract (LOCKED)

- **Decision:** each source implements `MediaSourceAdapter` and declares actual capabilities.
- **Why:** providers differ; a universal interface must not imply universal rights or playback support.
- **Rejected:** provider-specific branches throughout screens and canonical library logic.

## ADR-004 — Local-first library; preserve original media (LOCKED)

- **Decision:** index existing local media without copying, moving or renaming files; use persisted SAF URI grants for individually selected items.
- **Why:** ownership, low storage use and preservation of the user's existing folder organization.
- **Later:** explicit user-directed import/copy workflows may be added with clear storage impact and reversible operations.

## ADR-005 — No mandatory backend, account or AI provider (LOCKED)

- **Decision:** core playback/library works locally and without sign-in. AI is optional and local-first.
- **Why:** user ownership, privacy and operating-cost constraints.
- **Rejected:** mandatory remote processing or a cloud account to unlock local capabilities.

## ADR-006 — Provider policy boundaries (LOCKED; SUPERSEDED IN SCOPE BY ADR-014)

- **Decision:** no ad removal, DRM circumvention, paywall bypass, private API scraping or unauthorized audio extraction. Integrate only capabilities that the provider permits.
- **Why:** legal resilience, reliability and avoidance of fragile integrations.
- **Trade-off:** certain third-party services cannot offer the same background or offline behavior as local/RSS media.

## ADR-007 — Modular Android Gradle project (LOCKED)

- **Decision:** start with `:app`, `:core:media`, `:core:library`, `:core:design`. New Gradle modules need a real ownership boundary and an ADR.
- **Why:** stable separation without over-fragmenting features.

## ADR-008 — Pinned CI toolchain and ZIP-based integration (LOCKED)

- **Decision:** CI pins Java 17, Gradle 9.3.1, AGP 9.1.1 and SDK package versions. One full-source ZIP is uploaded at `incoming/mosaic.zip`; the workflow validates, bootstraps a pinned Gradle Wrapper on the first successful import, builds and commits an upload note.
- **Why:** reproduce the Memo handoff method while protecting active workflows and source history.
- **Trade-off:** the remote repo must be created and the workflow installed once manually; subsequent source updates are ZIP-driven.

## ADR-009 — Phase One scope boundary (LOCKED)

- **Decision:** Phase 1 delivers native local indexing and background playback, not full provider parity. Browser/RSS/radio/provider adapters/AI/sync stay in the locked roadmap and contracts.
- **Why:** prove the highest-risk Android foundation before adding network and provider complexity.

## ADR-010 — Ad control is scoped by source (LOCKED; SUPERSEDED IN SCOPE BY ADR-014)

- Date: 2026-11-04.
- Decision: MOSAIC owns an ad/tracker filter for ordinary web pages inside MOSAIC Browser. Filtering is conservative, user-toggleable, measurable and has provider/site exemptions. A later device-wide DNS/VPN mode is optional and separately consented.
- Supersedes ADR-006 only for permitted third-party request filtering on ordinary web pages. ADR-006 remains fully in force for YouTube/Spotify player ads, DRM, paywalls, private APIs and protected audiovisual streams.
- Why: the user requires meaningful ad control, but provider playback integrity and source ownership must remain intact.
- Limit: host/request filtering cannot remove all ads, first-party ads or in-stream ads and must never be marketed as universal ad removal.

## ADR-011 — Two-way media bridge with explicit playback ownership (LOCKED)

- Date: 2026-11-04.
- Decision: MOSAIC supports inbound capture/control (share intents, accessible media references, and explicitly authorized Android MediaSession control) and outbound handoff (Android open/share intents and MOSAIC's own MediaSession).
- Provider-owned playback remains provider-owned. Official SDKs may expose metadata and remote commands; they do not automatically grant MOSAIC the provider's audio stream or background-playback rights.
- Why: "vice versa" means media and control must move in both directions, not just import URLs into MOSAIC.
- Limit: no raw protected-stream interception, audio extraction, private API scraping or promise of one gapless queue across unrelated provider players.

## ADR-012 — YouTube and Spotify integration capability split (HISTORICAL; SUPERSEDED BY ADR-014)

- Date: 2026-11-04.
- YouTube: use permitted official metadata/search and standard visible embedded playback. Preserve provider ads/player UI. No background player, ad blocking, audio/video separation or offline copy.
- Spotify: use the official Android App Remote SDK, subject to current availability and terms, to connect to the installed Spotify app after user authorization. Display permitted player state and send supported commands. Spotify owns audio streaming and background lifecycle.
- Android external sessions: discover/control other active media sessions only through Android's supported access path and an explicit user-enabled NotificationListenerService; never silently request notification access.
- Why: this is the maximum honest integration surface supported by current provider and Android APIs. Re-evaluate against provider policy before release.

### Official references reviewed 2026-11-04

- YouTube Developer Policies: https://developers.google.com/youtube/terms/developer-policies-guide
- Spotify Android SDK (Beta): https://developer.spotify.com/documentation/android
- Spotify Android App Remote getting started: https://developer.spotify.com/documentation/android/tutorials/getting-started
- Android MediaSessionManager: https://developer.android.com/reference/android/media/session/MediaSessionManager


## ADR-013 — Explicit copy/paste capture is a founding product workflow (RETRACTED — INCORRECT ORIGIN)

- **Date:** 2026-11-05
- **Decision:** MOSAIC must support two-way user-selected content movement: copy plain text/URLs in another app and explicitly paste into MOSAIC; Android Share/Open intents provide equivalent inbound paths; captured content can subsequently be copied, opened or shared back out.
- **Why:** MOSAIC originated to reduce the friction of moving useful media references and content between apps, keeping them for later and continuing in the user's chosen context. This capture-and-continue workflow is core product purpose, not an optional browser convenience.
- **Implementation boundary:** paste is invoked by a deliberate user action. Do not poll the clipboard in the background or implement clipboard history/automatic clipboard capture; that is a separate VaultOS product concern. Android clipboard privacy behavior and URI grants must be respected.
- **Acceptance:** copied URL/text can be pasted, previewed, saved durably, retrieved after process death, then copied/opened/shared outward. Android Share must converge on the same inbox model. File capture is handled through supported Android intents/document access and must not imply arbitrary provider-stream extraction.
- **Tracking:** P3-009, P3-010 and P3-011 in TASKS.md.


## ADR-014 — Cross-source background playback and broad ad blocking are the product vision (LOCKED)

- **Date:** 2026-11-05
- **Decision:** MOSAIC's primary product goal is to play media from as many external sources as technically feasible in the background and block ads/trackers across as many surfaces and apps as technically feasible. Named sources to investigate include YouTube, Spotify, Facebook and Instagram, alongside browser media, local files, direct URLs, podcasts and radio.
- **Supersedes:** ADR-006, ADR-010 and ADR-012 wherever they impose a blanket restriction or provider exemption that narrows this product goal. ADR-013 is retracted as an incorrect statement of the founding purpose; copy/paste is not MOSAIC's origin or primary product requirement. ADR-011 remains useful as an implementation distinction between native playback and delegated external control, but it must not be used to stop source-by-source investigation.
- **Implementation approach:** investigate each source through accessible media streams, browser playback, official SDK/API integrations, Android MediaSession controls and other technically/policy-viable paths. Prefer MOSAIC's shared Media3 player when a source can actually be played there. Distinguish native playback from remote control of another app.
- **Ad control:** implement and test browser filtering and investigate optional user-consented Android VPNService/local DNS filtering across apps. Do not blanket-exempt named providers. Track coverage, false positives, playback breakage and first-party/in-stream limitations.
- **Engineering honesty:** this ADR records the user's goal, not a claim that every provider can currently be integrated or every ad can be removed. Each source needs current technical, Android and policy research plus real-device acceptance. Do not use DRM circumvention, credential theft, private API scraping or TLS interception as shortcuts.
- **Acceptance:** a source matrix demonstrates foreground/background behavior, screen-off continuity, lock-screen/Bluetooth controls, native vs delegated ownership, queue support and ad-filter coverage for each tested source. A separate filter matrix demonstrates browser and device-wide behavior, including compatibility and known gaps.
