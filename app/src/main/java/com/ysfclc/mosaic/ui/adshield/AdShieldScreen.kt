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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AdShieldScreen() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(AdShieldState.isRunning(context)) }
    var blockedCount by remember { mutableIntStateOf(AdShieldState.blockedCount(context)) }
    var lastBlockedDomain by remember { mutableStateOf(AdShieldState.lastBlockedDomain(context)) }
    var error by remember { mutableStateOf(AdShieldState.lastError(context)) }
    var strictPrivateDns by remember { mutableStateOf(hasStrictPrivateDns(context)) }
    var showVpnConflict by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AdShieldVpnService.start(context)
        } else {
            error = "Android VPN permission was not granted."
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
            error = "Allow notifications so the active VPN filter remains visible and controllable."
        }
    }

    LaunchedEffect(context) {
        while (isActive) {
            running = AdShieldState.isRunning(context)
            blockedCount = AdShieldState.blockedCount(context)
            lastBlockedDomain = AdShieldState.lastBlockedDomain(context)
            error = AdShieldState.lastError(context)
            strictPrivateDns = hasStrictPrivateDns(context)
            delay(1000)
        }
    }

    fun requestStart() {
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

        if (lastBlockedDomain.isNotBlank()) {
            InfoCard(
                title = "Last blocked domain",
                body = lastBlockedDomain,
                accent = MosaicPalette.Cyan,
            )
        }

        if (error.isNotBlank()) {
            InfoCard(
                title = "Service message",
                body = error,
                accent = Color(0xFFFFB45B),
            )
        }

        if (strictPrivateDns) {
            InfoCard(
                title = "Private DNS may bypass filtering",
                body = "Android is configured with a strict Private DNS provider. Some apps may send encrypted DNS outside this local filter.",
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

private fun hasStrictPrivateDns(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 28) return false
    return Settings.Global.getString(context.contentResolver, "private_dns_mode") == "hostname"
}

private fun hasOtherVpn(context: Context): Boolean {
    if (AdShieldState.isRunning(context)) return false
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.allNetworks.any { network ->
        manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
}
