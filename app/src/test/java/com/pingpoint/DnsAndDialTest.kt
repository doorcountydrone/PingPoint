package com.pingpoint

import com.pingpoint.net.DnsLookup
import com.pingpoint.net.SpeedDial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsAndDialTest {
    @Test
    fun tellsAnAddressFromAName() {
        assertTrue(DnsLookup.isIpAddress("8.8.8.8"))
        assertTrue(DnsLookup.isIpAddress("2001:4860:4860::8888"))
        assertTrue(!DnsLookup.isIpAddress("dns.google"))
        assertTrue(!DnsLookup.isIpAddress("192.168.1.256"))
    }

    @Test
    fun listsIpv4BeforeIpv6AndSkipsANameThatIsJustTheAddress() {
        val records = DnsLookup.forwardRecords(
            listOf("2001:db8::1" to false, "1.2.3.4" to true, "1.2.3.4" to true),
        )
        assertEquals(listOf("IPv4", "IPv6"), records.map { it.kind })
        assertEquals("1.2.3.4", records.first().value)
        assertNull(DnsLookup.reverseName("8.8.8.8", "8.8.8.8"))
        assertEquals("dns.google", DnsLookup.reverseName("8.8.8.8", "dns.google.")?.value)
    }

    @Test
    fun needleRestsAtZeroAndPinsAtTheTopOfTheScale() {
        assertEquals(0f, SpeedDial.fraction(0.0))
        assertEquals(0f, SpeedDial.fraction(-5.0))
        assertEquals(1f, SpeedDial.fraction(1000.0))
        assertEquals(1f, SpeedDial.fraction(5000.0))
        val slow = SpeedDial.fraction(10.0)
        val mid = SpeedDial.fraction(100.0)
        assertTrue(slow > 0f && slow < mid && mid < 1f)
    }
}
