import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class ProjectContractTests(unittest.TestCase):
    def test_architecture_declares_single_media_core(self):
        text = (ROOT / "docs/ARCHITECTURE.md").read_text(encoding="utf-8")
        self.assertIn("one app-wide `PlaybackService`", text)
        self.assertIn("MediaSourceAdapter", text)

    def test_locked_product_vision_has_both_primary_pillars(self):
        decisions = (ROOT / "docs/DECISIONS.md").read_text(encoding="utf-8")
        spec = (ROOT / "docs/PRODUCT_SPEC.md").read_text(encoding="utf-8")
        self.assertIn("cross-source background media playback/control", decisions)
        self.assertIn("broad ad/tracker blocking across apps and websites", decisions)
        for source in ("YouTube", "Spotify", "Facebook", "Instagram"):
            self.assertIn(source, spec)
        self.assertIn("no provider is automatically exempt", (ROOT / "docs/ARCHITECTURE.md").read_text(encoding="utf-8"))

    def test_workflow_is_present(self):
        self.assertTrue((ROOT / ".github/workflows/build-apk.yml").is_file())

    def test_workflow_builds_direct_edits_and_preserves_zip_import(self):
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn('"app/**"', workflow)
        self.assertIn('"core/**"', workflow)
        self.assertIn('direct-build:', workflow)
        self.assertIn("needs.preflight.outputs.mode == 'direct'", workflow)
        self.assertIn("needs.preflight.outputs.mode == 'import'", workflow)
        self.assertIn("Validate and safely extract source ZIP", workflow)

    def test_player_artwork_receives_revision_from_player_screen(self):
        source = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("PlayerArtwork(isPlaying = isPlaying, revision = revision)", source)
        self.assertIn("private fun PlayerArtwork(isPlaying: Boolean, revision: Int)", source)

    def test_workflow_installs_published_android_37_sdk_package(self):
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn("\"platforms;android-37.2\"", workflow)
        self.assertNotIn("\"platforms;android-37\"", workflow)

    def test_browser_declares_network_permission_and_provider_exemptions(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        policy = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.INTERNET", manifest)
        self.assertIn("WebView(context)", browser)
        self.assertIn("protectedProviderDomains", policy)

    def test_external_media_controls_require_explicit_notification_access(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        bridge = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/ExternalMediaSessionBridge.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", manifest)
        self.assertIn("enabled_notification_listeners", bridge)
        self.assertIn("getActiveSessions(listener)", bridge)

    def test_android_text_share_opens_in_mosaic_browser(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        activity = (ROOT / "app/src/main/java/com/ysfclc/mosaic/MainActivity.kt").read_text(encoding="utf-8")
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("android.intent.action.SEND", manifest)
        self.assertIn("android:mimeType", manifest)
        self.assertIn("text/plain", manifest)
        self.assertIn("Intent.EXTRA_TEXT", activity)
        self.assertIn("initialSharedText", app)
        self.assertIn("Destination.BROWSER", app)

    def test_playback_service_owns_audio_focus_and_wake_mode(self):
        source = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/PlaybackService.kt").read_text(encoding="utf-8")
        self.assertIn("setAudioAttributes(AudioAttributes.DEFAULT", source)
        self.assertIn("setWakeMode(C.WAKE_MODE_LOCAL)", source)

    def test_video_is_not_advertised_before_video_surface_exists(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("Phase 1 deliberately exposes audio playback only", readme)

if __name__ == "__main__":
    unittest.main()
