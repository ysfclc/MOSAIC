package com.ysfclc.mosaic.adshield

import java.nio.charset.StandardCharsets

internal data class DnsIpv4UdpQuery(
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
    val dnsMessage: ByteArray,
)

internal data class DnsQuestion(val domain: String, val questionEndOffset: Int)

internal object DnsPacketCodec {
    private const val IP_HEADER = 20
    private const val UDP_HEADER = 8
    private const val DNS_HEADER = 12

    fun parseIpv4UdpQuery(packet: ByteArray, packetLength: Int = packet.size): DnsIpv4UdpQuery? {
        if (packetLength < IP_HEADER + UDP_HEADER + DNS_HEADER) return null
        val first = packet[0].toInt() and 0xff
        if (first ushr 4 != 4) return null
        val ipHeaderLength = (first and 0x0f) * 4
        if (ipHeaderLength < IP_HEADER || ipHeaderLength + UDP_HEADER + DNS_HEADER > packetLength) return null
        val totalLength = u16(packet, 2)
        if (totalLength > packetLength || totalLength < ipHeaderLength + UDP_HEADER + DNS_HEADER) return null
        if ((packet[9].toInt() and 0xff) != 17) return null
        if (u16(packet, 6) and 0x3fff != 0) return null
        val udpOffset = ipHeaderLength
        val sourcePort = u16(packet, udpOffset)
        val destinationPort = u16(packet, udpOffset + 2)
        if (destinationPort != 53) return null
        val udpLength = u16(packet, udpOffset + 4)
        if (udpLength < UDP_HEADER + DNS_HEADER || udpOffset + udpLength > totalLength) return null
        val dns = packet.copyOfRange(udpOffset + UDP_HEADER, udpOffset + udpLength)
        if (parseQuestion(dns) == null) return null
        return DnsIpv4UdpQuery(
            packet.copyOfRange(12, 16),
            packet.copyOfRange(16, 20),
            sourcePort,
            destinationPort,
            dns,
        )
    }

    fun parseQuestion(dns: ByteArray): DnsQuestion? {
        if (dns.size < DNS_HEADER || u16(dns, 4) < 1) return null
        val labels = ArrayList<String>()
        var offset = DNS_HEADER
        while (offset < dns.size) {
            val size = dns[offset].toInt() and 0xff
            offset++
            if (size == 0) break
            if (size > 63 || size and 0xc0 != 0 || offset + size > dns.size) return null
            val label = String(dns, offset, size, StandardCharsets.US_ASCII)
            if (label.isBlank()) return null
            labels += label.lowercase()
            offset += size
        }
        if (labels.isEmpty() || offset + 4 > dns.size) return null
        val domain = labels.joinToString(".")
        if (domain.length > 253) return null
        return DnsQuestion(domain, offset + 4)
    }

    fun buildNxDomainResponse(query: ByteArray): ByteArray? = buildErrorResponse(query, 3)

    fun buildServFailResponse(query: ByteArray): ByteArray? = buildErrorResponse(query, 2)

    fun buildIpv4UdpResponse(query: DnsIpv4UdpQuery, dnsResponse: ByteArray): ByteArray? {
        if (dnsResponse.size < DNS_HEADER || query.sourceAddress.size != 4 || query.destinationAddress.size != 4) return null
        val total = IP_HEADER + UDP_HEADER + dnsResponse.size
        if (total > 0xffff) return null
        val result = ByteArray(total)
        result[0] = 0x45
        putU16(result, 2, total)
        putU16(result, 6, 0x4000)
        result[8] = 64
        result[9] = 17
        query.destinationAddress.copyInto(result, 12)
        query.sourceAddress.copyInto(result, 16)
        putU16(result, 10, checksum(result, 0, IP_HEADER))
        putU16(result, IP_HEADER, query.destinationPort)
        putU16(result, IP_HEADER + 2, query.sourcePort)
        putU16(result, IP_HEADER + 4, UDP_HEADER + dnsResponse.size)
        dnsResponse.copyInto(result, IP_HEADER + UDP_HEADER)
        return result
    }

    internal fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        val end = offset + length
        while (index + 1 < end) {
            sum += ((data[index].toInt() and 0xff) shl 8) or (data[index + 1].toInt() and 0xff)
            index += 2
        }
        if (index < end) sum += (data[index].toInt() and 0xff) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xffffL) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun buildErrorResponse(query: ByteArray, code: Int): ByteArray? {
        val question = parseQuestion(query) ?: return null
        val response = query.copyOfRange(0, question.questionEndOffset)
        val recursionDesired = u16(query, 2) and 0x0100
        putU16(response, 2, 0x8080 or recursionDesired or code)
        putU16(response, 4, 1)
        putU16(response, 6, 0)
        putU16(response, 8, 0)
        putU16(response, 10, 0)
        return response
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

    private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }
}
