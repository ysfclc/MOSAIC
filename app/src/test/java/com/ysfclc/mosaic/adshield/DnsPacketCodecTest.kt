package com.ysfclc.mosaic.adshield

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsPacketCodecTest {
    @Test
    fun parsesIpv4UdpDnsQuestion() {
        val parsed = DnsPacketCodec.parseIpv4UdpQuery(ipv4DnsQuery("ads.example.doubleclick.net"))
        assertEquals("ads.example.doubleclick.net", DnsPacketCodec.parseQuestion(parsed!!.dnsMessage)?.domain)
        assertEquals(53124, parsed.sourcePort)
        assertEquals(53, parsed.destinationPort)
    }

    @Test
    fun blockedResponseIsNxDomainAndEchoesQuestion() {
        val query = dnsQuery("ads.example.doubleclick.net")
        val response = DnsPacketCodec.buildNxDomainResponse(query)!!

        assertEquals(0x8183, u16(response, 2))
        assertEquals(1, u16(response, 4))
        assertEquals(0, u16(response, 6))
        assertEquals(0, u16(response, 8))
        assertEquals(0, u16(response, 10))
        assertArrayEquals(query.copyOfRange(12, query.size), response.copyOfRange(12, response.size))
    }

    @Test
    fun wrapsDnsResponseInReversedIpv4UdpPacket() {
        val request = DnsPacketCodec.parseIpv4UdpQuery(ipv4DnsQuery("media.example"))!!
        val dnsResponse = DnsPacketCodec.buildServFailResponse(request.dnsMessage)!!
        val packet = DnsPacketCodec.buildIpv4UdpResponse(request, dnsResponse)!!

        assertEquals(17, packet[9].toInt() and 0xff)
        assertEquals(53, u16(packet, 20))
        assertEquals(53124, u16(packet, 22))
        assertArrayEquals(request.destinationAddress, packet.copyOfRange(12, 16))
        assertArrayEquals(request.sourceAddress, packet.copyOfRange(16, 20))
        assertEquals(0, DnsPacketCodec.checksum(packet, 0, 20))
    }

    @Test
    fun ignoresNonDnsAndMalformedPackets() {
        val packet = ipv4DnsQuery("media.example")
        packet[23] = 80
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(packet))
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun rejectsPacketLengthOutsideBackingBuffer() {
        val packet = ipv4DnsQuery("media.example")
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(packet, packet.size + 1))
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(packet, packet.size - 1))
    }

    @Test
    fun rejectsFragmentedAndInconsistentUdpPackets() {
        val fragmented = ipv4DnsQuery("media.example").also { putU16(it, 6, 0x2000) }
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(fragmented))

        val invalidUdpLength = ipv4DnsQuery("media.example").also { putU16(it, 24, it.size) }
        assertNull(DnsPacketCodec.parseIpv4UdpQuery(invalidUdpLength))
    }

    @Test
    fun acceptsOnlyMatchingDnsResponses() {
        val query = dnsQuery("media.example")
        val valid = DnsPacketCodec.buildServFailResponse(query)!!
        assertTrue(DnsPacketCodec.isMatchingResponse(query, valid))

        val wrongId = valid.copyOf().also { it[0] = (it[0].toInt() xor 0x01).toByte() }
        assertFalse(DnsPacketCodec.isMatchingResponse(query, wrongId))

        val notAResponse = valid.copyOf().also { it[2] = (it[2].toInt() and 0x7f).toByte() }
        assertFalse(DnsPacketCodec.isMatchingResponse(query, notAResponse))

        val wrongQuestion = valid.copyOf().also { it[13] = 'x'.code.toByte() }
        assertFalse(DnsPacketCodec.isMatchingResponse(query, wrongQuestion))
    }

    private fun ipv4DnsQuery(domain: String): ByteArray {
        val dns = dnsQuery(domain)
        val packet = ByteArray(20 + 8 + dns.size)
        packet[0] = 0x45
        putU16(packet, 2, packet.size)
        packet[8] = 64
        packet[9] = 17
        byteArrayOf(10, 111, 0, 2).copyInto(packet, 12)
        byteArrayOf(10, 111, 0, 1).copyInto(packet, 16)
        putU16(packet, 20, 53124)
        putU16(packet, 22, 53)
        putU16(packet, 24, 8 + dns.size)
        dns.copyInto(packet, 28)
        return packet
    }

    private fun dnsQuery(domain: String): ByteArray {
        val question = ArrayList<Byte>()
        domain.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            question += bytes.size.toByte()
            bytes.forEach { question.add(it) }
        }
        question += 0.toByte()
        question += 0.toByte()
        question += 1.toByte() // A
        question += 0.toByte()
        question += 1.toByte() // IN
        val dns = ByteArray(12 + question.size)
        putU16(dns, 0, 0x1234)
        putU16(dns, 2, 0x0100) // Recursion desired.
        putU16(dns, 4, 1)
        question.forEachIndexed { index, value -> dns[12 + index] = value }
        return dns
    }

    private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}

class AdShieldRulesTest {
    @Test
    fun blocksKnownAdDomainsAndSubdomains() {
        assertTrue(AdShieldRules.isBlocked("doubleclick.net"))
        assertTrue(AdShieldRules.isBlocked("ads.eu.doubleclick.net"))
    }

    @Test
    fun doesNotBlockLookalikeDomains() {
        assertFalse(AdShieldRules.isBlocked("doubleclick.net.example.org"))
        assertFalse(AdShieldRules.isBlocked("mydoubleclick.net"))
    }

    @Test
    fun allowlistOverridesBlockRulesForExactHostAndSubdomains() {
        val allowlist = setOf("doubleclick.net")
        assertFalse(AdShieldRules.isBlocked("ads.doubleclick.net", allowlist))
    }

    @Test
    fun ignoresMalformedHostValues() {
        assertFalse(AdShieldRules.isBlocked("https://doubleclick.net/ad.js"))
        assertFalse(AdShieldRules.isBlocked(""))
    }
}
