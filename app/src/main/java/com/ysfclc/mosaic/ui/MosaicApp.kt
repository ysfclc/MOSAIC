package com.ysfclc.mosaic.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.session.MediaController
import com.ysfclc.mosaic.core.design.MosaicPalette
import com.ysfclc.mosaic.core.library.MediaStoreCatalog
import com.ysfclc.mosaic.core.media.MediaItemRecord
import com.ysfclc.mosaic.core.media.PlaybackConnection
import com.ysfclc.mosaic.core.media.PlayableStreamAddress
import com.ysfclc.mosaic.core.media.PodcastFeedLoader
import com.ysfclc.mosaic.ui.browser.AdSkipAccessibilityStatus
import com.ysfclc.mosaic.ui.browser.BrowserScreen
import com.ysfclc.mosaic.ui.browser.ExternalMediaSession
import com.ysfclc.mosaic.ui.browser.ExternalMediaSessionBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class Destination(val title: String) {
    HOME("Home"), EXPLORE("Explore"), LIBRARY("Library"), PLAYER("Player"), SHIELD("Shield"), BROWSER("Browser")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MosaicApp(initialSharedText: String? = null, onSharedTextConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val catalog = remember(context) { MediaStoreCatalog(context.applicationContext) }
    val playback = remember(context) { PlaybackConnection(context.applicationContext) }
    val controller by playback.controller.collectAsState()
    val connectionError by playback.connectionError.collectAsState()

    var destination by remember { mutableStateOf(Destination.HOME) }
    var pendingBrowserUrl by remember { mutableStateOf<String?>(null) }
    var externalSessions by remember { mutableStateOf(emptyList<ExternalMediaSession>()) }
    var notificationAccess by remember { mutableStateOf(ExternalMediaSessionBridge.hasNotificationAccess(context)) }
    var adSkipEnabled by remember { mutableStateOf(AdSkipAccessibilityStatus.isEnabled(context)) }
    var podcastEpisodes by remember { mutableStateOf(emptyList<MediaItemRecord>()) }
    var podcastFeedTitle by remember { mutableStateOf<String?>(null) }
    var podcastLoading by remember { mutableStateOf(false) }
    var podcastError by remember { mutableStateOf<String?>(null) }
    val podcastFeedPreferences = remember(context) {
        context.getSharedPreferences("mosaic_podcast_feeds", android.content.Context.MODE_PRIVATE)
    }
    var savedPodcastFeeds by remember(podcastFeedPreferences) {
        mutableStateOf(podcastFeedPreferences.getStringSet("saved_urls", emptySet()).orEmpty().toList().sorted())
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(context) {
        while (isActive) {
            notificationAccess = ExternalMediaSessionBridge.hasNotificationAccess(context)
            adSkipEnabled = AdSkipAccessibilityStatus.isEnabled(context)
            externalSessions = if (notificationAccess) ExternalMediaSessionBridge.activeSessions(context) else emptyList()
            delay(2000)
        }
    }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            pendingBrowserUrl = initialSharedText
            destination = Destination.BROWSER
            onSharedTextConsumed()
        }
    }
    var tracks by remember { mutableStateOf(emptyList<MediaItemRecord>()) }
    var indexing by remember { mutableStateOf(false) }
    var audioPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    var scanCounter by remember { mutableIntStateOf(0) }
    var playerRevision by remember { mutableIntStateOf(0) }

    val permissionName = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audioPermission = granted
        if (granted) scanCounter++
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "Selected media"
            val mime = context.contentResolver.getType(uri)
            playback.playUri(uri, title, mime)
            destination = Destination.PLAYER
        }
    }

    LaunchedEffect(playback) { playback.connect() }
    DisposableEffect(playback) { onDispose { playback.release() } }

    LaunchedEffect(scanCounter, audioPermission) {
        if (audioPermission) {
            indexing = true
            tracks = withContext(Dispatchers.IO) { catalog.scanAudio() }
            indexing = false
        }
    }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { playerRevision++ }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playerRevision++ }
            override fun onPlaybackStateChanged(playbackState: Int) { playerRevision++ }
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) { playerRevision++ }
            override fun onVideoSizeChanged(videoSize: VideoSize) { playerRevision++ }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (controller != null) {
            if (controller?.isPlaying == true) playerRevision++
            delay(800)
        }
    }

    val requestScan: () -> Unit = {
        if (audioPermission) scanCounter++ else permissionLauncher.launch(permissionName)
    }
    val openFile: () -> Unit = { documentLauncher.launch(arrayOf("audio/*")) }
    val playTrack: (Int) -> Unit = { index ->
        playback.play(tracks, index)
        destination = Destination.PLAYER
    }
    val openExternalSource: (String) -> Unit = { source ->
        val target = when (source.lowercase(Locale.ROOT)) {
            "youtube" -> "com.google.android.youtube" to "https://www.youtube.com"
            "spotify" -> "com.spotify.music" to "https://open.spotify.com"
            "instagram" -> "com.instagram.android" to "https://www.instagram.com"
            "facebook" -> "com.facebook.katana" to "https://www.facebook.com"
            else -> "" to "https://www.google.com"
        }
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target.second)).apply {
            if (target.first.isNotBlank()) setPackage(target.first)
        }
        runCatching { context.startActivity(appIntent) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target.second))) }
    }

    val currentItem = controller?.currentMediaItem
    val hasNowPlaying = currentItem != null
    val currentTitle = currentItem?.mediaMetadata?.title?.toString().orEmpty()
    val currentArtist = currentItem?.mediaMetadata?.artist?.toString().orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0A0E19), MosaicPalette.Canvas, Color(0xFF0B1020)),
                    start = Offset.Zero,
                    end = Offset(1100f, 2100f),
                ),
            ),
    ) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            MosaicHeader(phaseLabel = "PHASE 01")

            AnimatedContent(
                targetState = destination,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 28 }) togetherWith
                        (fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 36 }) using SizeTransform(clip = false)
                },
                label = "mosaic-destination",
            ) { target ->
                when (target) {
                    Destination.HOME -> HomeScreen(
                        trackCount = tracks.size,
                        indexing = indexing,
                        hasPermission = audioPermission,
                        nowPlaying = hasNowPlaying,
                        currentTitle = currentTitle,
                        currentArtist = currentArtist,
                        externalSessionCount = externalSessions.size,
                        onScan = requestScan,
                        onOpenFile = openFile,
                        onOpenLibrary = { destination = Destination.LIBRARY },
                        onOpenPlayer = { destination = Destination.PLAYER },
                        onExplore = { destination = Destination.EXPLORE },
                        onOpenBrowser = { destination = Destination.BROWSER },
                        onOpenShield = { destination = Destination.SHIELD },
                        onOpenSource = openExternalSource,
                        onResume = { controller?.play() },
                    )
                    Destination.EXPLORE -> ExploreScreen(
                        onOpenSource = openExternalSource,
                        onOpenBrowser = { destination = Destination.BROWSER },
                        onOpenLibrary = { destination = Destination.LIBRARY },
                        onOpenPlayer = { destination = Destination.PLAYER },
                        onOpenShield = { destination = Destination.SHIELD },
                        onScan = requestScan,
                        onOpenFile = openFile,
                    )
                    Destination.LIBRARY -> LibraryScreen(
                        tracks = tracks,
                        indexing = indexing,
                        hasPermission = audioPermission,
                        onScan = requestScan,
                        onOpenFile = openFile,
                        onPlay = playTrack,
                    )
                    Destination.PLAYER -> PlayerScreen(
                        controller = controller,
                        revision = playerRevision,
                        connectionError = connectionError,
                        externalSessions = externalSessions,
                        hasExternalSessionAccess = notificationAccess,
                        onEnableExternalSessionAccess = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        adSkipEnabled = adSkipEnabled,
                        onEnableAdSkip = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onPlayStream = { rawAddress ->
                            val address = PlayableStreamAddress.normalize(rawAddress)
                            if (address == null) {
                                false
                            } else {
                                val uri = Uri.parse(address)
                                val title = uri.lastPathSegment?.substringBefore('?')?.takeIf { it.isNotBlank() } ?: uri.host.orEmpty()
                                val mimeType = if (uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)) "application/x-mpegURL" else null
                                playback.playUri(uri, title.ifBlank { "Internet radio" }, mimeType)
                                true
                            }
                        },
                        podcastEpisodes = podcastEpisodes,
                        podcastFeedTitle = podcastFeedTitle,
                        podcastLoading = podcastLoading,
                        podcastError = podcastError,
                        savedPodcastFeeds = savedPodcastFeeds,
                        onRemovePodcastFeed = { address ->
                            val updated = savedPodcastFeeds - address
                            savedPodcastFeeds = updated
                            podcastFeedPreferences.edit().putStringSet("saved_urls", updated.toSet()).apply()
                        },
                        onLoadPodcastFeed = { address ->
                            podcastLoading = true
                            podcastError = null
                            coroutineScope.launch {
                                runCatching { PodcastFeedLoader.load(address) }
                                    .onSuccess { feed ->
                                        podcastFeedTitle = feed.title
                                        podcastEpisodes = feed.episodes
                                        PlayableStreamAddress.normalize(address)?.let { safeAddress ->
                                            val updated = (savedPodcastFeeds + safeAddress).distinct().sorted()
                                            savedPodcastFeeds = updated
                                            podcastFeedPreferences.edit().putStringSet("saved_urls", updated.toSet()).apply()
                                        }
                                        if (feed.episodes.isEmpty()) podcastError = "No playable HTTPS episodes were found in this feed."
                                    }
                                    .onFailure { error -> podcastError = error.message ?: "Could not load this podcast feed." }
                                podcastLoading = false
                            }
                        },
                        onPlayPodcastEpisode = { index ->
                            playback.play(podcastEpisodes, index)
                            destination = Destination.PLAYER
                        },
                        onOpenLibrary = { destination = Destination.LIBRARY },
                    )
                    Destination.SHIELD -> com.ysfclc.mosaic.ui.adshield.AdShieldScreen()
                    Destination.BROWSER -> BrowserScreen(
                        initialUrl = pendingBrowserUrl,
                        onInitialUrlConsumed = { pendingBrowserUrl = null },
                        onPlayMedia = { rawAddress, mediaTitle, mediaType ->
                            val safeAddress = PlayableStreamAddress.normalize(rawAddress)
                            if (safeAddress == null) {
                                false
                            } else {
                                val uri = Uri.parse(safeAddress)
                                val mimeType = mediaType?.takeIf { it.isNotBlank() }
                                    ?: if (uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)) "application/x-mpegURL" else null
                                playback.playUri(uri, mediaTitle.ifBlank { "Web media" }, mimeType)
                                destination = Destination.PLAYER
                                true
                            }
                        },
                    )
                }
            }

            if (hasNowPlaying && destination != Destination.PLAYER) {
                MiniPlayer(
                    title = currentTitle.ifBlank { "Unknown title" },
                    subtitle = currentArtist,
                    isPlaying = controller?.isPlaying == true,
                    onOpen = { destination = Destination.PLAYER },
                    onToggle = { if (controller?.isPlaying == true) controller?.pause() else controller?.play() },
                    onNext = { controller?.seekToNextMediaItem() },
                )
            }

            BottomNavigation(
                selected = destination,
                onSelect = { destination = it },
            )
        }
    }
}

