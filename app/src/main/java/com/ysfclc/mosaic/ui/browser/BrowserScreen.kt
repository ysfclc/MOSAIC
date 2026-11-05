package com.ysfclc.mosaic.ui.browser

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ysfclc.mosaic.core.design.MosaicPalette
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


@Composable
fun BrowserScreen(initialUrl: String? = null, onInitialUrlConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("New tab") }
    var pageHost by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var blockedRequests by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var shieldEnabled by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val shieldRef = remember { AtomicBoolean(true) }
    val topHostRef = remember { AtomicReference("") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val currentShieldEnabled by rememberUpdatedState(shieldEnabled)
    SideEffect { shieldRef.set(currentShieldEnabled) }

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (!shieldRef.get() || !request.url.scheme.orEmpty().startsWith("http")) return null
                    if (!AdBlockPolicy.shouldBlock(request.url.toString(), topHostRef.get())) return null
                    mainHandler.post { blockedRequests += 1 }
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    topHostRef.set(android.net.Uri.parse(url).host.orEmpty())
                    mainHandler.post {
                        address = url
                        pageHost = topHostRef.get()
                        pageTitle = "Loading…"
                        progress = 5
                        loadError = null
                        canGoBack = view.canGoBack()
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    mainHandler.post {
                        address = url
                        pageTitle = view.title?.takeIf { it.isNotBlank() } ?: url
                        pageHost = android.net.Uri.parse(url).host.orEmpty()
                        progress = 100
                        canGoBack = view.canGoBack()
                    }
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    if (request.isForMainFrame) {
                        mainHandler.post { loadError = error.description?.toString() ?: "Page failed to load" }
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    mainHandler.post { progress = newProgress }
                }
            }
            loadUrl("https://www.google.com")
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = canGoBack) {
        if (webView.canGoBack()) webView.goBack()
    }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val target = when {
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            value.contains(" ") || !value.contains(".") -> "https://www.google.com/search?q=" + android.net.Uri.encode(value)
            else -> "https://$value"
        }
        webView.loadUrl(target)
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            navigate(initialUrl)
            onInitialUrlConsumed()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MOSAIC BROWSER", color = MosaicPalette.TextPrimary, fontSize = 16.sp)
                Text(
                    if (AdBlockPolicy.isProtectedProviderHost(pageHost)) "PROVIDER PLAYER · FILTER EXEMPT"
                    else "AD/TRACKER FILTER · " + if (shieldEnabled) "ON" else "OFF",
                    color = MosaicPalette.Cyan,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                )
            }
            TextButton(onClick = { shieldEnabled = !shieldEnabled }) {
                Text(if (shieldEnabled) "Shield on" else "Shield off")
            }
            TextButton(onClick = {
                if (address.isNotBlank()) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, address)
                    }
                    context.startActivity(Intent.createChooser(share, "Share page"))
                }
            }, enabled = address.isNotBlank()) { Text("Share") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            TextButton(onClick = { if (webView.canGoBack()) webView.goBack() }, enabled = canGoBack) { Text("‹") }
            TextButton(onClick = { if (webView.canGoForward()) webView.goForward() }) { Text("›") }
            TextButton(onClick = { webView.reload() }) { Text("↻") }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search or enter address") },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { navigate(address) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) { Text("Go") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { navigate("https://www.youtube.com") }) { Text("YouTube") }
            TextButton(onClick = { navigate("https://open.spotify.com") }) { Text("Spotify") }
            TextButton(onClick = { navigate("https://www.google.com") }) { Text("Search") }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(3.dp))
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color(0xFF111B2B),
            shape = RoundedCornerShape(17.dp),
        ) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)))
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0x44151F31), RoundedCornerShape(12.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pageTitle, color = MosaicPalette.TextPrimary, fontSize = 11.sp, maxLines = 1)
                Text("$blockedRequests third-party ad/tracker requests blocked", color = MosaicPalette.TextMuted, fontSize = 9.sp)
            }
            if (loadError != null) Text("Load error", color = Color(0xFFFF9C9C), fontSize = 9.sp)
        }
        Text(
            "Provider pages (including YouTube and Spotify) are deliberately exempt. This shield is not a universal or system-wide ad blocker.",
            color = MosaicPalette.TextMuted,
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
    }
}

