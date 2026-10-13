package com.pingpoint

import com.pingpoint.ui.ConnectionFit
import com.pingpoint.ui.rateConnection
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionRatingTest {
    @Test
    fun aFastLinkIsGoodForEveryUse() {
        val ratings = rateConnection(downloadMbps = 100.0, uploadMbps = 20.0)
        assertEquals(List(5) { ConnectionFit.Good }, ratings.map { it.fit })
    }

    @Test
    fun aModestLinkStreamsHdAndCanPlay() {
        val ratings = rateConnection(downloadMbps = 6.0, uploadMbps = 2.0).associate { it.activity to it.fit }
        assertEquals(ConnectionFit.Good, ratings.getValue("Browsing"))
        assertEquals(ConnectionFit.Fair, ratings.getValue("HD streaming"))
        assertEquals(ConnectionFit.Poor, ratings.getValue("4K streaming"))
        assertEquals(ConnectionFit.Fair, ratings.getValue("Video calls"))
        assertEquals(ConnectionFit.Fair, ratings.getValue("Gaming"))
    }

    @Test
    fun aSlowUploadLimitsCallsAndGaming() {
        val ratings = rateConnection(downloadMbps = 3.0, uploadMbps = 0.5).associate { it.activity to it.fit }
        assertEquals(ConnectionFit.Fair, ratings.getValue("Browsing"))
        assertEquals(ConnectionFit.Poor, ratings.getValue("HD streaming"))
        assertEquals(ConnectionFit.Poor, ratings.getValue("Video calls"))
        assertEquals(ConnectionFit.Poor, ratings.getValue("Gaming"))
    }
}
