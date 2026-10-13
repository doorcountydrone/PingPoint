package com.pingpoint

import com.pingpoint.ui.SideHealth
import com.pingpoint.ui.alertMessage
import com.pingpoint.ui.shouldAlert
import com.pingpoint.ui.shouldClearAlert
import com.pingpoint.ui.sideHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorAlertTest {
    @Test
    fun waitsForThreeSamples() {
        assertEquals(SideHealth.Waiting, sideHealth(listOf(null, null)))
    }

    @Test
    fun silenceWhenMostPacketsGetNoReply() {
        assertEquals(SideHealth.Silent, sideHealth(listOf(null, null, null)))
        assertEquals(SideHealth.Silent, sideHealth(listOf(12.0, null, null, null, 20.0)))
    }

    @Test
    fun slowWhenTheTimeStaysBad() {
        assertEquals(SideHealth.Slow, sideHealth(listOf(200.0, 180.0, 40.0)))
        assertEquals(SideHealth.Slow, sideHealth(listOf(200.0, null, 220.0, 30.0, 240.0)))
    }

    @Test
    fun okWhenBadSamplesAreNotHalfTheWindow() {
        assertEquals(SideHealth.Ok, sideHealth(listOf(12.0, null, 20.0, 30.0, 40.0)))
    }

    @Test
    fun alertsOnTheChangeIntoBadAndAgainIfItStopsAnswering() {
        assertTrue(shouldAlert(SideHealth.Ok, SideHealth.Slow))
        assertTrue(shouldAlert(SideHealth.Slow, SideHealth.Silent))
        assertFalse(shouldAlert(SideHealth.Slow, SideHealth.Slow))
        assertFalse(shouldAlert(SideHealth.Silent, SideHealth.Ok))
    }

    @Test
    fun clearsAfterTheSideRecovers() {
        assertTrue(shouldClearAlert(SideHealth.Silent, SideHealth.Ok))
        assertFalse(shouldClearAlert(SideHealth.Silent, SideHealth.Slow))
        assertFalse(shouldClearAlert(SideHealth.Ok, SideHealth.Ok))
    }

    @Test
    fun messagesNameTheSide() {
        assertEquals("Router stopped answering", alertMessage("Router", SideHealth.Silent, listOf(null, null, null)))
        assertEquals(
            "8.8.8.8 is staying slow · 200.0 ms",
            alertMessage("8.8.8.8", SideHealth.Slow, listOf(210.0, 180.0, 200.0)),
        )
    }
}
