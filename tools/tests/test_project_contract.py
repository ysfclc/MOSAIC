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
        self.assertIn("no blanket provider exemption", (ROOT / "docs/ARCHITECTURE.md").read_text(encoding="utf-8").lower())

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

    def test_browser_declares_network_permission_and_filter_policy(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        policy = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.INTERNET", manifest)
        self.assertIn("WebView(context)", browser)
        self.assertIn("object AdBlockPolicy", policy)
        self.assertIn("shouldBlock", policy)

    def test_browser_filter_uses_source_and_referrer_context(self):
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        policy = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicy.kt").read_text(encoding="utf-8")
        tests = (ROOT / "app/src/test/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicyTest.kt").read_text(encoding="utf-8")
        self.assertIn("request.requestHeaders.entries", browser)
        self.assertIn('it.key.equals("Referer", ignoreCase = true)', browser)
        self.assertIn("topHostRef.get(), referrerUrl", browser)
        self.assertIn("val contextHosts = listOfNotNull", policy)
        self.assertIn("youtubePlayerCompatibilityDomains", policy)
        self.assertNotIn("isProtectedProviderHost", policy)
        self.assertIn("matchesDomainBoundariesAndNormalizesRequestHosts", tests)
        self.assertIn("doesNotTrustLookalikeYoutubeReferrersForCompatibility", tests)

    def test_ad_shield_validates_tunnel_lengths_and_system_dns_answers(self):
        codec = (ROOT / "app/src/main/java/com/ysfclc/mosaic/adshield/DnsPacketCodec.kt").read_text(encoding="utf-8")
        service = (ROOT / "app/src/main/java/com/ysfclc/mosaic/adshield/AdShieldVpnService.kt").read_text(encoding="utf-8")
        tests = (ROOT / "app/src/test/java/com/ysfclc/mosaic/adshield/DnsPacketCodecTest.kt").read_text(encoding="utf-8")
        self.assertIn("packetLength > packet.size", codec)
        self.assertIn("if (length < 0) break", service)
        self.assertIn("DnsPacketCodec.isMatchingResponse(query, answer)", service)
        self.assertIn("AdShieldState.setRunning(this, false)", service)
        self.assertLess(service.index("AdShieldState.setRunning(this, false)"), service.index("VpnService.prepare(this)"))
        self.assertIn("rejectsPacketLengthOutsideBackingBuffer", tests)
        self.assertIn("rejectsFragmentedAndInconsistentUdpPackets", tests)

    def test_external_media_controls_require_explicit_notification_access(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        bridge = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/ExternalMediaSessionBridge.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", manifest)
        self.assertIn("enabled_notification_listeners", bridge)
        self.assertIn("getActiveSessions(listener)", bridge)

    def test_external_sessions_are_controlled_from_primary_player(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        bridge = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/ExternalMediaSessionBridge.kt").read_text(encoding="utf-8")
        self.assertIn("ExternalPlaybackPanel", app)
        self.assertIn("PLAYING IN OTHER APPS", app)
        self.assertNotIn("ExternalMediaPanel", browser)
        self.assertIn("ACTION_SKIP_TO_PREVIOUS", bridge)
        self.assertIn("ACTION_SEEK_TO", bridge)

    def test_native_accessibility_ad_skip_is_opt_in_and_scoped(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        service = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdSkipAccessibilityService.kt").read_text(encoding="utf-8")
        config = (ROOT / "app/src/main/res/xml/ad_skip_accessibility_service.xml").read_text(encoding="utf-8")
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("BIND_ACCESSIBILITY_SERVICE", manifest)
        self.assertIn('android:packageNames="com.google.android.youtube"', config)
        self.assertIn("AD_SKIP_LABELS", service)
        self.assertIn('"skip ad"', service)
        self.assertIn("performAction(AccessibilityNodeInfo.ACTION_CLICK)", service)
        self.assertNotIn('"skip"', service)
        self.assertIn("AdSkipPanel", app)
        self.assertIn("ACTION_ACCESSIBILITY_SETTINGS", app)

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

    def test_browser_direct_media_handoff_is_https_only_and_native(self):
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("querySelectorAll('audio,video')", browser)
        self.assertIn("PlayableStreamAddress.normalize(candidate)", browser)
        self.assertIn("No direct HTTPS media URL found", browser)
        self.assertIn("onPlayMedia = { rawAddress, mediaTitle, mediaType ->", app)
        self.assertIn('playback.playUri(uri, mediaTitle.ifBlank { "Web media" }, mimeType)', app)

    def test_player_exposes_the_mosaic_owned_queue_and_selection(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("MOSAIC QUEUE", app)
        self.assertIn("controller.getMediaItemAt(index)", app)
        self.assertIn("controller?.seekTo(index, 0L)", app)
        self.assertIn("queuedItem.mediaMetadata.title", app)

    def test_player_speed_and_persistent_sleep_timer_are_wired(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        service = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/PlaybackService.kt").read_text(encoding="utf-8")
        timer = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/PlaybackSleepTimerStore.kt").read_text(encoding="utf-8")
        self.assertIn("PLAYBACK SPEED", app)
        self.assertIn("controller?.setPlaybackSpeed(speed)", app)
        self.assertIn("SLEEP TIMER", app)
        self.assertIn("PlaybackSleepTimerStore", app)
        self.assertIn("PlaybackSleepTimerPolicy.isExpired", service)
        self.assertIn("sleepTimerRunnable", service)
        self.assertIn("deadline_epoch_ms", timer)

    def test_media_identity_is_source_scoped_across_playback_and_resolution(self):
        factory = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/MediaItemFactory.kt").read_text(encoding="utf-8")
        identity = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/MediaIdentity.kt").read_text(encoding="utf-8")
        adapter = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/MediaStoreCatalog.kt").read_text(encoding="utf-8")
        connection = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/PlaybackConnection.kt").read_text(encoding="utf-8")
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("MediaIdentity.forRecord(this)", factory)
        self.assertIn("fun sourceItemId(stableId: String, expectedSourceId: String)", identity)
        self.assertIn("MediaIdentity.sourceItemId(mediaId, descriptor.id)", adapter)
        self.assertIn("mediaId: String? = null", connection)
        self.assertIn("entry.resumePositionMs, entry.mediaId", app)

    def test_podcast_feed_subscriptions_are_saved_on_device(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn('getSharedPreferences("mosaic_podcast_feeds"', app)
        self.assertIn('putStringSet("saved_urls"', app)
        self.assertIn("savedFeeds.forEach", app)
        self.assertIn("onRemoveFeed(savedFeed)", app)

    def test_ci_publishes_apk_as_private_prerelease_when_artifact_storage_is_full(self):
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn("gh release create", workflow)
        self.assertIn("--prerelease", workflow)
        self.assertIn("continue-on-error: true", workflow)

    def test_direct_web_video_has_a_native_player_surface(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("androidx.media3:media3-ui:1.11.1", gradle)
        self.assertIn("PlayerView(viewContext)", app)
        self.assertIn("@androidx.annotation.OptIn(markerClass = [UnstableApi::class])", app)
        self.assertIn("onVideoSizeChanged(videoSize: VideoSize)", app)
        self.assertIn("Local library indexing and file selection remain audio-only", readme)

if __name__ == "__main__":
    unittest.main()
