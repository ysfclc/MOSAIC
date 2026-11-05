package com.ysfclc.mosaic.ui.adshield

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ysfclc.mosaic.adshield.AdShieldState
import com.ysfclc.mosaic.adshield.AdShieldVpnService
import com.ysfclc.mosaic.core.design.MosaicPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import kotlinx.coroutines.isActive

@Composable
fun AdShieldScreen() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(AdShieldState.isRunning(context)) }
    var blockedCount by remember { mutableIntStateOf(AdShieldState.blockedCount(context)) }
    var lastBlockedDomain by remember { mutableStateOf(AdShieldState.lastBlockedDomain(context)) }
    var allowlist by remember { mutableStateOf(AdShieldState.allowlist(context)) }
    var allowlistEntry by remember { mutableStateOf("") }
    var allowlistMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(AdShieldState.lastError(context)) }
    var privateDnsCompatibilityWarning by remember { mutableStateOf(hasPrivateDnsCompatibilityWarning(context)) }
    var showVpnConflict by remember { mutableStateOf(false) }
    var diagnosticsRunning by remember { mutableStateOf(false) }
    var diagnostics by remember { mutableStateOf("Run this check while protection is active.") }
    val scope = rememberCoroutineScope()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AdShieldVpnService.start(context)
        } else {
            AdShieldState.setError(context, "Android VPN permission was not granted.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) vpnPermissionLauncher.launch(prepareIntent)
            else AdShieldVpnService.start(context)
        } else {
            AdShieldState.setError(context, "Allow notifications so the active VPN filter remains visible and controllable.")
        }
    }

    LaunchedEffect(context) {
        while (isActive) {
            running = AdShieldState.isRunning(context)
            blockedCount = AdShieldState.blockedCount(context)
            lastBlockedDomain = AdShieldState.lastBlockedDomain(context)
            error = AdShieldState.lastError(context)
            allowlist = AdShieldState.allowlist(context)
            privateDnsCompatibilityWarning = hasPrivateDnsCompatibilityWarning(context)
            delay(1000)
        }
    }

    fun requestStart() {
        if (privateDnsCompatibilityWarning) {
            AdShieldState.setError(context, "Private DNS must be turned off on Android 9 to avoid downgrading DNS privacy.")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) vpnPermissionLauncher.launch(prepareIntent)
        else AdShieldVpnService.start(context)
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("AD SHIELD", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Protection beyond the browser.", color = MosaicPalette.TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "A user-controlled local DNS filter for apps across your device.",
                color = MosaicPalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }

        Surface(
            color = if (running) Color(0x3322D3C5) else MosaicPalette.Surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, if (running) MosaicPalette.Cyan.copy(alpha = .32f) else MosaicPalette.Divider),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (running) MosaicPalette.Cyan.copy(alpha = .15f) else MosaicPalette.SurfaceRaised,
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (running) MosaicPalette.Cyan else MosaicPalette.TextMuted,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (running) "PROTECTION ACTIVE" else "PROTECTION OFF",
                        color = if (running) MosaicPalette.Cyan else MosaicPalette.TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    if (running) "MOSAIC is filtering DNS requests." else "Device-wide filtering is currently stopped.",
                    color = MosaicPalette.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = {
                        if (running) AdShieldVpnService.stop(context)
                        else if (hasOtherVpn(context)) showVpnConflict = true
                        else requestStart()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text(if (running) "Stop protection" else "Enable device-wide protection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusMetric(
                label = "BLOCKED THIS SESSION",
                value = blockedCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatusMetric(
                label = "FILTER MODE",
                value = "DNS ONLY",
                modifier = Modifier.weight(1f),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MosaicPalette.Surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MosaicPalette.Divider),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("QUICK SELF-TEST", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "Checks that ordinary DNS still resolves and that a unique test hostname under a blocked ad domain is intercepted by MOSAIC.",
                    color = MosaicPalette.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
                Button(
                    onClick = {
                        scope.launch {
                            diagnosticsRunning = true
                            diagnostics = "Checking normal DNS and the blocked-domain probe…"
                            val ordinaryAddress = withContext(Dispatchers.IO) {
                                runCatching { InetAddress.getAllByName("example.com").firstOrNull()?.hostAddress }.getOrNull()
                            }
                            val probeHost = "mosaic-adshield-${System.currentTimeMillis()}.doubleclick.net"
                            val shouldBeAllowlisted = AdShieldState.allowlist(context).any { rule ->
                                probeHost == rule || probeHost.endsWith(".$rule")
                            }
                            val blockedBefore = AdShieldState.blockedCount(context)
                            val blockedLookup = withContext(Dispatchers.IO) {
                                runCatching { InetAddress.getAllByName(probeHost) }
                            }
                            val intercepted = withTimeoutOrNull(4_000) {
                                while (
                                    AdShieldState.blockedCount(context) <= blockedBefore ||
                                    AdShieldState.lastBlockedDomain(context) != probeHost
                                ) delay(100)
                                true
                            } ?: false
                            val normalPassed = !ordinaryAddress.isNullOrBlank()
                            val blockedPassed = if (shouldBeAllowlisted) !intercepted else intercepted && blockedLookup.isFailure
                            diagnostics = buildString {
                                append(if (normalPassed) "PASS · normal DNS resolved" else "FAIL · normal DNS did not resolve")
                                append("\n")
                                append(
                                    when {
                                        !blockedPassed -> "FAIL · DNS rule behavior did not match the allowlist"
                                        shouldBeAllowlisted -> "PASS · allowlist override respected"
                                        else -> "PASS · blocked hostname intercepted"
                                    },
                                )
                                if (normalPassed) append("\nResolved example.com to $ordinaryAddress")
                                if (!blockedPassed) append("\nCheck that Ad Shield stayed active and DNS is not bypassing the VPN.")
                            }
                            diagnosticsRunning = false
                        }
                    },
                    enabled = running && !diagnosticsRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (diagnosticsRunning) "Testing…" else "Run DNS self-test", fontSize = 10.sp)
                }
                Text(diagnostics, color = MosaicPalette.TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }

        if (lastBlockedDomain.isNotBlank()) {
            InfoCard(
                title = "Last blocked domain",
                body = lastBlockedDomain,
                accent = MosaicPalette.Cyan,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MosaicPalette.Surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MosaicPalette.Divider),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ALLOWLIST", color = MosaicPalette.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("If a site or app stops working, allow its hostname here. Subdomains are included.", color = MosaicPalette.TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                OutlinedTextField(
                    value = allowlistEntry,
                    onValueChange = { allowlistEntry = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Hostname, e.g. example.com") },
                )
                Button(
                    onClick = {
                        val host = normalizeAllowlistDomain(allowlistEntry)
                        if (host == null) {
                            allowlistMessage = "Enter a hostname only, without a URL or path."
                        } else {
                            allowlist = allowlist + host
                            AdShieldState.setAllowlist(context, allowlist)
                            allowlistEntry = ""
                            allowlistMessage = "Added $host"
                        }
                    },
                    enabled = allowlistEntry.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow this domain", fontSize = 10.sp)
                }
                if (allowlistMessage.isNotBlank()) {
                    Text(allowlistMessage, color = MosaicPalette.TextMuted, fontSize = 9.sp)
                }
                allowlist.sorted().forEach { domain ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(domain, color = MosaicPalette.TextPrimary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            allowlist = allowlist - domain
                            AdShieldState.setAllowlist(context, allowlist)
                            allowlistMessage = "Removed $domain"
                        }) { Text("Remove", fontSize = 9.sp) }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            InfoCard(
                title = "Service message",
                body = error,
                accent = Color(0xFFFFB45B),
            )
        }

        if (privateDnsCompatibilityWarning) {
            InfoCard(
                title = "Private DNS is enabled on Android 9",
                body = "This Android version does not expose the system DNS resolver needed to preserve Private DNS. MOSAIC will not downgrade your DNS privacy; turn Private DNS off before enabling this filter.",
                accent = Color(0xFFFFB45B),
            )
        }

        InfoCard(
            title = "What this first version does",
            body = "MOSAIC routes DNS queries to a local resolver, blocks a curated set of known ad/tracker domains, and forwards allowed queries through your current network. Ordinary app traffic is not routed through a full VPN tunnel.",
            accent = MosaicPalette.Violet,
        )

        InfoCard(
            title = "Coverage limits",
            body = "Apps using encrypted DNS/DoH, strict Private DNS or custom resolvers may bypass DNS filtering. Ads served from the same domain as the media, or inserted into the stream, cannot reliably be separated by DNS alone. Browser filtering remains a separate layer.",
            accent = MosaicPalette.TextMuted,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(Icons.Default.Security, contentDescription = null, tint = MosaicPalette.TextMuted, modifier = Modifier.padding(end = 8.dp))
            Text(
                "Android will show a persistent notification while this VPN filter is active. You can stop it here or from that notification.",
                color = MosaicPalette.TextMuted,
                fontSize = 9.sp,
                lineHeight = 14.sp,
            )
        }
    }

    if (showVpnConflict) {
        AlertDialog(
            onDismissRequest = { showVpnConflict = false },
            title = { Text("Another VPN is active") },
            text = {
                Text("Android allows only one active VPN at a time. Continuing may disconnect your current VPN and replace it with MOSAIC Ad Shield.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showVpnConflict = false
                    requestStart()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showVpnConflict = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MosaicPalette.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MosaicPalette.Divider),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = MosaicPalette.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            Text(value, color = MosaicPalette.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MosaicPalette.Surface.copy(alpha = .88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MosaicPalette.Divider),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(body, color = MosaicPalette.TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

private fun hasPrivateDnsCompatibilityWarning(context: Context): Boolean {
    if (Build.VERSION.SDK_INT != 28) return false
    return Settings.Global.getString(context.contentResolver, "private_dns_mode") != "off"
}

private fun hasOtherVpn(context: Context): Boolean {
    if (AdShieldState.isRunning(context)) return false
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.allNetworks.any { network ->
        manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
}


private fun normalizeAllowlistDomain(value: String): String? {
    val host = value.trim().trimEnd('.').lowercase()
    if (host.isBlank() || host.length > 253) return null
    if (!host.contains('.') || host.any { it.isWhitespace() || it == '/' || it == ':' || it == '@' }) return null
    return host
}
