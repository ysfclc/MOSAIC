# MOSAIC source capability matrix

This is a live engineering matrix, not a list of product exclusions. The user's target is background playback from as many sources as possible and ad/tracker blocking across as many apps and surfaces as technically possible. A target is not marked supported until verified on a real Android device.

| Source | MOSAIC-native playback | Background continuation | External control | Ad/filtering target | Current evidence/status |
|---|---|---|---|---|---|
| Local audio files | Yes | Yes, through MOSAIC Media3 service | MOSAIC MediaSession | N/A for local file playback | Implemented in source; device acceptance pending |
| RSS podcasts | Yes, when enclosure is accessible | Target: MOSAIC Media3 service | MOSAIC MediaSession | Block known tracker/ad hosts where requests are separable | Adapter not implemented |
| Internet radio | Yes, for supported stream formats | Target: MOSAIC Media3 service | MOSAIC MediaSession | Filter separable ad/tracker endpoints; preserve stream | Adapter not implemented |
| Direct audio/video URLs | Yes, when URI and format are accessible | Target: MOSAIC Media3 service | MOSAIC MediaSession | Filter separable third-party requests | Resolver/adapter not implemented |
| Browser media | Browser-owned or native handoff when accessible | Must be tested in both modes | Browser controls or MOSAIC MediaSession after handoff | Browser request filtering; source-aware rules | Basic WebView exists; media handoff not implemented |
| YouTube app/site | Official IFrame/API route is embedded YouTube playback, not a MOSAIC-owned audio stream | YouTube's current mobile-app help says background play requires Premium; official API policy explicitly prohibits allowing API playback to continue when the API service is minimized/closed | MOSAIC can control an exposed Android MediaSession only; that leaves playback owned by YouTube | YouTube API policy prohibits restricting ads in its API service; DNS filtering may block separable third-party hosts but cannot safely promise first-party/in-stream ad removal | Official policy/help reviewed 2026-11-05; no compliant native-audio extraction/background route demonstrated. Prioritize supported YouTube app session control and clearly label ownership |
| Spotify app/site | Official Android App Remote controls the installed Spotify app; Spotify retains the audio stream, network, caching and player | Spotify app owns background continuation and OS integration; MOSAIC can show a unified delegated control surface after explicit authorization | Generic Android MediaSession now; official App Remote is a stronger source-specific integration candidate | DNS may block separable ad/tracker hosts, but cannot guarantee in-stream ad removal or safely suppress Spotify ads inside its player | Official Android SDK docs reviewed 2026-11-05. This is delegated Spotify playback, not audio inside MOSAIC's Media3 queue; SDK is beta and commercial streaming integrations are restricted by Spotify developer policy |
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


## Evidence update — 2026-11-05

- **Spotify:** Spotify's official [Android SDK documentation](https://developer.spotify.com/documentation/android) describes App Remote as a way to read current playback state and issue playback commands to the Spotify app. Spotify itself performs playback, networking, caching and system integration. This gives MOSAIC a credible route to a polished Spotify control surface, but not to ingest Spotify audio into MOSAIC's own Media3 player. The SDK requires user authorization and is documented as beta. Spotify's developer getting-started page also states that streaming applications may not be commercial and Spotify content must remain unaltered: https://developer.spotify.com/documentation/android/tutorials/getting-started.
- **YouTube:** YouTube's [Android background-play help](https://support.google.com/youtube/answer/7437614) says background play is a Premium feature in the YouTube mobile apps. The [YouTube developer policy guide](https://developers.google.com/youtube/terms/developer-policies-guide) explicitly disallows API services from allowing YouTube playback to continue when minimized/closed and from restricting ads that would otherwise play. Therefore the compliant target is source-owned YouTube playback with exposed session controls where available, not extracting audio into MOSAIC or stripping YouTube ads.
- **Ad filtering:** DNS/host rules can still block known separable third-party ad/tracker hosts across apps, but they cannot reliably identify same-host or in-stream ads. No universal ad-skip claim is made.
- **Next implementation split:** (1) integrate and test Spotify App Remote as a clearly labelled delegated source; (2) improve YouTube MediaSession discovery/control and document account/background requirements; (3) build MOSAIC-native adapters for direct URLs, radio and RSS, which can genuinely enter the shared Media3 queue; (4) test Ad Shield compatibility separately for each route.
