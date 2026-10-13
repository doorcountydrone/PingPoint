package com.pingpoint

import com.pingpoint.model.MonitorPoint
import com.pingpoint.ui.PathVerdict
import com.pingpoint.ui.pathVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

class PathVerdictTest {
    @Test
    fun waitsUntilThreePackets() {
        val points = listOf(MonitorPoint(12.0, 20.0), MonitorPoint(12.0, null))
        assertEquals(PathVerdict.Waiting, pathVerdict(points))
    }

    @Test
    fun bothFineWhenNeitherSideIsMostlyBad() {
        val points = List(5) { MonitorPoint(12.0, 40.0) }
        assertEquals(PathVerdict.BothFine, pathVerdict(points))
    }

    @Test
    fun internetDownMeansThePathPastTheRouter() {
        val points = List(4) { MonitorPoint(12.0, null) } + MonitorPoint(80.0, 400.0)
        assertEquals(PathVerdict.BeyondRouter, pathVerdict(points))
    }

    @Test
    fun bothDownMeansTheWifiPath() {
        val points = List(3) { MonitorPoint(null, null) }
        assertEquals(PathVerdict.LocalPath, pathVerdict(points))
    }

    @Test
    fun routerDownWhileInternetAnswers() {
        val points = List(3) { MonitorPoint(200.0, 30.0) }
        assertEquals(PathVerdict.RouterOnly, pathVerdict(points))
    }
}
