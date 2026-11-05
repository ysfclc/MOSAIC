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

## Phase 2 — Durable library and source foundations

- Room schema and migrations for media identity, locations, collections, history and bookmarks.
- Incremental MediaStore sync and change observation; duplicate identity rules.
- Search/filter/sort and user collections.
- RSS podcast adapter with feed validation, episode metadata and playback position.
- Internet radio adapter with format/capability detection and reconnect behavior.
- Download queue for sources whose terms permit offline copies.
- Sleep timer, playback speed and queue persistence.
- Native video surface, orientation handling and picture-in-picture (tracked as P2-011).
- Accessibility/reduced-motion pass and robust empty/error/loading states.

Exit gate: library survives process death/reboot; RSS and open radio work; no duplicate binary copies; playback history and resume are correct.

## Phase 3 — Browser, ad control and two-way media bridge

- Browser engine boundary; WebView baseline with replaceable implementation.
- Tabs, session persistence, downloads and bookmarks.
- MOSAIC Browser ad/tracker request filtering for ordinary pages; user toggle, visible blocked count, per-site exemptions and allowlist.
- Provider player pages/assets are exempt from MOSAIC filtering; no provider ad/DRM circumvention.
- Optional local DNS/VPN filtering is a separately gated feature, not implied by browser filtering.
- Share-to-MOSAIC capture inbox and outbound share/open intents.
- Page/media detection only for accessible, permitted content.
- Browser playback controls and native-player handoff only when the source and terms allow it.
- External active MediaSession discovery/control only with explicit Android notification-access permission.
- Clear native/browser/provider player ownership; never instantiate a second hidden player for the same stream.
- Site capability reporting, graceful fallback and normal web playback if handoff is unavailable.

Exit gate: ordinary-site filtering passes a site matrix; provider exemptions are verified; inbound/outbound handoff is tested; browser remains usable; no deceptive claim of universal ad removal or provider background playback.

## Phase 4 — Official platform integrations

- YouTube official APIs and embedded player for permitted search/metadata/visible playback.
- YouTube playback retains provider ads and standard controls; no background player, audio extraction or offline copy.
- Spotify Android App Remote for user-authorized metadata and remote playback commands to the installed Spotify app.
- Spotify remains the audio owner and handles its own background playback, audio focus and system integration.
- Authentication flows are explicit and source-scoped.
- Every integration has a documented capability matrix and policy review date.
- Unsupported native background, audio extraction, ad removal or offline functionality remains unsupported.

Exit gate: each adapter passes provider policy review, account/availability tests and reports limitations to the user.

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

Do not start Phase 2 while a Phase 1 exit item remains failed or unverified. New ideas enter `docs/TASKS.md` under Backlog; they do not silently expand the active phase.
