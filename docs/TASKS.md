# MOSAIC task board

Source of truth: this file. Update task status in the same commit as code changes.
Status values: DONE, IN PROGRESS, BLOCKED, TODO, PARKED.

## Product direction and experience reset

The complete north star is [docs/MOSAIC_PRODUCT_VISION.md](MOSAIC_PRODUCT_VISION.md). MOSAIC is a personal media OS, not a generic local player with extra panels. The source-first Home/Explore and external-session Player/mini-player correction passed CI run 42261337270; physical device acceptance remains open.

| ID | Task | Status | Verification / note |
|---|---|---|---|
| MOS-001 | Recover and lock the original personal media OS brainstorm | DONE | Canonical vision in docs/MOSAIC_PRODUCT_VISION.md; includes source scope, playback continuity, Ad Shield, unified Library, Home/Explore/Player, local-first principles, and future Intelligence/Automation |
| MOS-002 | Replace phase/status-led Home with source-first media command centre and add Explore | IN PROGRESS | Home/Explore redesign passed CI run 42261337270 (build #174); Redmi usability acceptance remains pending |
| MOS-003 | Make requested source entry points open installed apps with safe web fallback | IN PROGRESS | YouTube, Spotify, Instagram and Facebook intents wired in MosaicApp.kt; CI run 42261337270 passed; physical app launch and return-to-MOSAIC session behavior pending |
| MOS-004 | Make Player the coherent home for MOSAIC-owned and Android-exposed external sessions | IN PROGRESS | External sessions appear at the top of Player and in a dedicated mini-player; CI run 42261337270 passed; Redmi source-session acceptance and queue boundaries remain open |
| MOS-005 | Complete premium design language and product-grade states across all destinations | IN PROGRESS | Existing dark palette and motion are a base only; continue hierarchy, artwork, empty/loading/error states, accessibility and reduced-motion work |

## Phase 1 — Native foundation and local playback

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P1-001 | Establish modular Gradle project and package identity | DONE | :app, :core:media, :core:library, :core:design |
| P1-002 | Lock Media Core / adapter / capability contracts | DONE | docs/ARCHITECTURE.md, core/media |
| P1-003 | Create MOSAIC dark design system and motion language | IN PROGRESS | Palette and motion foundation exists; full product-grade visual system and screen consistency remain open |
| P1-004 | Build source-first Home / Explore / Library / Player navigation and states | IN PROGRESS | Home and Explore now foreground source journeys; CI/device verification and remaining screen integration pending |
| P1-005 | Implement read-only MediaStore audio catalog | DONE | MediaStoreCatalog; permission failure handled |
| P1-006 | Implement Storage Access Framework open-file playback | DONE | OpenDocument path; persist URI read permission where offered |
| P1-007 | Implement Media3 foreground playback service and session | DONE | PlaybackService, manifest service declaration |
| P1-008 | Connect UI via MediaController and expose queue controls | DONE | PlaybackConnection, player and mini-player |
| P1-009 | Add capability tests for source contract | DONE | SourceCapabilitiesTest |
| P1-010 | Add source verifier and ZIP-import CI workflow | DONE | ZIP import, source verifier, Python tests, Gradle build, lint and APK verification passed in CI |
| P1-011 | Generate official Gradle wrapper + distribution checksum | DONE | Generated in CI; Gradle 9.3.1 distribution SHA-256 is pinned |
| P1-012 | Run GitHub Actions debug build, lint and unit tests | DONE | Successful workflow run: https://github.com/ysfclc/MOSAIC/actions/runs/41957296181 |
| P1-013 | Install and validate on Redmi Note 12 Pro 5G | TODO | Screen-off, lock screen, Bluetooth, audio focus |
| P1-014 | Verify startup, permissions, SAF and local indexing on device | TODO | Physical device acceptance |
| P1-015 | Align CI with the published Android 37 SDK platform package | DONE | Workflow installs platforms;android-37.2; compileSdk remains 37 |
| P1-016 | Propagate playback revision into artwork composable | DONE | PlayerScreen forwards revision to PlayerArtwork; regression test added |

## Phase 2 — Durable library and source foundations

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P2-001 | Design Room entities, DAO and migration policy | TODO | Media identity, locations, collections, history and bookmarks |
| P2-002 | Persist playback history, queue and resume positions | IN PROGRESS | First slice stores up to 50 local/native playback references in app-private preferences, deduplicates by media identity, omits query/fragment/userinfo URLs to avoid persisting signed tokens, and exposes one-tap Recent playback on Home. Codec tests cover Unicode, optional fields, malformed rows, deduplication and bounds. Durable queue/resume positions, Library integration and process-death/device acceptance remain open |
| P2-003 | Add incremental MediaStore sync and stable identity rules | TODO | No duplicate copies; preserve original files |
| P2-004 | Add collections, bookmarks, sorting and filters | TODO | Durable library UX |
| P2-005 | Add RSS podcast adapter | IN PROGRESS | Player currently loads bounded HTTPS RSS 2.0 / Atom feeds, parses accessible HTTPS enclosures, queues episodes in Media3 and saves feed URLs locally; a dedicated source adapter, durable episode identity/resume and device acceptance remain open |
| P2-006 | Add internet radio adapter and reconnect policy | IN PROGRESS | Player accepts validated HTTPS stream URLs and Media3 HLS is included; station discovery, ICY metadata, reconnect policy and device acceptance remain open |
| P2-007 | Add offline/download manager for permitted sources | TODO | Terms/capability checked per source |
| P2-008 | Add sleep timer and playback speed controls | TODO | Shared Media3 session |
| P2-009 | Add Room migrations and process-death tests | TODO | Migration coverage |
| P2-010 | Add reduced-motion/accessibility settings and robust dynamic text states | TODO | System animation settings and readable layouts |
| P2-011 | Add a native video surface, orientation handling and picture-in-picture | IN PROGRESS | A Media3 PlayerView surface exists for supported direct web video; orientation handling, picture-in-picture and device acceptance remain open |

## Phase 3 — Browser, ad control and two-way media bridge

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P3-001 | Browser shell, navigation and user-controllable ad/tracker shield | IN PROGRESS | Basic WebView navigation and conservative third-party host filter exist; live site/device matrix pending |
| P3-002 | Persistent tabs, bookmarks, downloads and browser session restore | TODO | Room-backed browser session state |
| P3-003 | Maintainable ad/tracker filter lists, allowlist and diagnostics | TODO | Source-specific rules, rule provenance, false-positive handling and user controls |
| P3-004 | Browser media detection and native background-playback handoff | IN PROGRESS | Browser detects visible direct HTTPS HTML5 audio/video sources and can hand the validated URL to the shared Media3 player; DRM, MSE, blob, cookie/header-dependent streams and physical-device behavior remain unverified |
| P3-005 | Share-to-MOSAIC inbox (links, files and metadata) | IN PROGRESS | Android text/plain SEND target opens shared URL/text in Browser; file inbox and durable capture still pending |
| P3-006 | MOSAIC-to-external share/open handoff | IN PROGRESS | Browser can share current page link; file/content handoff still pending |
| P3-007 | Move external MediaSession controls into the primary Player | IN PROGRESS | Moved from Browser into Player; app labels, play/pause, previous/next and seek appear only when the source advertises support. CI build passed in run 42034925797; Redmi real-app verification remains pending. External control is not native stream playback |
| P3-008 | Device-wide DNS-only VPN Ad Shield and user controls | IN PROGRESS | First implementation is CI-verified in run 42051683244. Added an in-app DNS self-test for normal resolution, blocked-host interception and allowlist override, plus docs/AD_SHIELD_DEVICE_ACCEPTANCE.md. Source verification, unit tests, lint, APK assembly and signing verification passed in run 42078014230. Follow-up hardening now requires successful underlying-network socket binding, pins UDP replies to the selected resolver, validates DNS transaction ID/opcode/question/response flag, and clears stale underlying-network selections; all source verification, unit tests, lint, APK assembly, checksum and signing checks passed in run 42094128331. Additional hardening now stops the packet loop on tunnel EOF, validates Android system-resolver replies against the original transaction/question, and rejects packetLength values outside the backing buffer; tests added but not yet executed. Redmi checks for upstream loss/recovery, unrelated-app traffic, VPN stop/revoke, notification and lock-screen behavior remain pending |
| P3-009 | Cross-source background playback capability matrix | IN PROGRESS | Matrix exists; update it to distinguish native HTTPS/HLS/RSS and direct-browser handoff from delegated external sessions, and record per-source Redmi evidence before marking support |
| P3-010 | Native playback adapters for accessible external media | IN PROGRESS | Player accepts validated HTTPS audio/HLS URLs; bounded HTTPS RSS 2.0 / Atom feeds are parsed securely and episode enclosures enter MOSAIC's Media3 queue. Feed URLs persist locally. Browser detects direct HTTPS HTML5 audio/video URLs and hands them to the native player; a Media3 PlayerView renders video frames. DRM, MSE/blob and protected streams remain unsupported. Integrated browser/video slice plus Media3 lint opt-in regression check passed source verification, Python contract tests, Android unit tests, lint, APK assembly and stable signer verification in run 42162337225; private prerelease #157 published. Redmi source/screen-off acceptance remains pending; HTTP-only stations remain excluded |
| P3-011 | Cross-source background playback acceptance | TODO | Test each source with screen off, app switching, lock screen, notification, Bluetooth/headset and queue/control behavior |
| P3-012 | DNS packet parser and IPv4 UDP response codec | DONE | Unit tests cover IPv4 UDP DNS question parsing, malformed/non-DNS packets, exact suffix matching, allowlist overrides, NXDOMAIN response content and IPv4 header checksum; passed in run 42051683244 |
| P3-013 | Ad Shield coverage and regression matrix | IN PROGRESS | Basic rule/allowlist unit tests exist; still test real DNS traffic, named apps, false positives, playback breakage, DoH, IPv6 and VPN stop/revoke on Redmi |
| P3-014 | Stable APK signing and upgrade-in-place delivery | DONE | Run 42055276990 passed source verification, unit tests, lint and APK build. CI verified the persistent certificate fingerprint and set versionCode to run number 80. Future CI APKs now share the same signer; physical in-place install on Redmi remains to be confirmed. One-time migration from older ephemeral signatures may require one uninstall. |
| P3-017 | Source-specific YouTube/Spotify/social-media integration research | IN PROGRESS | Official YouTube and Spotify Android routes researched and recorded in docs/PROVIDER_CAPABILITY_MATRIX.md. Spotify App Remote is delegated control; YouTube API policy prohibits API-based background continuation and ad restriction. Next: implement Spotify delegated control and verify YouTube external-session controls; continue source/device evidence |
| P3-018 | Android-native cross-app media controls and visible ad-skip helper | IN PROGRESS | Android MediaSession/NotificationListener bridge and opt-in YouTube-only visible ad-skip helper passed source verification, Python tests, Android unit tests, lint, APK build, checksum and stable signing in CI run 42110725075. Redmi real-app checks remain pending. This does not extract audio, mute ads or force background continuation |
| P3-015 | Source-aware filtering and provider compatibility rules | IN PROGRESS | Removed whole-page YouTube/Spotify exemptions; known third-party ad/tracker domains are blocked on provider pages. Google ad infrastructure is blocked by default with a narrow YouTube-page compatibility exception; unit tests added, execution and Redmi compatibility acceptance pending |
| P3-016 | VPN conflict, Private DNS and service recovery handling | TODO | Detect an existing VPN and strict Private DNS, explain coverage limits, show persistent status/Stop action and restore normal networking on stop/revoke |

## Phase 4 — Official platform integrations

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P4-001 | YouTube source integration and background-playback/ad-filter feasibility | TODO | Research current APIs, browser/native paths, Android behavior and ad-filter compatibility; test rather than assume exclusions |
| P4-002 | Spotify source integration and background-playback/ad-filter feasibility | TODO | Research official SDKs, browser/native paths, Android behavior and ad-filter compatibility; distinguish remote control from native audio playback |
| P4-003 | Provider capability matrix and delegated-player UI | IN PROGRESS | Generic Android MediaSession controls and clear external-session Player labels exist; source-specific integrations and real-app/Redmi acceptance remain pending |
| P4-004 | Source policy, technical feasibility and integration regression review | TODO | Re-check current platform/provider terms and Android capabilities without preemptively narrowing the product goal |

## Phase 5 — Local intelligence

| ID | Task | Status |
|---|---|---|
| P5-001 | On-device transcription benchmark on target device | TODO |
| P5-002 | Timestamped transcript schema and pipeline | TODO |
| P5-003 | Language detection / translation workflow | TODO |
| P5-004 | Chapters, summaries and evidence-backed highlights | TODO |
| P5-005 | Semantic search and processing job queue | TODO |

## Phase 6 — Automation / backup / ecosystem

| ID | Task | Status |
|---|---|---|
| P6-001 | Rule engine for imports and processing | TODO |
| P6-002 | Export manifest and portable library format | TODO |
| P6-003 | Manual external backup and restore verification | TODO |
| P6-004 | Optional cross-device sync / self-host feasibility | TODO |
| P6-005 | Android Auto, casting and adapter SDK assessments | TODO |

## Repository maintenance and session continuity

| ID | Task | Status | Verification / note |
|---|---|---|---|
| GOV-001 | Add direct-edit CI trigger for source/documentation changes while preserving ZIP importer | DONE | Direct source/doc pushes run source verification, Python tests, Android unit tests, lint and debug APK build; ZIP import path remains separate. Verified run 42033924158 |
| GOV-002 | Reconcile latest unprocessed incoming ZIP | TODO | Run 41962171501 failed in preflight; inspect archive and safely integrate only after workflow path resolution is repaired |

## Locked product vision — ADR-014 and ADR-015

MOSAIC has two primary pillars: **cross-source background media playback/control** and **broad ad/tracker blocking across apps and websites**. Requested sources include YouTube, Spotify, Facebook, Instagram and other apps/sites, plus podcasts, radio, direct streams and local media. The browser is one supporting route, not the product. Copy/paste and clipboard history are not the founding purpose.

Current implementation remains a foundation, but it is broader than local audio: the Player accepts validated HTTPS streams (with HLS support), securely loads RSS 2.0 / Atom podcast feeds and queues accessible HTTPS enclosures, and Browser can hand directly exposed HTTPS HTML5 audio/video to the shared Media3 player. The Player also exposes delegated Android MediaSession controls, and Ad Shield has an opt-in DNS-only local VPN implementation. These capabilities are not yet accepted on the Redmi. Native playback from named third-party apps remains unproven, and device-wide DNS filtering is limited and not yet device-accepted. The browser filter still contains legacy blanket provider exemptions that conflict with ADR-014/015; P3-015 remains open.

## Current blockers

- Direct-edit CI is now active and verified in run 42033924158. The ZIP importer remains a separate path with archive validation and safe extraction.
- Keep the incoming ZIP importer and its path/secret checks; do not let it overwrite active workflows, notes or the handoff ledger.
- Physical Redmi Note 12 Pro 5G acceptance remains pending for Player and Ad Shield.
- Browser filtering and external-session controls are prototypes; native background playback from named external sources remains unproven. The first cross-app DNS Ad Shield slice is implemented and CI-verified, but physical-device and named-app acceptance are still pending.
- Do not claim device/source acceptance until the corresponding tests have actually passed.
- CI APKs must use the persistent MOSAIC development signing certificate and a higher versionCode. Never ask the user to uninstall between ordinary updates. The old ephemeral-signature APK may require a one-time migration uninstall.