private fun hasAudioPermission(context: android.content.Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }
    }.getOrNull()
}

@Composable
private fun MosaicHeader(phaseLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(
                Brush.linearGradient(listOf(MosaicPalette.Violet, MosaicPalette.Cyan)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Waves, contentDescription = null, tint = Color(0xFF080B14), modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
            Text("MOSAIC", color = MosaicPalette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp)
            Text("PERSONAL MEDIA OS", color = MosaicPalette.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.25.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = MosaicPalette.SurfaceGlass,
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, MosaicPalette.Violet.copy(alpha = .28f)),
        ) {
            Text(phaseLabel, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    trackCount: Int,
    indexing: Boolean,
    hasPermission: Boolean,
    nowPlaying: Boolean,
    currentTitle: String,
    currentArtist: String,
    externalSessionCount: Int,
    onScan: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onExplore: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenShield: () -> Unit,
    onOpenSource: (String) -> Unit,
    onResume: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("YOUR MEDIA, IN ONE FLOW", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Everything you love.\nOne place to play.", color = MosaicPalette.TextPrimary, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp)
                Text("Across your apps, your library and the open web.", color = MosaicPalette.TextSecondary, fontSize = 12.sp)
            }
        }
        item {
            HeroCard(
                nowPlaying = nowPlaying,
                currentTitle = currentTitle,
                currentArtist = currentArtist,
                onPrimary = if (nowPlaying) onResume else onExplore,
                onSecondary = onOpenPlayer,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading("YOUR SOURCES", "Pick up anywhere")
                Spacer(Modifier.weight(1f))
                Text("SEE ALL", color = MosaicPalette.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.clickable(onClick = onExplore).padding(6.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceLaunchCard("YouTube", "Video · sessions", Icons.Default.PlayArrow, Modifier.weight(1f)) { onOpenSource("YouTube") }
                    SourceLaunchCard("Spotify", "Music · sessions", Icons.Default.MusicNote, Modifier.weight(1f)) { onOpenSource("Spotify") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceLaunchCard("Instagram", "Reels · video", Icons.Default.Waves, Modifier.weight(1f)) { onOpenSource("Instagram") }
                    SourceLaunchCard("Facebook", "Video · watch", Icons.Default.Language, Modifier.weight(1f)) { onOpenSource("Facebook") }
                }
            }
        }
        item {
            SectionHeading("YOUR MEDIA SPACE", "Everything stays yours")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("LOCAL ITEMS", trackCount.toString().padStart(2, '0'), Icons.Default.LibraryMusic, Modifier.weight(1f))
                MetricCard("EXTERNAL SESSIONS", externalSessionCount.toString().padStart(2, '0'), Icons.Default.Waves, Modifier.weight(1f), positive = externalSessionCount > 0)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ActionTile("Your library", "Browse local media", Icons.Default.LibraryMusic, onOpenLibrary, Modifier.weight(1f))
                ActionTile("Open the web", "Find media online", Icons.Default.Language, onOpenBrowser, Modifier.weight(1f))
            }
        }
        item {
            SectionHeading("QUICK ACCESS", "Your tools, not the whole product")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ActionTile("Scan device", if (hasPermission) "Refresh local library" else "Grant audio access", Icons.Default.Refresh, onScan, Modifier.weight(1f), loading = indexing)
                ActionTile("Open a file", "Play from storage", Icons.Default.FolderOpen, onOpenFile, Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            SourceStatusCard(
                "Ad Shield",
                "Browser and device-wide DNS protection",
                "OPEN",
                MosaicPalette.Cyan,
                modifier = Modifier.clickable(onClick = onOpenShield),
            )
        }
        item {
            Text(
                "MOSAIC is local-first. It never copies or moves your original media. External apps keep ownership of streams MOSAIC cannot lawfully or technically play itself.",
                color = MosaicPalette.TextMuted,
                fontSize = 9.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
private fun ExploreScreen(
    onOpenSource: (String) -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenShield: () -> Unit,
    onScan: () -> Unit,
    onOpenFile: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("EXPLORE", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("One space.\nMany sources.", color = MosaicPalette.TextPrimary, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
                Text("Open a source, then return to MOSAIC for the controls Android makes available.", color = MosaicPalette.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        item {
            SectionHeading("STREAMING SERVICES", "Open your existing apps")
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceLaunchCard("YouTube", "External playback session", Icons.Default.PlayArrow, Modifier.weight(1f)) { onOpenSource("YouTube") }
                    SourceLaunchCard("Spotify", "External playback session", Icons.Default.MusicNote, Modifier.weight(1f)) { onOpenSource("Spotify") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceLaunchCard("Instagram", "Reels and video", Icons.Default.Waves, Modifier.weight(1f)) { onOpenSource("Instagram") }
                    SourceLaunchCard("Facebook", "Video and Watch", Icons.Default.Language, Modifier.weight(1f)) { onOpenSource("Facebook") }
                }
            }
        }
        item {
            SectionHeading("YOUR OWN SOURCES", "MOSAIC-native playback")
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SourceLaunchCard("Device library", "Index without moving originals", Icons.Default.LibraryMusic, Modifier.fillMaxWidth(), onClick = onOpenLibrary)
                SourceLaunchCard("Podcasts & radio", "RSS feeds and HTTPS streams", Icons.Default.Waves, Modifier.fillMaxWidth(), onClick = onOpenPlayer)
                SourceLaunchCard("Open web media", "Browse and hand off accessible streams", Icons.Default.Language, Modifier.fillMaxWidth(), onClick = onOpenBrowser)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ActionTile("Scan device", "Refresh library", Icons.Default.Refresh, onScan, Modifier.weight(1f))
                ActionTile("Open file", "Choose local media", Icons.Default.FolderOpen, onOpenFile, Modifier.weight(1f))
            }
        }
        item {
            SectionHeading("PROTECTION", "Ad Shield")
            SourceLaunchCard("Ad & tracker shield", "Browser filtering and optional local DNS VPN", Icons.Default.Shield, Modifier.fillMaxWidth(), onClick = onOpenShield)
        }
        item {
            Text("External services remain the owners of their own playback. MOSAIC shows only controls and capabilities that Android actually exposes.", color = MosaicPalette.TextMuted, fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SourceLaunchCard(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color(0xA51B2940),
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .075f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(37.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(MosaicPalette.Violet.copy(alpha = .35f), MosaicPalette.Cyan.copy(alpha = .18f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MosaicPalette.Cyan, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MosaicPalette.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = MosaicPalette.TextMuted, fontSize = 8.sp, lineHeight = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Open $title", tint = MosaicPalette.Cyan, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun HeroCard(
    nowPlaying: Boolean,
    currentTitle: String,
    currentArtist: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF20284C), Color(0xFF19243A), Color(0xFF102D36))))
            .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(26.dp)),
    ) {
        Canvas(Modifier.fillMaxWidth().height(210.dp)) {
            drawCircle(Brush.radialGradient(listOf(MosaicPalette.Violet.copy(alpha = .35f), Color.Transparent)), radius = size.width * .62f, center = Offset(size.width * .91f, size.height * .03f))
            drawCircle(Brush.radialGradient(listOf(MosaicPalette.Cyan.copy(alpha = .13f), Color.Transparent)), radius = size.width * .55f, center = Offset(size.width * .04f, size.height * .9f))
        }
        Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = MosaicPalette.Cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (nowPlaying) "IN YOUR SESSION" else "BUILT AROUND YOU", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("PHASE 01", color = Color.White.copy(alpha = .46f), fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (nowPlaying) currentTitle.ifBlank { "Now playing" } else "Your media,\nreconnected.", color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (nowPlaying) currentArtist.ifBlank { "MOSAIC player" } else "One native experience. Multiple sources.", color = Color.White.copy(alpha = .68f), fontSize = 11.sp, maxLines = 2)
                }
                SignalOrb(Modifier.size(100.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(containerColor = MosaicPalette.Violet, contentColor = Color(0xFF100D26)),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Icon(if (nowPlaying) Icons.Default.PlayArrow else Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (nowPlaying) "Resume" else "Open library", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onSecondary, shape = RoundedCornerShape(13.dp)) {
                    Text("Open player", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, positive: Boolean = false) {
    Surface(
        modifier = modifier,
        color = MosaicPalette.SurfaceGlass,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (positive) MosaicPalette.Cyan else MosaicPalette.Violet, modifier = Modifier.size(16.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (positive) MosaicPalette.Cyan else MosaicPalette.Violet))
            }
            Text(value, color = MosaicPalette.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .4.sp)
            Text(title, color = MosaicPalette.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.15.sp)
        }
    }
}

@Composable
private fun ActionTile(label: String, sublabel: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MosaicPalette.Surface,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MosaicPalette.Divider.copy(alpha = .8f)),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MosaicPalette.Violet.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(17.dp), color = MosaicPalette.Cyan, strokeWidth = 2.dp)
                } else {
                    Icon(icon, contentDescription = null, tint = MosaicPalette.Cyan, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = MosaicPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(sublabel, color = MosaicPalette.TextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, eyebrow: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, color = MosaicPalette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(eyebrow.uppercase(Locale.ROOT), color = MosaicPalette.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun SourceStatusCard(title: String, subtitle: String, status: String, accent: Color) {
    Surface(color = MosaicPalette.Surface.copy(alpha = .86f), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .055f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MosaicPalette.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MosaicPalette.TextMuted, fontSize = 9.sp)
            }
            Text(status, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
        }
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<MediaItemRecord>,
    indexing: Boolean,
    hasPermission: Boolean,
    onScan: () -> Unit,
    onOpenFile: () -> Unit,
    onPlay: (Int) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(tracks, query) {
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, ignoreCase = true) || it.creator.orEmpty().contains(query, ignoreCase = true) || it.collection.orEmpty().contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("YOUR COLLECTION", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Library", color = MosaicPalette.TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
            Text("Indexed in place. Your original folders stay untouched.", color = MosaicPalette.TextSecondary, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(MosaicPalette.Surface).border(1.dp, MosaicPalette.Divider, RoundedCornerShape(13.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MosaicPalette.TextMuted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = MosaicPalette.TextPrimary, fontSize = 12.sp),
                    cursorBrush = Brush.linearGradient(listOf(MosaicPalette.Violet, MosaicPalette.Cyan)),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("Search your library", color = MosaicPalette.TextMuted, fontSize = 12.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onScan, enabled = !indexing, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MosaicPalette.SurfaceRaised)) {
                if (indexing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(17.dp), color = MosaicPalette.Cyan, strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, contentDescription = "Scan local audio", tint = MosaicPalette.Cyan)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} ITEMS", color = MosaicPalette.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.weight(1f))
            Text("LOCAL · READ ONLY", color = MosaicPalette.Cyan.copy(alpha = .8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyLibraryState(
                    hasPermission = hasPermission,
                    indexing = indexing,
                    onScan = onScan,
                    onOpenFile = onOpenFile,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
                    val sourceIndex = tracks.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    MediaTrackRow(item = item, index = index + 1, onClick = { onPlay(sourceIndex) })
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(hasPermission: Boolean, indexing: Boolean, onScan: () -> Unit, onOpenFile: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
        SignalOrb(Modifier.size(116.dp))
        Text(if (hasPermission) "Your library is ready for its first scan." else "Connect your device library.", color = MosaicPalette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (hasPermission) "MOSAIC indexes media already on this device. It does not duplicate or move your files."
            else "Allow audio access to index local tracks, or open one file directly without granting library access.",
            color = MosaicPalette.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )
        Button(onClick = onScan, enabled = !indexing, shape = RoundedCornerShape(13.dp)) {
            Text(if (indexing) "Indexing…" else if (hasPermission) "Scan device" else "Grant audio access", fontSize = 11.sp)
        }
        OutlinedButton(onClick = onOpenFile, shape = RoundedCornerShape(13.dp)) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("Open one audio file", fontSize = 11.sp)
        }
    }
}

@Composable
private fun MediaTrackRow(item: MediaItemRecord, index: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MosaicPalette.Surface.copy(alpha = .88f),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .045f)),
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(index.toString().padStart(2, '0'), color = MosaicPalette.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(23.dp))
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Color(0xFF39366E), Color(0xFF14565D)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, color = MosaicPalette.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.creator, item.collection).joinToString(" · ").ifBlank { "Local media" }, color = MosaicPalette.TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatDuration(item.durationMs), color = MosaicPalette.TextMuted, fontSize = 9.sp)
            Spacer(Modifier.width(3.dp))
            Icon(Icons.Default.PlayArrow, contentDescription = "Play ${item.title}", tint = MosaicPalette.Cyan, modifier = Modifier.size(19.dp))
        }
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun PlayerScreen(
    controller: MediaController?,
    revision: Int,
    connectionError: String?,
    externalSessions: List<ExternalMediaSession>,
    hasExternalSessionAccess: Boolean,
    onEnableExternalSessionAccess: () -> Unit,
    adSkipEnabled: Boolean,
    onEnableAdSkip: () -> Unit,
    onPlayStream: (String) -> Boolean,
    podcastEpisodes: List<MediaItemRecord>,
    podcastFeedTitle: String?,
    podcastLoading: Boolean,
    podcastError: String?,
    savedPodcastFeeds: List<String>,
    onRemovePodcastFeed: (String) -> Unit,
    onLoadPodcastFeed: (String) -> Unit,
    onPlayPodcastEpisode: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val mediaItem = controller?.currentMediaItem
    val metadata = mediaItem?.mediaMetadata
    val title = metadata?.title?.toString()?.ifBlank { "Unknown title" } ?: "Nothing playing yet"
    val artist = metadata?.artist?.toString()?.ifBlank { "MOSAIC local player" } ?: "Choose something from your library"
    val duration = controller?.duration?.takeIf { it > 0L } ?: 1L
    val rawPosition = controller?.currentPosition?.coerceIn(0L, duration) ?: 0L
    val isPlaying = controller?.isPlaying == true
    val actualDuration = controller?.duration?.takeIf { it > 0L } ?: 0L
    var draggingPosition by remember(mediaItem) { mutableStateOf<Float?>(null) }
    val positionFraction = draggingPosition ?: (rawPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 12.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("NOW PLAYING", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text("Your listening space", color = MosaicPalette.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = MosaicPalette.SurfaceGlass, shape = CircleShape) {
                Text("MOSAIC PLAYER", color = MosaicPalette.Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        val videoSize = controller?.videoSize
        if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player = controller
                    }
                },
                update = { playerView -> playerView.player = controller },
                onRelease = { playerView -> playerView.player = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoSize.width.toFloat() / videoSize.height.toFloat())
                    .clip(RoundedCornerShape(22.dp)),
            )
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 13.dp), contentAlignment = Alignment.Center) {
                PlayerArtwork(isPlaying = isPlaying, revision = revision)
            }
        }
        Spacer(Modifier.height(17.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = MosaicPalette.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = MosaicPalette.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        Slider(
            value = positionFraction,
            onValueChange = { draggingPosition = it },
            onValueChangeFinished = {
                draggingPosition?.let { fraction -> controller?.seekTo((duration * fraction).toLong()) }
                draggingPosition = null
            },
            enabled = controller != null && mediaItem != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration((duration * positionFraction).toLong()), color = MosaicPalette.TextMuted, fontSize = 9.sp)
            Text(formatDuration(actualDuration), color = MosaicPalette.TextMuted, fontSize = 9.sp)
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(23.dp)) {
            IconButton(onClick = { controller?.seekToPreviousMediaItem() }, enabled = controller != null, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MosaicPalette.TextSecondary, modifier = Modifier.size(29.dp))
            }
            Surface(
                onClick = { if (isPlaying) controller?.pause() else controller?.play() },
                enabled = controller != null && mediaItem != null,
                color = MosaicPalette.Violet,
                shape = CircleShape,
                modifier = Modifier.size(66.dp),
                shadowElevation = 10.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = Color(0xFF100D26), modifier = Modifier.size(32.dp))
                }
            }
            IconButton(onClick = { controller?.seekToNextMediaItem() }, enabled = controller != null, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MosaicPalette.TextSecondary, modifier = Modifier.size(29.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (mediaItem == null) {
            Text(connectionError ?: "The player is ready. Choose a local track or open a media file.", color = MosaicPalette.TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenLibrary, shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Open library", fontSize = 11.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = MosaicPalette.Cyan, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(7.dp))
                Text("MOSAIC BACKGROUND SESSION READY", color = MosaicPalette.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        StreamSourcePanel(onPlayStream = onPlayStream)
        Spacer(Modifier.height(18.dp))
        PodcastFeedPanel(
            episodes = podcastEpisodes,
            feedTitle = podcastFeedTitle,
            loading = podcastLoading,
            error = podcastError,
            savedFeeds = savedPodcastFeeds,
            onRemoveFeed = onRemovePodcastFeed,
            onLoadFeed = onLoadPodcastFeed,
            onPlayEpisode = onPlayPodcastEpisode,
        )
        Spacer(Modifier.height(22.dp))
        ExternalPlaybackPanel(
            hasAccess = hasExternalSessionAccess,
            sessions = externalSessions,
            onEnableAccess = onEnableExternalSessionAccess,
        )
        Spacer(Modifier.height(14.dp))
        AdSkipPanel(enabled = adSkipEnabled, onEnable = onEnableAdSkip)
    }
}

@Composable
private fun StreamSourcePanel(
    onPlayStream: (String) -> Boolean,
) {
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PLAY FROM A LINK", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Bring a stream into MOSAIC", color = MosaicPalette.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "HTTPS audio streams and HLS radio (.m3u8) play in MOSAIC's own background session. HTTP-only stations are not enabled yet.",
                color = MosaicPalette.TextMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("HTTPS audio or radio URL") },
            placeholder = { Text("https://station.example/live.m3u8") },
        )
        if (error != null) {
            Text(error.orEmpty(), color = Color(0xFFFF9B9B), fontSize = 9.sp)
        }
        Button(
            onClick = {
                if (address.isBlank()) {
                    error = "Enter an HTTPS stream URL."
                } else if (onPlayStream(address)) {
                    error = null
                } else {
                    error = "This link is not a valid HTTPS stream address."
                }
            },
            enabled = address.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("Play in MOSAIC", fontSize = 11.sp)
        }
    }
}

@Composable
private fun PodcastFeedPanel(
    episodes: List<MediaItemRecord>,
    feedTitle: String?,
    loading: Boolean,
    error: String?,
    savedFeeds: List<String>,
    onRemoveFeed: (String) -> Unit,
    onLoadFeed: (String) -> Unit,
    onPlayEpisode: (Int) -> Unit,
) {
    var feedAddress by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PODCAST FEEDS", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Your episodes, in the MOSAIC queue", color = MosaicPalette.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Load an RSS or Atom feed over HTTPS. Choose an episode and MOSAIC queues the remaining playable episodes behind it.",
                color = MosaicPalette.TextMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
        OutlinedTextField(
            value = feedAddress,
            onValueChange = { feedAddress = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Podcast RSS / Atom feed URL") },
            placeholder = { Text("https://example.com/podcast.xml") },
        )
        Button(
            onClick = { onLoadFeed(feedAddress) },
            enabled = feedAddress.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
        ) {
            Text(if (loading) "Loading feed…" else "Load episodes", fontSize = 11.sp)
        }
        if (savedFeeds.isNotEmpty()) {
            Text("SAVED FEEDS", color = MosaicPalette.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            savedFeeds.forEach { savedFeed ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0x441B2940)).padding(start = 11.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        savedFeed,
                        color = MosaicPalette.TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable(enabled = !loading) { feedAddress = savedFeed; onLoadFeed(savedFeed) }.padding(vertical = 5.dp),
                    )
                    Text(
                        "REMOVE",
                        color = MosaicPalette.TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onRemoveFeed(savedFeed) }.padding(horizontal = 5.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = Color(0xFFFF9B9B), fontSize = 9.sp, lineHeight = 13.sp)
        }
        if (!feedTitle.isNullOrBlank() && episodes.isNotEmpty()) {
            Text(
                "$feedTitle · ${episodes.size} playable episodes",
                color = MosaicPalette.Cyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
            )
            episodes.take(30).forEachIndexed { index, episode ->
                Surface(
                    onClick = { onPlayEpisode(index) },
                    color = Color(0x661B2940),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(episode.title, color = MosaicPalette.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(episode.creator ?: feedTitle, color = MosaicPalette.TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play episode and queue following episodes", tint = MosaicPalette.Cyan)
                    }
                }
            }
            if (episodes.size > 30) {
                Text("Showing the first 30 episodes.", color = MosaicPalette.TextMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ExternalPlaybackPanel(
    hasAccess: Boolean,
    sessions: List<ExternalMediaSession>,
    onEnableAccess: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PLAYING IN OTHER APPS", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Control source-owned sessions from MOSAIC", color = MosaicPalette.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "These controls talk to the original app. They do not move its audio into MOSAIC.",
                color = MosaicPalette.TextMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
        if (!hasAccess) {
            Surface(
                color = Color(0x661B2940),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("External media access is off", color = MosaicPalette.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Android requires you to enable notification-listener access before MOSAIC can see supported media sessions. MOSAIC does not read notification text.", color = MosaicPalette.TextMuted, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onEnableAccess, shape = RoundedCornerShape(11.dp)) {
                        Text("Enable", fontSize = 10.sp)
                    }
                }
            }
        } else if (sessions.isEmpty()) {
            Surface(
                color = Color(0x441B2940),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "No external session detected. Start media in YouTube, Spotify or another app; any session exposed by Android will appear here.",
                    color = MosaicPalette.TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(13.dp),
                )
            }
        } else {
            sessions.forEach { session ->
                ExternalSessionCard(session)
            }
        }
    }
}

@Composable
private fun AdSkipPanel(
    enabled: Boolean,
    onEnable: () -> Unit,
) {
    Surface(
        color = Color(0x661B2940),
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MosaicPalette.Cyan.copy(alpha = .22f) else Color.White.copy(alpha = .07f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "VISIBLE AD SKIP",
                    color = MosaicPalette.Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    if (enabled) "Android helper is enabled" else "Optional Android accessibility helper",
                    color = MosaicPalette.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "For YouTube only: taps a visible button when its label explicitly says “Skip ad”. It cannot remove or mute an ad, and it cannot force background playback.",
                    color = MosaicPalette.TextMuted,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onEnable,
                shape = RoundedCornerShape(11.dp),
            ) {
                Text(if (enabled) "Settings" else "Enable", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ExternalSessionCard(session: ExternalMediaSession) {
    var seekingFraction by remember(session.packageName, session.title) { mutableStateOf<Float?>(null) }
    val duration = session.durationMs
    val fraction = seekingFraction ?: if (duration > 0L) {
        (session.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Surface(
        color = Color(0x771B2940),
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MosaicPalette.Cyan.copy(alpha = .13f)),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(session.appLabel.uppercase(Locale.ROOT), color = MosaicPalette.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
                    Text(session.title, color = MosaicPalette.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (session.artist.isNotBlank()) {
                        Text(session.artist, color = MosaicPalette.TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    color = if (session.isPlaying) MosaicPalette.Cyan.copy(alpha = .15f) else MosaicPalette.SurfaceGlass,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (session.isPlaying) "PLAYING" else "PAUSED", color = if (session.isPlaying) MosaicPalette.Cyan else MosaicPalette.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                }
            }
            if (session.canSeek && duration > 0L) {
                Slider(
                    value = fraction,
                    onValueChange = { seekingFraction = it },
                    onValueChangeFinished = {
                        seekingFraction?.let { session.seekTo((duration * it).toLong()) }
                        seekingFraction = null
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration((duration * fraction).toLong()), color = MosaicPalette.TextMuted, fontSize = 8.sp)
                    Text(formatDuration(duration), color = MosaicPalette.TextMuted, fontSize = 8.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = session::skipPrevious, enabled = session.canSkipPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous in ${session.appLabel}", tint = MosaicPalette.TextSecondary)
                }
                IconButton(onClick = session::togglePlayback, enabled = session.canPlayPause) {
                    Icon(if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (session.isPlaying) "Pause ${session.appLabel}" else "Play ${session.appLabel}", tint = MosaicPalette.Cyan, modifier = Modifier.size(27.dp))
                }
                IconButton(onClick = session::skipNext, enabled = session.canSkipNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next in ${session.appLabel}", tint = MosaicPalette.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PlayerArtwork(isPlaying: Boolean, revision: Int) {
    val transition = rememberInfiniteTransition(label = "artwork-motion")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(22000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "artwork-rotation")
    val pulse by transition.animateFloat(.93f, 1.04f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "artwork-pulse")
    val breathe by animateFloatAsState(if (isPlaying) 1f else .72f, tween(350), label = "playing-state")
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(29.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF23204B), Color(0xFF123E50), Color(0xFF0D1B2B))))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(29.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * .34f * pulse
            drawCircle(Brush.radialGradient(listOf(MosaicPalette.Violet.copy(alpha = .42f * breathe), Color.Transparent), center, radius * 1.8f), radius = radius * 1.8f, center = center)
            for (ring in 0..4) {
                drawCircle(
                    color = if (ring % 2 == 0) MosaicPalette.Cyan.copy(alpha = .28f) else MosaicPalette.Violet.copy(alpha = .30f),
                    radius = radius * (0.46f + ring * .19f),
                    center = center,
                    style = Stroke(width = if (ring == 0) 2.0.dp.toPx() else 1.dp.toPx()),
                )
            }
            val path = Path()
            for (step in 0..180) {
                val angle = step / 180f * (PI.toFloat() * 2f)
                val wobble = 1f + .07f * sin(angle * 5f + rotation / 55f) + .035f * cos(angle * 9f - rotation / 70f)
                val r = radius * .72f * wobble
                val point = Offset(center.x + cos(angle + rotation / 180f) * r, center.y + sin(angle + rotation / 180f) * r)
                if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(path, color = MosaicPalette.Cyan.copy(alpha = .62f * breathe), style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Waves, contentDescription = null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(48.dp))
            Text("MOSAIC", color = Color.White.copy(alpha = .90f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("SIGNAL / ${revision.toString().padStart(3, '0')}", color = MosaicPalette.Cyan.copy(alpha = .78f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun MiniPlayer(title: String, subtitle: String, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
        color = Color(0xE91B2940),
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MosaicPalette.Violet.copy(alpha = .24f)),
        shadowElevation = 12.dp,
    ) {
        Row(Modifier.clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(MosaicPalette.VioletDeep, Color(0xFF155D66)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = MosaicPalette.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle.ifBlank { "MOSAIC player" }, color = MosaicPalette.TextMuted, fontSize = 8.sp, maxLines = 1)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(34.dp)) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = MosaicPalette.TextPrimary)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MosaicPalette.Cyan)
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: Destination, onSelect: (Destination) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xD9121928))
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        val destinations = listOf(
            Triple(Destination.HOME, Icons.Default.Home, "Home"),
            Triple(Destination.EXPLORE, Icons.Default.Language, "Explore"),
            Triple(Destination.LIBRARY, Icons.Default.LibraryMusic, "Library"),
            Triple(Destination.PLAYER, Icons.Default.Waves, "Player"),
            Triple(Destination.SHIELD, Icons.Default.Shield, "Shield"),
        )
        destinations.forEach { (destination, icon, label) ->
            val active = selected == destination
            val alpha by animateFloatAsState(if (active) 1f else .62f, tween(180), label = "nav-$label")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                    .background(if (active) MosaicPalette.Violet.copy(alpha = .16f) else Color.Transparent)
                    .clickable { onSelect(destination) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(icon, contentDescription = label, tint = if (active) MosaicPalette.Cyan else MosaicPalette.TextMuted.copy(alpha = alpha), modifier = Modifier.size(18.dp))
                Text(label, color = if (active) MosaicPalette.TextPrimary else MosaicPalette.TextMuted, fontSize = 8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SignalOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "signal-orb")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orb-phase")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "orb-spin")
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * (.25f + phase * .035f)
        drawCircle(Brush.radialGradient(listOf(MosaicPalette.Cyan.copy(alpha = .48f), MosaicPalette.Violet.copy(alpha = .19f), Color.Transparent), center, r * 2.5f), r * 2.5f, center)
        for (i in 0..2) {
            drawCircle(
                color = (if (i % 2 == 0) MosaicPalette.Cyan else MosaicPalette.Violet).copy(alpha = .45f - i * .08f),
                radius = r * (1.05f + i * .37f),
                center = center,
                style = Stroke(width = 1.1.dp.toPx()),
            )
        }
        val arcPath = Path()
        for (i in 0..72) {
            val a = (i / 72f * 2f * PI.toFloat()) + spin / 120f
            val rr = r * (1.32f + .13f * sin(a * 3f + phase * 2f))
            val p = Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
            if (i == 0) arcPath.moveTo(p.x, p.y) else arcPath.lineTo(p.x, p.y)
        }
        drawPath(arcPath, color = Color.White.copy(alpha = .56f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = .92f), radius = 2.dp.toPx(), center = Offset(center.x + cos(spin / 57f) * r * 1.5f, center.y + sin(spin / 57f) * r * 1.5f))
    }
}

@Composable
private fun AmbientGlow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient-glow")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ambient-drift")
    Canvas(modifier) {
        drawCircle(
            brush = Brush.radialGradient(listOf(MosaicPalette.Violet.copy(alpha = .11f), Color.Transparent), center = Offset(size.width * (.90f - drift * .12f), size.height * .20f), radius = size.width * .72f),
            radius = size.width * .72f,
            center = Offset(size.width * (.90f - drift * .12f), size.height * .20f),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(MosaicPalette.Cyan.copy(alpha = .07f), Color.Transparent), center = Offset(size.width * .02f, size.height * (.76f - drift * .08f)), radius = size.width * .56f),
            radius = size.width * .56f,
            center = Offset(size.width * .02f, size.height * (.76f - drift * .08f)),
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    if (milliseconds <= 0L) return "0:00"
    val seconds = milliseconds / 1000L
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
