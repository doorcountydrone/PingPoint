package com.pingpoint

import com.pingpoint.net.Subnet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetTest {
    @Test
    fun slash24ExcludesSelfNetworkAndBroadcast() {
        val hosts = Subnet.hostsForScan("192.168.1.23", 24)
        assertEquals(253, hosts.size)
        assertFalse(hosts.contains("192.168.1.0"))
        assertFalse(hosts.contains("192.168.1.255"))
        assertFalse(hosts.contains("192.168.1.23"))
        assertTrue(hosts.contains("192.168.1.1"))
    }

    @Test
    fun slash28StaysInsideTheSmallSubnet() {
        val hosts = Subnet.hostsForScan("192.168.1.20", 28)
        assertEquals(13, hosts.size)
        assertTrue(hosts.all { it.startsWith("192.168.1.") })
        assertFalse(hosts.contains("192.168.1.15"))
        assertFalse(hosts.contains("192.168.1.32"))
    }

    @Test
    fun widePrefixClampsToTheLocalSlash24() {
        val hosts = Subnet.hostsForScan("10.1.2.3", 8)
        assertEquals(253, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.1.2.") })
        assertEquals("10.1.2.0/24", Subnet.scanCidr("10.1.2.3", 8))
    }

    @Test
    fun privateLanCheck() {
        assertTrue(Subnet.isPrivateLan("192.168.0.1"))
        assertTrue(Subnet.isPrivateLan("10.2.3.4"))
        assertTrue(Subnet.isPrivateLan("172.16.0.1"))
        assertFalse(Subnet.isPrivateLan("172.32.0.1"))
        assertFalse(Subnet.isPrivateLan("8.8.8.8"))
    }
}
