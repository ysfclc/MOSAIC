# Phases and release gates

**Rule:** complete one phase, verify it, update this file and `PROJECT_STATE.md`, then begin the next. The full architecture is established now; feature delivery remains phased.

## Phase 1 — Native foundation and local playback (CURRENT)

**Outcome:** a useful Android app that can index existing local audio and play it reliably in the background.

Included: modular project, design system, Home/Library/Player, MediaStore index, Storage Access Framework open-file path, Media3 background session, controller, queue and adapter contracts.

Exit gate:
- [ ] GitHub Actions debug build, unit tests and lint pass.
- [ ] App installs and starts on Redmi Note 12 Pro 5G.
- [ ] Screen-off audio playback survives switching apps and normal navigation.
- [ ] Notification, lock-screen and headset/Bluetooth controls operate the same session.
- [ ] Audio focus and noisy-route behavior are correct.
- [ ] Local indexing does not copy, move or rename files.
- [ ] Open Document playback works without broad library permission.
- [ ] No critical crash or data-loss issue.

## Phase 2 — Cross-source background playback and source foundations

- Room schema and migrations for media identity, locations, collections, history and bookmarks.
- Incremental MediaStore sync and change observation; duplicate identity rules.
- Search/filter/sort and user collections.
- RSS podcast adapter with feed validation, episode metadata and playback position.
- Internet radio adapter with format/capability detection and reconnect behavior.
- Direct media URL resolver and browser-to-native handoff for accessible media.
- Source-by-source investigation and device tests for YouTube, Spotify, Facebook, Instagram and other requested apps: source-owned background continuation, exposed MediaSession controls, browser playback and supported native routes.
- A visible distinction between MOSAIC playback and external-app playback.
- Download queue for sources whose terms permit offline copies.
- Sleep timer, playback speed and queue persistence.
- Native video surface, orientation handling and picture-in-picture (tracked as P2-011).
- Accessibility/reduced-motion pass and robust empty/error/loading states.

Exit gate: local playback remains stable; RSS and open radio work; at least one accessible external media route is played through the shared Media3 service; every named provider has a documented test result and ownership label; no duplicate binary copies.

## Phase 3 — Broad ad blocking and source handoff

- Ad Shield is a first-class product surface with separate Browser and Device-wide controls.
- Implement a user-enabled Android VpnService local DNS/host filter for known ad/tracker domains across apps; explicit consent, persistent foreground notification, VPN conflict handling, safe stop/recovery and diagnostics are required.
- Build source-aware rule lists, allowlist/blocklist controls, rule provenance, blocked-domain counters and false-positive reporting.
- Do not blanket-exempt YouTube, Spotify, Facebook, Instagram or any other provider. Test rules against content delivery and preserve per-domain/per-app allowlisting.
- Keep the MOSAIC Browser request filter as a supporting layer, not the whole ad-blocking product.
- Browser engine boundary, tabs/session persistence and downloads are secondary to playback and Ad Shield.
- Share-to-MOSAIC capture inbox and outbound share/open intents.
- Page/media detection only for accessible, permitted content.
- Browser playback controls and native-player handoff only when the source and terms allow it.
- External active MediaSession discovery/control only with explicit Android notification-access permission.
- Clear native/browser/provider player ownership; never instantiate a second hidden player for the same stream.
- Site capability reporting, graceful fallback and normal web playback if handoff is unavailable.

Exit gate: DNS/VPN filtering passes cross-app allow/block and recovery tests; no accidental traffic blackhole; app compatibility and false positives are recorded; Browser filtering passes a separate site matrix; limitations are visible and no universal-blocking claim is made without evidence.

## Phase 4 — Official platform integrations

- Source-specific integration work for YouTube, Spotify, Facebook, Instagram and other high-priority providers based on the Phase 2 capability research.
- Evaluate official APIs/SDKs, source-owned Android background behavior, browser playback and accessible native media routes.
- Use Android MediaSession/official remote controls where exposed and user-authorized; clearly label external ownership.
- Do not claim a provider's audio is in MOSAIC's native player unless the shared Media3 service actually plays it.
- Authentication flows are explicit and source-scoped.
- Every integration has a documented capability matrix and policy review date.
- Unsupported capabilities remain explicitly labelled unavailable; the product goal remains in scope for continued technical investigation.

Exit gate: each named provider has a tested integration route or a documented, evidence-backed technical/platform limitation and a concrete next research item.

## Phase 5 — Local media intelligence

- On-device transcription and timestamps.
- Language detection and user-triggered translation.
- Chapters, summaries, highlights and citations to media timestamps.
- Search transcript content and concepts.
- Optional local model manager with device/storage checks.
- Processing queue, cancellation, retry, provenance and deletion.

Exit gate: original media remains unchanged; output is reproducible enough to audit; AI never invents transcript evidence; processing is controllable and can be disabled.

## Phase 6 — Automation, backup and ecosystem

- User-defined import rules and media processing rules.
- Exportable metadata and manifests.
- Manual local/external backup destinations and restore tests.
- Optional cross-device sync, self-hosted backend only if it materially improves the product.
- Android Auto / casting / other platform capability assessments.
- Adapter SDK and compatibility tests.

Exit gate: export/restore works, backup integrity is verified, sync is optional, and user data remains portable.

## Phase sequencing rule

Keep Phase 1 device acceptance visible, but do not let an unperformed physical-device test prevent independent engineering work that can be built and CI-verified. A phase is not called accepted until its full exit gate passes. New ideas enter `docs/TASKS.md`; they do not silently expand the active phase.
