# MOSAIC provider capability matrix

This matrix describes what MOSAIC may own, what remains provider-owned, and what the user can expect. It is intentionally conservative; an adapter must report per-item/account/session capability instead of promising a provider-wide feature.

| Source | MOSAIC native player | Background | MOSAIC ad filtering | MOSAIC remote control | Notes |
|---|---|---|---|---|---|
| Local files | Yes | Yes | N/A | Yes, through MOSAIC MediaSession | Original files stay in place |
| RSS podcast enclosure | Yes, when directly accessible and permitted | Yes | N/A | Yes, through MOSAIC MediaSession | Feed metadata and episode URLs |
| Open internet radio | Yes, for supported stream formats | Yes | N/A | Yes, through MOSAIC MediaSession | Reconnect and ICY metadata |
| Permitted direct web media | Yes, after safe media resolution | Yes, when the source permits it | Third-party request filter on page only | Yes, through MOSAIC MediaSession | Never infer capability from URL alone |
| Ordinary web page | Browser-owned | Not automatically | Conservative host/request filter | Browser controls | No universal/cosmetic guarantee yet |
| YouTube official player/API | No extracted native audio stream | No background player through the API | No; preserve YouTube ads/player | Visible player controls; external app session only when exposed | Standard visible player and ads must remain intact |
| YouTube installed app | No raw stream access | Only as allowed by the user's YouTube account/app features | No | Android MediaSession controls where exposed and user-authorized | MOSAIC cannot unlock free-account background playback |
| Spotify installed app | No Spotify audio stream inside MOSAIC | Spotify app owns background playback | No | Official App Remote and/or Android MediaSession after authorization | Spotify app must be installed; it owns streaming/audio focus |
| Other media apps | No raw audio capture | Their own app decides | No | Android MediaSession where exposed, after notification-access opt-in | Metadata/actions only; no access to protected audio bytes |

## Two directions of the bridge

**Into MOSAIC**
- Accept shared text/URLs through Android ACTION_SEND and open them in Browser (implemented preview); file/metadata inbox capture is pending.
- Save captured references and provenance to the future durable inbox.
- Detect and control an external media session only with explicit Android notification access.
- Do not extract provider audio or silently download protected media.

**Out of MOSAIC**
- Share ordinary links and user-owned files through Android intents with temporary URI grants.
- Expose MOSAIC playback via the standard Android MediaSession for headset, lock-screen and system controls.
- Use official provider remote-control APIs only after user authorization.
- Do not pipe arbitrary local audio into another provider's proprietary player.

## Ad-control layers

1. Browser request filter (implemented preview): conservative host-level filtering for ordinary pages, with provider exemptions. It does not block every ad, first-party ad, in-stream ad, cosmetic placeholder or same-host ad.
2. Managed filter lists (planned): updateable rules, per-site allowlist, rule provenance and diagnostics.
3. Optional local DNS/VPN filter (parked): may block known ad/tracker domains across apps but cannot reliably remove ads delivered from the same host as content; consumes the Android VPN slot and can affect app compatibility.
4. Protected provider players: no ad stripping, player modification, audio extraction, DRM bypass or hidden background playback.

## WebView filter limitations

The current filter uses Android WebView request interception and a small bundled host set. Android documents that interception does not cover every request scheme (including javascript: and blob:) and that redirects are reported only for the initial URL. Therefore the first preview is a limited request-level filter, not a full EasyList-class engine or a universal ad blocker. The provider exemptions are intentional.
