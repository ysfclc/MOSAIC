package com.ysfclc.mosaic.core.media

import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches a podcast feed over HTTPS with bounded size and strict timeouts. */
object PodcastFeedLoader {
    private const val MAX_FEED_BYTES = 2 * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    suspend fun load(feedAddress: String): PodcastFeed = withContext(Dispatchers.IO) {
        val safeAddress = PlayableStreamAddress.normalize(feedAddress)
            ?: throw IllegalArgumentException("Podcast feed must use a valid HTTPS address.")
        val connection = (URL(safeAddress).openConnection() as HttpsURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            setRequestProperty("User-Agent", "MOSAIC-Android/0.2")
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) { "Podcast server returned HTTP $status." }
            val contentLength = connection.contentLengthLong
            require(contentLength < 0L || contentLength <= MAX_FEED_BYTES) {
                "Podcast feed is larger than 2 MB."
            }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_FEED_BYTES) { "Podcast feed is larger than 2 MB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val xml = String(bytes, Charsets.UTF_8)
            PodcastFeedParser.parse(xml, safeAddress)
        } finally {
            connection.disconnect()
        }
    }
}
