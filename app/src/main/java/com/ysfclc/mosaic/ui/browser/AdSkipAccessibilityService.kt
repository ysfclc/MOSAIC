package com.ysfclc.mosaic.ui.browser

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional Android-native helper. It clicks only an explicitly labelled, visible ad-skip
 * control in the YouTube app. It does not inspect audio, network traffic or hidden app state.
 */
class AdSkipAccessibilityService : AccessibilityService() {
    private var lastClickAtMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != YOUTUBE_PACKAGE) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastClickAtMs < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        val target = findVisibleSkipControl(root, 0, intArrayOf(0)) ?: return
        if (clickNearestClickable(target)) {
            lastClickAtMs = now
        }
    }

    override fun onInterrupt() = Unit

    private fun findVisibleSkipControl(
        node: AccessibilityNodeInfo,
        depth: Int,
        visited: IntArray,
    ): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH || visited[0]++ >= MAX_NODES || !node.isVisibleToUser) return null
        if (isExplicitAdSkipLabel(node.text) || isExplicitAdSkipLabel(node.contentDescription)) return node

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val match = findVisibleSkipControl(child, depth + 1, visited)
            if (match != null) return match
        }
        return null
    }

    private fun clickNearestClickable(target: AccessibilityNodeInfo): Boolean {
        var node: AccessibilityNodeInfo? = target
        repeat(MAX_CLICK_ANCESTORS) {
            val current = node ?: return false
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            node = current.parent
        }
        return false
    }

    private fun isExplicitAdSkipLabel(value: CharSequence?): Boolean {
        val label = value?.toString()?.trim()?.lowercase()?.replace(Regex("\\s+"), " ") ?: return false
        return label in AD_SKIP_LABELS
    }

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val CLICK_COOLDOWN_MS = 1_500L
        private const val MAX_DEPTH = 12
        private const val MAX_NODES = 350
        private const val MAX_CLICK_ANCESTORS = 6

        private val AD_SKIP_LABELS = setOf(
            "skip ad", "skip ads", "skip advertisement", "skip advertisements",
            "pomiń reklamę", "pomiń reklamy", "pomiń ogłoszenie",
            "reklamı atla", "reklamları atla",
        )
    }
}

object AdSkipAccessibilityStatus {
    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val expected = ComponentName(context, AdSkipAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
