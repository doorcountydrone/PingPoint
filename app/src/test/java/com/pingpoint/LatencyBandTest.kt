package com.pingpoint

import com.pingpoint.ui.LatencyBand
import com.pingpoint.ui.latencyBand
import org.junit.Assert.assertEquals
import org.junit.Test

class LatencyBandTest {
    @Test
    fun classifiesGoodFairAndBad() {
        assertEquals(LatencyBand.Good, latencyBand(0.0))
        assertEquals(LatencyBand.Good, latencyBand(59.9))
        assertEquals(LatencyBand.Fair, latencyBand(60.0))
        assertEquals(LatencyBand.Fair, latencyBand(149.9))
        assertEquals(LatencyBand.Bad, latencyBand(150.0))
        assertEquals(LatencyBand.Bad, latencyBand(400.0))
        assertEquals(LatencyBand.Bad, latencyBand(null))
    }
}
