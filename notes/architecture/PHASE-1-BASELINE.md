# Phase 1 architecture baseline

**Locked:** 2026-10-30  
**Source snapshot:** MOSAIC Phase 1 / 0.1.0

The durable architecture is the four-module native Android project described in `docs/ARCHITECTURE.md`. `:core:media` owns the provider-neutral model, source adapter capability contract and Media3 session boundary. `:core:library` owns read-only MediaStore discovery. `:core:design` owns the visual tokens. `:app` owns Compose screens, permission UX and composition root.

The app uses one MediaSessionService and one primary ExoPlayer. UI and Android system controls use MediaController. The local library is an index, not a copied media store. Each future source must declare real capabilities and resolve permitted items into the shared playback boundary where possible.

The first release deliberately proves local playback before RSS, radio, browser, provider services, AI or synchronization. Do not narrow the product vision; implement it in gated phases.
