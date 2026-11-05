package com.ysfclc.mosaic

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.mutableStateOf
import com.ysfclc.mosaic.core.design.MosaicTheme
import com.ysfclc.mosaic.ui.MosaicApp

class MainActivity : ComponentActivity() {
    private val sharedText = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText.value = extractSharedText(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText.value = extractSharedText(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 20)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 20)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        setContent {
            MosaicTheme {
                MosaicApp(
                    initialSharedText = sharedText.value,
                    onSharedTextConsumed = { sharedText.value = null },
                )
            }
        }
    }
}

private fun extractSharedText(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_SEND) return null
    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
    if (text.isBlank()) return null
    val sharedUrl = text.lineSequence()
        .flatMap { it.split(' ').asSequence() }
        .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
    return sharedUrl?.trimEnd('.', ',', ')', ']') ?: text
}
