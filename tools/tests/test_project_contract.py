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

    def test_workflow_validates_direct_edits_and_keeps_zip_import_paused(self):
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn('"app/**"', workflow)
        self.assertIn('"core/**"', workflow)
        self.assertIn('direct-build:', workflow)
        self.assertIn("needs.preflight.outputs.mode == 'direct'", workflow)
        import_job = workflow.split("  build:", 1)[1].split("  direct-build:", 1)[0]
        self.assertIn("if: false", import_job)
        self.assertIn("Validate and safely extract source ZIP", import_job)

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

    def test_browser_webview_has_explicit_navigation_and_content_security(self):
        browser = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/BrowserScreen.kt").read_text(encoding="utf-8")
        policy = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicy.kt").read_text(encoding="utf-8")
        tests = (ROOT / "app/src/test/java/com/ysfclc/mosaic/ui/browser/AdBlockPolicyTest.kt").read_text(encoding="utf-8")
        self.assertIn("WebSettings.MIXED_CONTENT_NEVER_ALLOW", browser)
        self.assertIn("settings.safeBrowsingEnabled = true", browser)
        self.assertIn("settings.javaScriptCanOpenWindowsAutomatically = false", browser)
        self.assertIn("settings.allowFileAccess = false", browser)
        self.assertIn("settings.allowContentAccess = false", browser)
        self.assertIn("settings.allowFileAccessFromFileURLs = false", browser)
        self.assertIn("settings.allowUniversalAccessFromFileURLs = false", browser)
        self.assertIn("handler.cancel()", browser)
        self.assertIn("BrowserNavigationPolicy.isAllowedNavigation", browser)
        self.assertIn("object BrowserNavigationPolicy", policy)
        self.assertIn("browserNavigationAllowsOnlyHttpAndHttpsUrlsWithHosts", tests)
        self.assertNotIn("addJavascriptInterface", browser)

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

    def test_room_persistence_module_exports_non_destructive_schema_and_daos(self):
        settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
        database = (ROOT / "core/persistence/src/main/java/com/ysfclc/mosaic/core/persistence/MosaicDatabase.kt").read_text(encoding="utf-8")
        entities = (ROOT / "core/persistence/src/main/java/com/ysfclc/mosaic/core/persistence/LibraryEntities.kt").read_text(encoding="utf-8")
        daos = (ROOT / "core/persistence/src/main/java/com/ysfclc/mosaic/core/persistence/LibraryDaos.kt").read_text(encoding="utf-8")
        build = (ROOT / "core/persistence/build.gradle.kts").read_text(encoding="utf-8")
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        self.assertIn(":core:persistence", settings)
        self.assertIn("version = 1", database)
        self.assertIn("exportSchema = true", database)
        self.assertIn("MediaCollectionItemEntity", entities)
        self.assertIn("suspend fun replaceQueue", daos)
        self.assertIn("room.schemaLocation", build)
        self.assertIn(":core:persistence:testDebugUnitTest", workflow)
        self.assertNotIn("fallbackToDestructiveMigration", database)
        self.assertIn("PlaybackTimerStateEntity", database)
        self.assertIn("abstract fun playbackTimer(): PlaybackTimerDao", database)

    def test_library_parent_upserts_do_not_delete_dependent_rows(self):
        daos = (ROOT / "core/persistence/src/main/java/com/ysfclc/mosaic/core/persistence/LibraryDaos.kt").read_text(encoding="utf-8")
        media_items = daos.split("interface MediaItemDao", 1)[1].split("@Dao", 1)[0]
        collections = daos.split("interface MediaCollectionDao", 1)[1].split("@Dao", 1)[0]
        self.assertIn("@Upsert", media_items)
        self.assertIn("suspend fun upsert(item: MediaItemEntity)", media_items)
        self.assertIn("@Upsert", collections)
        self.assertIn("suspend fun upsert(collection: MediaCollectionEntity)", collections)
        self.assertNotIn("OnConflictStrategy.REPLACE", media_items)
        self.assertNotIn("OnConflictStrategy.REPLACE", collections)

    def test_legacy_playback_preferences_import_is_atomic_and_retry_safe(self):
        importer = (ROOT / "app/src/main/java/com/ysfclc/mosaic/LegacyPreferencesImporter.kt").read_text(encoding="utf-8")
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("database.withTransaction", importer)
        self.assertIn("markerDao.find(MARKER_KEY)", importer)
        self.assertIn("database.playbackHistory().findByEventId", importer)
        self.assertIn("database.playbackQueue().entries", importer)
        self.assertIn("database.playbackTimer().state", importer)
        self.assertIn("database.playbackHistory().clearAll()", importer)
        self.assertIn("database.playbackQueue().clearQueue", importer)
        self.assertIn("database.playbackTimer().clear", importer)
        self.assertIn("if (!alreadyImported)", importer)
        self.assertLess(importer.index("check(historyEntities.all"), importer.index("markerDao.markCompleted"))
        self.assertIn("LegacyPreferencesImporter(context.applicationContext).importIfNeeded()", app)
        self.assertNotIn("preferences.edit().clear()", importer)

    def test_library_repository_is_source_neutral_and_preserves_user_state(self):
        repository = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/LibraryRepository.kt").read_text(encoding="utf-8")
        runtime_test = (ROOT / "app/src/test/java/com/ysfclc/mosaic/LibraryRepositoryTest.kt").read_text(encoding="utf-8")
        self.assertIn("fun sync(adapter: MediaSourceAdapter", repository)
        self.assertIn("MediaIdentity.stableId(sourceId, sourceItemId)", repository)
        self.assertIn("SafeMediaLocatorPolicy.isSafe(record.uri)", repository)
        self.assertIn("database.withTransaction", repository)
        self.assertIn("resumePositionMs = existing?.resumePositionMs ?: 0L", repository)
        self.assertIn("isBookmarked = existing?.isBookmarked ?: false", repository)
        self.assertIn("locationsForMediaIds", repository)
        self.assertIn(".chunked(QUERY_CHUNK_SIZE)", repository)
        self.assertIn("reconcileMissing = query.isBlank() && result.isCompleteSnapshot", repository)
        self.assertIn("markAllMissing", repository)
        self.assertIn("clearForMissingSourceItems", repository)
        media_store = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/MediaStoreCatalog.kt").read_text(encoding="utf-8")
        self.assertIn("browseSnapshot(query: String): MediaBrowseResult", media_store)
        self.assertIn("isCompleteSnapshot = completeSnapshot", media_store)
        self.assertIn("syncsAnyAdapterPreservesUserStateAndRejectsEphemeralLocations", runtime_test)

    def test_library_ui_uses_room_repository_as_its_record_source(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn("libraryRepository.observeVisible().collectAsState", app)
        self.assertIn("val adapter = LocalMediaSourceAdapter(catalog)", app)
        self.assertIn("libraryRepository.observeAndSync(adapter).collect", app)
        self.assertNotIn("catalog.scanAudio()", app)


    def test_media_store_change_observer_is_lifecycle_bound_and_preserves_distinct_events(self):
        monitor = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/MediaStoreChangeMonitor.kt").read_text(encoding="utf-8")
        repository = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/LibraryRepository.kt").read_text(encoding="utf-8")
        adapter = (ROOT / "core/library/src/main/java/com/ysfclc/mosaic/core/library/MediaStoreCatalog.kt").read_text(encoding="utf-8")
        self.assertIn("registerContentObserver", monitor)
        self.assertIn("awaitClose", monitor)
        self.assertIn("unregisterContentObserver", monitor)
        self.assertIn(".buffer(Channel.UNLIMITED)", monitor)
        self.assertIn(".distinctUntilChanged { previous, current ->", monitor)
        self.assertIn("previous is MediaSourceChange.ItemChanged", monitor)
        self.assertIn("current is MediaSourceChange.ItemChanged", monitor)
        self.assertNotIn(".conflate()", monitor)
        self.assertIn("override fun observeChanges(): Flow<Unit>", adapter)
        self.assertIn("fun observeChanges(): Flow<Unit> = emptyFlow()", (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/MediaSource.kt").read_text(encoding="utf-8"))
        media_source = (ROOT / "core/media/src/main/java/com/ysfclc/mosaic/core/media/MediaSource.kt").read_text(encoding="utf-8")
        self.assertIn("adapter.observeChangeEvents()", repository)
        self.assertIn(".onStart { emit(MediaSourceChange.Invalidate) }", repository)
        self.assertIn(".buffer(Channel.UNLIMITED)", repository)
        self.assertIn(".distinctUntilChanged { previous, current ->", repository)
        self.assertIn("previous is MediaSourceChange.ItemChanged", repository)
        self.assertIn("current is MediaSourceChange.ItemChanged", repository)
        self.assertNotIn(".conflate()", repository)
        self.assertIn("observeChanges().map { MediaSourceChange.Invalidate }", media_source)
        self.assertIn("override fun observeChangeEvents(): Flow<MediaSourceChange>", adapter)
        self.assertIn("override suspend fun findItem(sourceItemId: String)", adapter)
        self.assertIn("findAudioById(rowId)", adapter)

    def test_podcast_feed_subscriptions_are_saved_on_device(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        self.assertIn('getSharedPreferences("mosaic_podcast_feeds"', app)
        self.assertIn('putStringSet("saved_urls"', app)
        self.assertIn("savedFeeds.forEach", app)
        self.assertIn("onRemoveFeed(savedFeed)", app)

    def test_ci_validation_path_never_assembles_or_publishes_apk(self):
        workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
        import_job = workflow.split("  build:", 1)[1].split("  direct-build:", 1)[0]
        direct_job = workflow.split("  direct-build:", 1)[1]
        self.assertIn("if: false", import_job)
        self.assertIn("needs.preflight.outputs.mode == 'direct'", direct_job)
        self.assertNotIn(":app:assembleDebug", direct_job)
        self.assertNotIn("gh release create", direct_job)
        self.assertNotIn("Upload direct-build APK", direct_job)
        self.assertNotIn("MOSAIC_DEBUG_KEYSTORE", direct_job)
        self.assertIn(":app:testDebugUnitTest", direct_job)
        self.assertIn(":app:lintDebug", direct_job)

    def test_direct_web_video_has_a_native_player_surface(self):
        app = (ROOT / "app/src/main/java/com/ysfclc/mosaic/ui/MosaicApp.kt").read_text(encoding="utf-8")
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("androidx.media3:media3-ui:1.11.1", gradle)
        self.assertIn("PlayerView(viewContext)", app)
        self.assertIn("@androidx.annotation.OptIn(markerClass = [UnstableApi::class])", app)
        self.assertIn("onVideoSizeChanged(videoSize: VideoSize)", app)
        self.assertIn("Local library indexing and file selection remain audio-only", readme)

    def test_signing_migration_candidate_is_manual_and_not_published(self):
        workflow = (ROOT / ".github/workflows/signing-migration.yml").read_text(encoding="utf-8")
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("refs/heads/security/signing-lineage-migration", workflow)
        self.assertNotIn("push:", workflow)
        for secret in (
            "MOSAIC_RELEASE_KEYSTORE_BASE64",
            "MOSAIC_RELEASE_KEYSTORE_PASSWORD",
            "MOSAIC_RELEASE_KEY_ALIAS",
            "MOSAIC_RELEASE_KEY_PASSWORD",
        ):
            self.assertIn('if [[ -z "$' + secret + '" ]]', workflow)
        self.assertIn("--lineage", workflow)
        self.assertIn("--rotation-min-sdk-version 28", workflow)
        self.assertIn("MOSAIC-signing-lineage.bin", workflow)
        self.assertIn("contents: read", workflow)
        self.assertNotIn("gh release create", workflow)
        self.assertIn("Physical Android 14 install-over-existing-app/data-preservation test is still required.", workflow)

    def test_release_variant_is_unsigned_until_apksigner_lineage_step(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        release = gradle.split('getByName("release")', 1)[1].split("}", 1)[0]
        self.assertIn("isDebuggable = false", release)
        self.assertNotIn("signingConfig", release)
        workflow = (ROOT / ".github/workflows/signing-migration.yml").read_text(encoding="utf-8")
        self.assertIn(":app:assembleRelease", workflow)
        self.assertIn('"$APKSIGNER" sign', workflow)

if __name__ == "__main__":
    unittest.main()
