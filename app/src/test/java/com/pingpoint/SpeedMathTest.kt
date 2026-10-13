package com.pingpoint

import com.pingpoint.net.SpeedMath
import com.pingpoint.ui.formatMbps
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedMathTest {
    @Test
    fun convertsBytesAndTimeToMegabits() {
        assertEquals(8.0, SpeedMath.mbps(1_000_000L, 1_000_000_000L), 0.001)
        assertEquals(1.0, SpeedMath.mbps(125_000L, 1_000_000_000L), 0.001)
        assertEquals(80.0, SpeedMath.mbps(5_000_000L, 500_000_000L), 0.001)
    }

    @Test
    fun emptyTransferIsZero() {
        assertEquals(0.0, SpeedMath.mbps(0L, 1_000_000_000L), 0.0)
        assertEquals(0.0, SpeedMath.mbps(1_000_000L, 0L), 0.0)
    }

    @Test
    fun formatsMegabits() {
        assertEquals("8.00 Mb/s", formatMbps(8.0))
        assertEquals("80.0 Mb/s", formatMbps(80.0))
    }
}
