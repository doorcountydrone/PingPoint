package com.pingpoint

import com.pingpoint.model.MonitorHistory
import com.pingpoint.model.SpeedHistory
import com.pingpoint.net.HistoryCodec
import com.pingpoint.net.WakeOnLan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAndWakeTest {
    @Test
    fun roundTripsSpeedAndMonitorHistory() {
        val entries = listOf(
            SpeedHistory(atMillis = 1000L, downloadMbps = 120.5, uploadMbps = 18.25),
            MonitorHistory(
                atMillis = 2000L,
                host = "192.168.1.1",
                avgMs = 12.5,
                lossPercent = 5,
                sent = 40,
                internetHost = "8.8.8.8",
                internetAvgMs = 30.0,
                internetLossPercent = 0,
            ),
        )
        assertEquals(entries, HistoryCodec.decode(HistoryCodec.encode(entries)))
    }

    @Test
    fun keepsNewestHistoryFirstAndCapsTheList() {
        val seed = List(HistoryCodec.MAX_ENTRIES) { index ->
            SpeedHistory(atMillis = index.toLong(), downloadMbps = 1.0, uploadMbps = 1.0)
        }
        val next = HistoryCodec.prepend(seed, SpeedHistory(9999L, 50.0, 10.0))
        assertEquals(HistoryCodec.MAX_ENTRIES, next.size)
        assertEquals(9999L, next.first().atMillis)
    }

    @Test
    fun buildsAMagicPacketFromAMacAddress() {
        val packet = WakeOnLan.magicPacket("AA:BB:CC:DD:EE:FF")!!
        assertEquals(102, packet.size)
        assertTrue(packet.take(6).all { it == 0xFF.toByte() })
        val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        for (i in 0 until 16) {
            assertTrue(packet.copyOfRange(6 + i * 6, 12 + i * 6).contentEquals(mac))
        }
        assertNull(WakeOnLan.magicPacket("bad"))
    }
}
