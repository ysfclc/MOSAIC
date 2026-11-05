package com.ysfclc.mosaic.adshield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.ysfclc.mosaic.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AdShieldVpnService : VpnService() {
    @Volatile private var isRunning = false
    @Volatile private var tunnel: ParcelFileDescriptor? = null
    @Volatile private var packetThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopShield()
                stopSelf()
                START_NOT_STICKY
            }
            else -> {
                startShield()
                START_STICKY
            }
        }
    }

    override fun onRevoke() {
        stopShield()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopShield()
        super.onDestroy()
    }

    private fun startShield() {
        if (isRunning) return
        if (VpnService.prepare(this) != null) {
            AdShieldState.setError(this, "Android VPN permission has not been granted.")
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && isPrivateDnsEnabled()) {
            AdShieldState.setError(this, "Private DNS is enabled on Android 9. MOSAIC will not downgrade it to unencrypted DNS.")
            stopSelf()
            return
        }

        runCatching { startShieldNotification() }.onFailure {
            AdShieldState.setError(this, "Could not start the Ad Shield notification: ${it.message}")
            stopSelf()
            return
        }

        val descriptor = runCatching {
            Builder()
                .setSession("MOSAIC Ad Shield")
                .setMtu(1500)
                .setBlocking(true)
                .addAddress(VPN_INTERFACE_ADDRESS, 32)
                .addDnsServer(VPN_DNS_ADDRESS)
                .addRoute(VPN_DNS_ADDRESS, 32)
                .establish()
        }.getOrElse {
            AdShieldState.setError(this, "Could not establish the local DNS filter: ${it.message}")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        if (descriptor == null) {
            AdShieldState.setError(this, "Android did not establish the VPN interface.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        tunnel = descriptor
        isRunning = true
        AdShieldState.resetSession(this)
        AdShieldState.setRunning(this, true)
        packetThread = Thread({ processPackets(descriptor) }, "MOSAIC-AdShield-DNS").apply {
            isDaemon = true
            start()
        }
    }

    private fun processPackets(descriptor: ParcelFileDescriptor) {
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32 * 1024)
        try {
            while (isRunning) {
                val length = input.read(buffer)
                if (length <= 0) continue
                val query = DnsPacketCodec.parseIpv4UdpQuery(buffer, length) ?: continue
                val question = DnsPacketCodec.parseQuestion(query.dnsMessage) ?: continue

                val dnsResponse = if (AdShieldRules.isBlocked(question.domain, AdShieldState.allowlist(this))) {
                    AdShieldState.recordBlocked(this, question.domain)
                    DnsPacketCodec.buildNxDomainResponse(query.dnsMessage)
                } else {
                    forwardDnsQuery(query.dnsMessage)
                        ?: DnsPacketCodec.buildServFailResponse(query.dnsMessage)
                } ?: continue

                val responsePacket = DnsPacketCodec.buildIpv4UdpResponse(query, dnsResponse) ?: continue
                output.write(responsePacket)
            }
        } catch (error: IOException) {
            if (isRunning) AdShieldState.setError(this, "DNS filter stopped: ${error.message}")
        } catch (error: Throwable) {
            if (isRunning) AdShieldState.setError(this, "DNS filter error: ${error.message}")
        } finally {
            runCatching { input.close() }
            runCatching { output.close() }
            if (isRunning) {
                isRunning = false
                AdShieldState.setRunning(this, false)
                AdShieldState.setError(this, "The local DNS filter stopped unexpectedly.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun forwardDnsQuery(query: ByteArray): ByteArray? {
        val networks = findUnderlyingNetworks()
        if (networks.isEmpty()) {
            AdShieldState.setError(this, "No active non-VPN network is available for DNS.")
            return null
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            forwardWithSystemResolver(networks.first(), query)
        } else {
            forwardWithProtectedUdp(networks.first(), query)
        }
    }

    private fun findUnderlyingNetworks(): List<Network> {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = runCatching {
            manager.allNetworks.filter { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@filter false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            }
        }.getOrDefault(emptyList())
        if (networks.isNotEmpty()) setUnderlyingNetworks(networks.toTypedArray())
        return networks
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("DEPRECATION")
    private fun forwardWithSystemResolver(network: Network, query: ByteArray): ByteArray? {
        val latch = CountDownLatch(1)
        val response = AtomicReference<ByteArray?>()
        val cancellation = CancellationSignal()
        val callbackExecutor = Executor { command -> command.run() }
        return try {
            DnsResolver.getInstance().rawQuery(
                network,
                query,
                DnsResolver.FLAG_NO_CACHE_LOOKUP or DnsResolver.FLAG_NO_CACHE_STORE,
                callbackExecutor,
                cancellation,
                object : DnsResolver.Callback<ByteArray> {
                    override fun onAnswer(answer: ByteArray, rcode: Int) {
                        response.set(answer)
                        latch.countDown()
                    }

                    override fun onError(error: DnsResolver.DnsException) {
                        AdShieldState.setError(this@AdShieldVpnService, "System DNS error: ${error.message}")
                        latch.countDown()
                    }
                },
            )
            if (!latch.await(DNS_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)) {
                cancellation.cancel()
                AdShieldState.setError(this, "System DNS timed out. Check the network or stop Ad Shield.")
                null
            } else {
                response.get().also { answer ->
                    if (answer != null) AdShieldState.setError(this, null)
                }
            }
        } catch (error: Exception) {
            AdShieldState.setError(this, "System DNS failed: ${error.message}")
            null
        }
    }

    private fun forwardWithProtectedUdp(network: Network, query: ByteArray): ByteArray? {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val configured = manager.getLinkProperties(network)?.dnsServers.orEmpty().filter { it.address.size == 4 }
        val resolvers = (configured + listOf(
            InetAddress.getByName(CLOUDFLARE_DNS),
            InetAddress.getByName(GOOGLE_DNS),
        )).distinctBy { it.hostAddress }

        for (resolver in resolvers) {
            val socket = runCatching { DatagramSocket() }.getOrNull() ?: continue
            try {
                if (!protect(socket)) continue
                runCatching { network.bindSocket(socket) }
                socket.soTimeout = DNS_TIMEOUT_MS
                socket.send(DatagramPacket(query, query.size, resolver, DNS_PORT))

                val responseBuffer = ByteArray(MAX_DNS_RESPONSE_BYTES)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                val answer = responsePacket.data.copyOf(responsePacket.length)
                if (answer.size >= 12 && answer[0] == query[0] && answer[1] == query[1]) {
                    AdShieldState.setError(this, null)
                    return answer
                }
            } catch (_: Exception) {
                // Try the next resolver without interrupting the local packet loop.
            } finally {
                socket.close()
            }
        }
        AdShieldState.setError(this, "Upstream DNS did not respond. Check the network or stop Ad Shield.")
        return null
    }

    private fun isPrivateDnsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val mode = Settings.Global.getString(contentResolver, "private_dns_mode")
        return !mode.isNullOrBlank() && mode != "off"
    }

    private fun startShieldNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MOSAIC Ad Shield", NotificationManager.IMPORTANCE_LOW),
            )
        }

        val stopIntent = Intent(this, AdShieldVpnService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mosaic)
            .setContentTitle("MOSAIC Ad Shield is active")
            .setContentText("Filtering known ad and tracker DNS requests")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopShield() {
        isRunning = false
        AdShieldState.setRunning(this, false)
        val activeTunnel = tunnel
        tunnel = null
        runCatching { activeTunnel?.close() }
        packetThread?.interrupt()
        packetThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_START = "com.ysfclc.mosaic.adshield.START"
        const val ACTION_STOP = "com.ysfclc.mosaic.adshield.STOP"

        private const val VPN_INTERFACE_ADDRESS = "10.111.0.2"
        private const val VPN_DNS_ADDRESS = "10.111.0.1"
        private const val CHANNEL_ID = "mosaic_ad_shield"
        private const val NOTIFICATION_ID = 7312
        private const val STOP_REQUEST_CODE = 7313
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 2500
        private const val MAX_DNS_RESPONSE_BYTES = 16 * 1024
        private const val CLOUDFLARE_DNS = "1.1.1.1"
        private const val GOOGLE_DNS = "8.8.8.8"

        fun start(context: Context) {
            val intent = Intent(context, AdShieldVpnService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AdShieldVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
