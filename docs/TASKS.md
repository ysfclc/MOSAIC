# MOSAIC task board

Source of truth: this file. Update task status in the same commit as code changes.
Status values: DONE, IN PROGRESS, BLOCKED, TODO, PARKED.

## Phase 1 — Native foundation and local playback

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P1-001 | Establish modular Gradle project and package identity | DONE | :app, :core:media, :core:library, :core:design |
| P1-002 | Lock Media Core / adapter / capability contracts | DONE | docs/ARCHITECTURE.md, core/media |
| P1-003 | Create MOSAIC dark design system and motion language | DONE | Compose theme, ambient glow, animated artwork and transitions |
| P1-004 | Build Home / Library / Player navigation and states | DONE | MosaicApp.kt |
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
| P2-002 | Persist playback history, queue and resume positions | TODO | Process-death and reboot tests |
| P2-003 | Add incremental MediaStore sync and stable identity rules | TODO | No duplicate copies; preserve original files |
| P2-004 | Add collections, bookmarks, sorting and filters | TODO | Durable library UX |
| P2-005 | Add RSS podcast adapter | TODO | Feed validation, episode metadata and resume |
| P2-006 | Add internet radio adapter and reconnect policy | TODO | MIME/format detection and ICY metadata |
| P2-007 | Add offline/download manager for permitted sources | TODO | Terms/capability checked per source |
| P2-008 | Add sleep timer and playback speed controls | TODO | Shared Media3 session |
| P2-009 | Add Room migrations and process-death tests | TODO | Migration coverage |
| P2-010 | Add reduced-motion/accessibility settings and robust dynamic text states | TODO | System animation settings and readable layouts |
| P2-011 | Add a native video surface, orientation handling and picture-in-picture | TODO | Native sources only where supported |

## Phase 3 — Browser, ad control and two-way media bridge

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P3-001 | Browser shell, navigation and user-controllable ad/tracker shield | IN PROGRESS | Basic WebView navigation and conservative third-party host filter exist; live site/device matrix pending |
| P3-002 | Persistent tabs, bookmarks, downloads and browser session restore | TODO | Room-backed browser session state |
| P3-003 | Maintainable ad/tracker filter lists, allowlist and diagnostics | TODO | Source-specific rules, rule provenance, false-positive handling and user controls |
| P3-004 | Browser media detection and native background-playback handoff | TODO | Investigate accessible media and route supported sources into shared Media3 player |
| P3-005 | Share-to-MOSAIC inbox (links, files and metadata) | IN PROGRESS | Android text/plain SEND target opens shared URL/text in Browser; file inbox and durable capture still pending |
| P3-006 | MOSAIC-to-external share/open handoff | IN PROGRESS | Browser can share current page link; file/content handoff still pending |
| P3-007 | External active MediaSession discovery and remote controls | IN PROGRESS | Opt-in notification access and basic play/pause/next controls added; device/source verification pending; not native stream playback |
| P3-008 | Optional device-wide local VPN/DNS ad and tracker filter | TODO | Investigate Android VPNService, per-app coverage, explicit consent, VPN conflicts and same-domain limitations |
| P3-009 | Cross-source background playback capability matrix | TODO | Investigate YouTube, Spotify, Facebook, Instagram, browser media, RSS, radio and direct URLs; record native/delegated paths and evidence |
| P3-010 | Native playback adapters for accessible external media | TODO | Route supported browser/direct/RSS/radio media into the shared Media3 background player; no unsupported source claims |
| P3-011 | Cross-source background playback acceptance | TODO | Test each source with screen off, app switching, lock screen, notification, Bluetooth/headset and queue/control behavior |
| P3-012 | Cross-app ad-blocking feasibility and prototype | TODO | Evaluate Android VPNService/local DNS filtering, per-app coverage, consent, conflicts, false positives and source compatibility |
| P3-013 | Ad-blocking coverage and regression matrix | TODO | Test browser and device-wide filtering against named sources; record blocked ads, playback breakage and unavoidable first-party/in-stream cases |
| P3-014 | Source-specific YouTube/Spotify/social-media integration research | TODO | Compare official APIs/SDKs, browser playback, Android MediaSession and technically viable background paths; record current policy/technical evidence |

## Phase 4 — Official platform integrations

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P4-001 | YouTube source integration and background-playback/ad-filter feasibility | TODO | Research current APIs, browser/native paths, Android behavior and ad-filter compatibility; test rather than assume exclusions |
| P4-002 | Spotify source integration and background-playback/ad-filter feasibility | TODO | Research official SDKs, browser/native paths, Android behavior and ad-filter compatibility; distinguish remote control from native audio playback |
| P4-003 | Provider capability matrix and delegated-player UI | IN PROGRESS | Matrix documented; UI labels/implementation pending |
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
| GOV-001 | Add direct-edit CI trigger for source/documentation changes while preserving ZIP importer | TODO | Current workflow only watches incoming ZIP paths or manual dispatch; direct GitHub edits need build/test/lint/APK verification |
| GOV-002 | Reconcile latest unprocessed incoming ZIP | TODO | Run 41962171501 failed in preflight; inspect archive and safely integrate only after workflow path resolution is repaired |

## Product-critical vision — cross-source background playback and ad blocking

MOSAIC's founding purpose is to play media from other sources in the background and block ads as broadly as technically possible. This includes investigating YouTube, Spotify, Facebook, Instagram and other services. Do not replace this vision with clipboard capture, a bookmark inbox, or a browser-only filter. Sharing links/files is supporting functionality.

Current implementation is partial: native local audio playback exists; browser request filtering is conservative and limited to MOSAIC Browser; external Android MediaSession discovery/control exists but does not transfer audio into MOSAIC. Cross-source native background playback and device-wide ad filtering are not yet complete.

## Current blockers

- Yesterday's source upload was not integrated: upload commit `d054c992b2c4d6b6eeceae2626db5f772eed9ebf` triggered run 41926489353. ZIP validation/extraction and staging passed, but source verification failed because the active workflow requested Android SDK 36 while the verifier expected the published 37.2 package. Gradle build, APK build and integration commit were skipped.
- Direct edits to source/docs do not currently trigger the Android build; GOV-001 tracks a safe direct-edit CI path. The ZIP import workflow must remain supported.
- Physical Redmi Note 12 Pro 5G acceptance remains pending.
- Browser ad filtering and external-session controls are prototypes; broad device-wide ad blocking and cross-source background playback have not been accepted.
- Do not claim Phase 1 release acceptance or cross-source feature completion until the corresponding device/source acceptance tests pass.
