#!/usr/bin/env python3
"""Fast structural verifier for the MOSAIC source snapshot."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/ysfclc/mosaic/MainActivity.kt",
    "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt",
    "core/media/build.gradle.kts",
    "core/library/build.gradle.kts",
    "core/design/build.gradle.kts",
    "docs/ARCHITECTURE.md",
    "docs/PHASES.md",
    "docs/TASKS.md",
    "PROJECT_STATE.md",
    "tools/mosaic-debug-signing-key.b64",
]

def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)

missing = [p for p in REQUIRED if not (ROOT / p).is_file()]
if missing:
    fail("required project files missing: " + ", ".join(missing))

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
service = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/PlaybackService.kt").read_text(encoding="utf-8")
contract = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/MediaSource.kt").read_text(encoding="utf-8")
browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
external_bridge = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/ExternalMediaSessionBridge.kt").read_text(encoding="utf-8")
ad_shield_service = (ROOT / "app/src/main/java/com/ysfclc/mosaic/adshield/AdShieldVpnService.kt").read_text(encoding="utf-8")
workflow = ROOT / ".github/workflows/build-apk.yml"
checks = {
    "media playback foreground permission": "FOREGROUND_SERVICE_MEDIA_PLAYBACK" in manifest,
    "single MediaSessionService declaration": manifest.count("androidx.media3.session.MediaSessionService") == 1,
    "mediaPlayback foreground service type": 'android:foregroundServiceType="mediaPlayback"' in manifest,
    "screen-off wake lock permission": "android.permission.WAKE_LOCK" in manifest,
    "local playback wake mode": "setWakeMode(C.WAKE_MODE_LOCAL)" in service,
    "automatic audio focus": "setAudioAttributes(AudioAttributes.DEFAULT" in service,
    "service is a MediaSessionService": "MediaSessionService" in service,
    "source adapter contract": "interface MediaSourceAdapter" in contract,
    "explicit source capabilities": "class SourceCapabilities" in contract,
    "current architecture notes": (ROOT / "docs/ARCHITECTURE.md").is_file(),
    "workflow template included": workflow.is_file(),
    "stable debug signing is configured in CI": "MOSAIC_DEBUG_KEYSTORE" in workflow.read_text(encoding="utf-8") and "700b857678b343aed93f7a7280e7d777df73c3f5f5e5024d94c119a4578079d0" in workflow.read_text(encoding="utf-8"),
    "CI build version code is incremented": "mosaicVersionCode" in (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8"),
    "browser has network permission for WebView": "android.permission.INTERNET" in manifest and "WebView(context)" in browser,
    "browser ad-filter policy is present": "object AdBlockPolicy" in (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicy.kt").read_text(encoding="utf-8"),
    "external media controls require notification listener permission": "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" in manifest,
    "external sessions are queried through Android MediaSessionManager": "getActiveSessions(listener)" in external_bridge,
    "Android share target is registered": "android.intent.action.SEND" in manifest and "android:mimeType=" in manifest,
    "Ad Shield fails over safely when underlying-network socket binding fails": "runCatching { network.bindSocket(socket) }.isFailure" in ad_shield_service,
    "Ad Shield pins upstream UDP replies to the selected resolver": "socket.connect(resolver, DNS_PORT)" in ad_shield_service,
    "Ad Shield refreshes underlying network selection including disconnects": "setUnderlyingNetworks(networks.toTypedArray())" in ad_shield_service,
    "shared URLs are forwarded into the Browser": "initialSharedText" in (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8") and "onInitialUrlConsumed" in browser,
    "Phase 1 picker is audio-only until video UI exists": 'documentLauncher.launch(arrayOf("audio/*"))' in (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8"),
}
failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(f"{'PASS' if passed else 'FAIL'}: {name}")
if failed:
    fail("one or more architecture invariants failed")

# Guard the source tree against accidental claims that future integrations are already implemented.
source_text = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in ROOT.rglob("*.kt"))
if re.search(r"TODO\(phase1\)", source_text, flags=re.IGNORECASE):
    fail("found an unresolved Phase 1 placeholder marker")
print("MOSAIC source structure verified. This is not a substitute for Gradle or device testing.")
