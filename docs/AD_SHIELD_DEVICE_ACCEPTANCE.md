# Ad Shield — Redmi device acceptance

**Target:** Redmi Note 12 Pro 5G  
**Status:** NOT YET DEVICE-VERIFIED  
**Build requirement:** install a MOSAIC APK signed with the persistent development key. Do not use an older ephemeral-key artifact.

This checklist separates automated code/build evidence from behavior that can only be confirmed on the physical phone. A passing CI run is not device acceptance.

## Before testing

1. Open MOSAIC → Ad Shield.
2. Confirm Android has granted MOSAIC VPN permission.
3. Confirm notification permission is granted on Android 13+.
4. Disconnect any other VPN. Keep the current network connected.
5. Enable **Device-wide protection** and accept Android's VPN prompt.
6. Confirm the Ad Shield screen says **PROTECTION ACTIVE** and Android shows the persistent MOSAIC notification with a Stop action.

Do not disable Private DNS globally as a routine workaround. On Android 9, MOSAIC must fail closed when Private DNS is enabled. On Android 10+, the system resolver path is used to preserve Android's DNS configuration.

## Automated in-app DNS self-test

With protection active, press **Run DNS self-test**.

- Normal DNS: `example.com` must resolve to an address.
- Block test: MOSAIC sends a unique hostname under `doubleclick.net`. If that suffix is not allowlisted, the blocked counter and last-blocked-domain field must confirm interception.
- Allowlist test: add `doubleclick.net` to the allowlist and run the self-test again. The same unique test hostname must not increment the blocked counter; the UI must report that the allowlist override was respected. Remove the test allowlist entry afterwards.

A pass only proves these DNS probes traversed the expected path during that run. It does not prove that every app uses system DNS or that every ad is blockable.

## Manual device checks

| Check | Procedure | Expected result |
|---|---|---|
| Ordinary DNS | Open a normal website or app while protection is active | Name resolution and ordinary traffic continue |
| Known blocked hostname | Run the in-app self-test without a matching allowlist entry | Block counter increases and exact probe hostname is shown |
| Allowlist override | Allowlist `doubleclick.net`, then run the self-test | Probe is forwarded and block counter does not increase for it |
| Upstream timeout / no network | With protection active, temporarily disable Wi-Fi/mobile data and run a fresh DNS lookup; restore connectivity afterwards | UI reports upstream DNS unavailable/timeout; no crash or silent false success; DNS recovers when connectivity returns |
| Unrelated app traffic | Use an unrelated app that loads fresh network content | Ordinary non-DNS traffic remains available; DNS-only mode does not tunnel all app payloads |
| Stop from app | Tap Stop protection | VPN indicator/notification disappears and normal DNS works |
| Stop from notification | Re-enable, then tap Stop in the persistent notification | Same clean shutdown and normal DNS recovery |
| VPN revocation | Re-enable, then revoke MOSAIC's VPN permission from Android VPN settings | Service stops, status becomes inactive, normal network access returns |
| Foreground notification | Leave Ad Shield active and inspect notification shade | Persistent MOSAIC notification remains visible and offers Stop |
| Screen-off / lock screen | Leave protection active, lock the phone for several minutes, unlock | Service remains active; DNS works; notification remains controllable |

Record Android version, network type, result, and any displayed service error for each failed or surprising check.

## Known limitations (not acceptance failures)

- This version intercepts IPv4 UDP DNS only.
- Apps using DoH, custom DNS, IPv6 DNS paths, or same-domain/in-stream ad delivery may bypass filtering.
- DNS blocking cannot reliably distinguish first-party ads from content served by the same hostname.
- Android permits one active VPN at a time; enabling MOSAIC may replace another VPN only after the user confirms.
- Android 9 with Private DNS enabled is intentionally blocked from starting rather than downgrading DNS privacy.

## Sign-off

Do not mark P3-008 DONE until the automated self-test and all applicable manual device checks above have been performed on the Redmi and their outcomes recorded in the project handoff.
