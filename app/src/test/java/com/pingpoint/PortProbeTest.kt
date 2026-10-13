package com.pingpoint

import com.pingpoint.net.PortProbe
import com.pingpoint.net.PortState
import com.pingpoint.net.portStateLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PortProbeTest {
    @Test
    fun connectionMeansOpen() {
        assertEquals(PortState.Open, PortProbe.stateFor(null))
        assertEquals("Open", portStateLabel(PortState.Open))
    }

    @Test
    fun refusalMeansClosed() {
        assertEquals(PortState.Closed, PortProbe.stateFor(ConnectException("Connection refused")))
        assertEquals("Closed", portStateLabel(PortState.Closed))
    }

    @Test
    fun timeoutOrMissingHostMeansNoAnswer() {
        assertEquals(PortState.NoAnswer, PortProbe.stateFor(SocketTimeoutException("timeout")))
        assertEquals(PortState.NoAnswer, PortProbe.stateFor(UnknownHostException("printer")))
        assertEquals(PortState.NoAnswer, PortProbe.stateFor(ConnectException("Network is unreachable")))
        assertEquals("No answer", portStateLabel(PortState.NoAnswer))
    }

    @Test
    fun checksTheOrdinaryServices() {
        assertEquals(listOf(80, 443, 53), PortProbe.services.map { it.number })
    }
}