package com.pingpoint

import com.pingpoint.model.PingSample
import com.pingpoint.net.HostInput
import com.pingpoint.net.PingOutputParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingOutputParserTest {
    @Test
    fun parsesIputilsReply() {
        val sample = PingOutputParser.parseSample(
            "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=11.2 ms"
        )
        requireNotNull(sample)
        assertEquals(64, sample.bytes)
        assertEquals(1, sample.sequence)
        assertEquals(117, sample.ttl)
        assertEquals(11.2, sample.rttMs, 0.001)
        assertEquals("8.8.8.8", sample.from)
        assertFalse(sample.rttIsUpperBound)
    }

    @Test
    fun parsesHostnameAndToyboxSeq() {
        val sample = PingOutputParser.parseSample(
            "64 bytes from router.home (192.168.1.1): seq=0 ttl=64 time=2.45 ms"
        )
        requireNotNull(sample)
        assertEquals(0, sample.sequence)
        assertEquals("192.168.1.1", sample.from)
        assertEquals(2.45, sample.rttMs, 0.001)
    }

    @Test
    fun parsesIpv6AndUpperBound() {
        val sample = PingOutputParser.parseSample(
            "64 bytes from 2001:db8::1: icmp_seq=1 ttl=64 time<1 ms"
        )
        requireNotNull(sample)
        assertEquals("2001:db8::1", sample.from)
        assertTrue(sample.rttIsUpperBound)
        assertEquals(1.0, sample.rttMs, 0.001)
    }

    @Test
    fun ignoresUnreachableLine() {
        assertNull(
            PingOutputParser.parseSample(
                "From 192.168.1.1 icmp_seq=1 Destination Host Unreachable"
            )
        )
    }

    @Test
    fun summarizesLossFromReplies() {
        val samples = listOf(sample(1.0), sample(3.0))
        val summary = PingOutputParser.summarize("10.0.0.1", 4, samples)
        assertEquals(4, summary.transmitted)
        assertEquals(2, summary.received)
        assertEquals(50, summary.lossPercent)
        assertEquals(1.0, summary.minMs!!, 0.001)
        assertEquals(3.0, summary.maxMs!!, 0.001)
        assertEquals(2.0, summary.avgMs!!, 0.001)
    }

    @Test
    fun explainsLookupFailures() {
        assertEquals(
            "Could not find that address.",
            PingOutputParser.failureMessage("ping: unknown host printer.local")
        )
        assertNull(PingOutputParser.failureMessage("64 bytes from 8.8.8.8: icmp_seq=1 ttl=64 time=1 ms"))
    }

    @Test
    fun rejectsFlagsAndBlankHosts() {
        assertNull(HostInput.normalize("  "))
        assertNull(HostInput.normalize("-c"))
        assertNull(HostInput.normalize("8.8.8.8; rm"))
        assertEquals("8.8.8.8", HostInput.normalize(" 8.8.8.8 "))
        assertEquals("server-1.local", HostInput.normalize("server-1.local"))
        assertEquals("2001:db8::1", HostInput.normalize("[2001:db8::1]"))
    }

    private fun sample(rtt: Double) = PingSample(
        sequence = 1,
        bytes = 64,
        ttl = 64,
        rttMs = rtt,
        rttIsUpperBound = false,
        from = "10.0.0.1",
    )
}
