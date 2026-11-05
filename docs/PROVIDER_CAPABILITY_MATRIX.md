# MOSAIC source capability matrix

This is a live engineering matrix, not a list of product exclusions. The user's target is background playback from as many sources as possible and ad/tracker blocking across as many apps and surfaces as technically possible. A target is not marked supported until verified on a real Android device.

| Source | MOSAIC-native playback | Background continuation | External control | Ad/filtering target | Current evidence/status |
|---|---|---|---|---|---|
| Local audio files | Yes | Yes, through MOSAIC Media3 service | MOSAIC MediaSession | N/A for local file playback | Implemented in source; device acceptance pending |
| RSS podcasts | Yes, when enclosure is accessible | Target: MOSAIC Media3 service | MOSAIC MediaSession | Block known tracker/ad hosts where requests are separable | Adapter not implemented |
| Internet radio | Yes, for supported stream formats | Target: MOSAIC Media3 service | MOSAIC MediaSession | Filter separable ad/tracker endpoints; preserve stream | Adapter not implemented |
| Direct audio/video URLs | Yes, when URI and format are accessible | Target: MOSAIC Media3 service | MOSAIC MediaSession | Filter separable third-party requests | Resolver/adapter not implemented |
| Browser media | Browser-owned or native handoff when accessible | Must be tested in both modes | Browser controls or MOSAIC MediaSession after handoff | Browser request filtering; source-aware rules | Basic WebView exists; media handoff not implemented |
| YouTube app/site | Investigate every technically viable path; native MOSAIC stream is not assumed | Explicit target; source-owned continuation and accessible alternate paths must be tested | Android MediaSession when exposed; other supported controls researched | Block known separable ad/tracker domains without breaking video delivery; measure first-party/in-stream limits | No demonstrated background continuation through MOSAIC; current browser policy exempts provider domains |
| Spotify app/site | Investigate official and technically viable paths; protected stream access is not assumed | Explicit target; test app-owned session and other supported paths | Android MediaSession / official App Remote where available and authorized | Block known separable ad/tracker domains; do not break playback | Current code only offers generic Android session discovery/control |
| Facebook app/site | Investigate accessible media and source-owned playback paths | Explicit target; test videos/reels and background behavior separately | Android MediaSession if exposed | Device-wide domain filter plus browser rules; measure first-party delivery | Not implemented or verified |
| Instagram app/site | Investigate accessible media and source-owned playback paths | Explicit target; test reels/video and background behavior separately | Android MediaSession if exposed | Device-wide domain filter plus browser rules; measure first-party delivery | Not implemented or verified |
| Other media apps/sites | Discover by capability, not hard-coded assumptions | Target where technically feasible | Android MediaSession when exposed | Device-wide filter plus source-specific compatibility | Not implemented or verified |

## Playback modes (must never be conflated)

- **MOSAIC PLAYER:** MOSAIC has a usable media locator and plays the item through its shared Media3 service. It owns the playback queue and app session.
- **BROWSER PLAYER:** the media is played inside MOSAIC's browser surface. Background behavior must be tested; merely loading a page is not support.
- **EXTERNAL PLAYER:** the original app/site owns the media session. MOSAIC can only show/control commands that Android and that source expose. This is useful but not equivalent to MOSAIC receiving the audio.
- **LINK ONLY:** MOSAIC can open or save a reference but has not demonstrated playback.

Each source can expose more than one mode. Capability is per item, account, app version and session where relevant.

## Ad-control layers

1. **Browser filtering:** request-level rules for content loaded inside MOSAIC. Current implementation is a small conservative host list and is not a full filter engine.
2. **Device-wide filtering:** optional, explicit-consent Android `VpnService` local DNS/host filtering. This is the main cross-app ad-blocking route to investigate and implement.
3. **Source-aware rules:** known ad/tracker endpoints may be blocked even when a first-party content domain is allowed. No blanket exemption is granted merely because a provider is named.
4. **Measured limits:** DNS/host filtering cannot reliably distinguish ads from content when both share a hostname or are inserted into the media stream. Record these as tested limitations, not as reasons to abandon broader filtering.

## Acceptance evidence required

For every named source, record app/site version, account tier where relevant, Android version/device, route used, playback owner, foreground behavior, screen-off continuation, app-switch behavior, lock-screen/notification/headset controls, seek/queue support, ad requests blocked, content breakage, false positives and known gaps. No capability is DONE based only on a successful page load or metadata display.
