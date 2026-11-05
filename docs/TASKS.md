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
| P3-001 | Browser shell, navigation and conservative request-level ad shield | IN PROGRESS | Browser destination and host filter compile; live site/device matrix pending |
| P3-002 | Persistent tabs, bookmarks, downloads and browser session restore | TODO | Room-backed browser session state |
| P3-003 | Maintainable filter-list updates, per-site allowlist and filter diagnostics | TODO | Provider exemptions and rule provenance required |
| P3-004 | Permitted page-media detection and browser-to-MOSAIC player handoff | TODO | Direct media only when technically accessible and allowed |
| P3-005 | Share-to-MOSAIC inbox (links, files and metadata) | IN PROGRESS | Android text/plain SEND target opens shared URL/text in Browser; file inbox and durable capture still pending |
| P3-006 | MOSAIC-to-external share/open handoff | IN PROGRESS | Browser can share current page link; file/content handoff still pending |
| P3-007 | External active MediaSession discovery and remote controls | IN PROGRESS | Notification-listener opt-in UI and controls added; device verification pending |
| P3-008 | Optional device-wide local DNS/VPN ad/tracker filter | PARKED | Separate consent, VPN conflict handling and same-domain limitations |
| P3-009 | Explicit copy/paste capture entry point | TODO | User can paste copied plain text/URLs into the same preview-and-capture flow as Android Share; clipboard is read only after an explicit paste action; no background clipboard polling |
| P3-010 | Durable unified capture inbox | TODO | Pasted/shared URLs, text and supported files persist with source, capture time, content type and processing state; items survive process death and can be opened, copied or shared back out |
| P3-011 | Capture round-trip acceptance | TODO | Verify copy in another app → paste into MOSAIC → save → reopen → copy/share/open back in another app; also verify Android Share follows the same inbox path |

## Phase 4 — Official platform integrations

| ID | Task | Status | Verification / note |
|---|---|---|---|
| P4-001 | YouTube official search/metadata and standard visible embedded playback | TODO | Preserve ads/player UI; no background player or audio extraction |
| P4-002 | Spotify Android App Remote integration | TODO | Installed Spotify app + explicit authorization; provider owns audio/background |
| P4-003 | Provider capability matrix and delegated-player UI | IN PROGRESS | Matrix documented; UI labels/implementation pending |
| P4-004 | Provider policy and integration regression review | TODO | Re-check official policies before each release |

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

## Founding product requirement — copy/paste and two-way capture

MOSAIC originated from the need to move useful content between apps without losing it: copy a URL/text or share content into MOSAIC, keep it for later, and then copy/open/share it back out. This is a core product workflow, not merely a browser convenience. See the Origin and founding problem section in PRODUCT_SPEC.md. Paste must be explicit and user-initiated; do not implement background clipboard monitoring or conflate MOSAIC with VaultOS.

## Current blockers

- Latest incoming ZIP commit `0b65d868` was not imported: run 41962171501 failed in preflight before extraction. GOV-002 tracks recovery.
- Direct edits to source/docs do not currently trigger the Android build; GOV-001 tracks a safe direct-edit CI path. The ZIP import workflow must remain supported.
- Physical Redmi Note 12 Pro 5G acceptance remains pending.
- The browser preview and external-session controls are implementation previews; live site compatibility and device behaviour are not yet accepted.
- Do not claim Phase 1 release acceptance or begin a Phase 2 release until the Phase 1 device gate passes. Provider integrations remain gated by their official capabilities and policies.
