package com.pingpoint.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket

enum class PortState {
    Open,
    Closed,
    NoAnswer,
}

data class ServicePort(val number: Int, val name: String)

data class PortResult(val service: ServicePort, val state: PortState)

object PortProbe {
    val services: List<ServicePort> = listOf(
        ServicePort(80, "Web"),
        ServicePort(443, "Secure web"),
        ServicePort(53, "DNS"),
    )

    const val TIMEOUT_MS = 1000

    fun stateFor(error: Exception?): PortState {
        if (error == null) return PortState.Open
        if (error is ConnectException && !isUnreachable(error)) return PortState.Closed
        return PortState.NoAnswer
    }

    suspend fun check(host: String): List<PortResult> = coroutineScope {
        services.map { service ->
            async(Dispatchers.IO) { PortResult(service, probe(host, service.number)) }
        }.awaitAll()
    }

    private suspend fun probe(host: String, port: Int): PortState = withContext(Dispatchers.IO) {
        val socket = Socket()
        coroutineContext.job.invokeOnCompletion {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
        try {
            socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            PortState.Open
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            stateFor(error)
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun isUnreachable(error: ConnectException): Boolean {
        val message = error.message.orEmpty()
        return message.contains("timed out", ignoreCase = true) ||
            message.contains("unreachable", ignoreCase = true)
    }
}

fun portStateLabel(state: PortState): String = when (state) {
    PortState.Open -> "Open"
    PortState.Closed -> "Closed"
    PortState.NoAnswer -> "No answer"
}
