package com.pingpoint.net

import com.pingpoint.model.LanDevice
import com.pingpoint.model.PingSample
import com.pingpoint.model.PingSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

sealed interface PingEvent {
    data class Sample(val sample: PingSample) : PingEvent
    data class Summary(val summary: PingSummary) : PingEvent
    data class Mac(val mac: String) : PingEvent
    data class Failure(val message: String) : PingEvent
    data class Lost(val sequence: Int) : PingEvent
}

sealed interface ScanEvent {
    data class Found(val device: LanDevice) : ScanEvent
    data class Progress(val completed: Int, val total: Int) : ScanEvent
}

/** One second of the monitor. A null time is a lost packet on that side. */
sealed interface WatchTick {
    data class Round(
        val primaryRtt: Double?,
        val internetRtt: Double?,
        val compare: Boolean,
    ) : WatchTick

    data class Failed(val message: String) : WatchTick
    data object Stopped : WatchTick
}

object PingEngine {
    private val binary: String by lazy {
        val systemPing = File("/system/bin/ping")
        if (systemPing.exists()) systemPing.absolutePath else "ping"
    }

    fun ping(host: String, count: Int): Flow<PingEvent> = flow {
        val safeCount = count.coerceIn(1, 16)
        val process = try {
            ProcessBuilder(binary, "-n", "-c", safeCount.toString(), "-W", "2", host)
                .redirectErrorStream(true)
                .start()
        } catch (_: IOException) {
            emit(PingEvent.Failure("Could not run ping on this phone."))
            return@flow
        }
        coroutineContext.job.invokeOnCompletion { process.destroy() }
        val samples = mutableListOf<PingSample>()
        try {
            process.inputStream.bufferedReader().use { reader ->
                while (coroutineContext.isActive) {
                    val line = reader.readLine() ?: break
                    val failure = PingOutputParser.failureMessage(line)
                    if (failure != null) {
                        emit(PingEvent.Failure(failure))
                        return@flow
                    }
                    val sample = PingOutputParser.parseSample(line) ?: continue
                    samples += sample
                    emit(PingEvent.Sample(sample))
                    macIfLocal(sample.from)?.let { emit(PingEvent.Mac(it)) }
                }
            }
            if (coroutineContext.isActive) {
                process.waitFor()
                emit(PingEvent.Summary(PingOutputParser.summarize(host, safeCount, samples)))
                val repliedFrom = samples.lastOrNull()?.from
                val mac = macIfLocal(repliedFrom) ?: macIfLocal(host)
                if (mac != null) emit(PingEvent.Mac(mac))
            }
        } catch (_: IOException) {
            if (coroutineContext.isActive) {
                emit(PingEvent.Failure("Could not run ping on this phone."))
            }
        } finally {
            process.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /** Pings the router and, when set, the internet address in the same second. */
    fun watchBoth(primary: String, internet: String?): Flow<WatchTick> = flow {
        while (coroutineContext.isActive) {
            val started = System.nanoTime()
            val tick = coroutineScope {
                val first = async { probeResult(primary) }
                val second = internet?.let { host -> async { probeResult(host) } }
                toTick(first.await(), second?.await(), internet != null)
            }
            emit(tick)
            if (tick is WatchTick.Failed) return@flow
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            val pause = 1000L - elapsedMs
            if (pause > 0 && coroutineContext.isActive) delay(pause)
        }
    }.flowOn(Dispatchers.IO)

    fun scan(hosts: List<String>): Flow<ScanEvent> = channelFlow {
        if (hosts.isEmpty()) return@channelFlow
        val total = hosts.size
        val completed = AtomicInteger(0)
        val gate = Semaphore(24)
        coroutineScope {
            for (ip in hosts) {
                launch(Dispatchers.IO) {
                    gate.withPermit {
                        try {
                            if (!isActive) return@launch
                            val rtt = probe(ip)
                            if (rtt != null) {
                                send(
                                    ScanEvent.Found(
                                        LanDevice(
                                            ip = ip,
                                            mac = ArpTable.macFor(ip),
                                            rttMs = rtt,
                                        )
                                    )
                                )
                            }
                        } finally {
                            send(ScanEvent.Progress(completed.incrementAndGet(), total))
                        }
                    }
                }
            }
        }
    }

    private suspend fun probe(host: String): Double? = when (val result = probeResult(host)) {
        is ProbeResult.Reply -> result.sample.rttMs
        else -> null
    }

    private suspend fun probeResult(host: String): ProbeResult {
        val process = try {
            withContext(Dispatchers.IO) {
                ProcessBuilder(binary, "-n", "-c", "1", "-W", "1", host)
                    .redirectErrorStream(true)
                    .start()
            }
        } catch (_: IOException) {
            return ProbeResult.Failed("Could not run ping on this phone.")
        }
        coroutineContext.job.invokeOnCompletion { process.destroy() }
        return try {
            withContext(Dispatchers.IO) {
                val output = process.inputStream.bufferedReader().readText()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
                val failure = output.lineSequence().firstNotNullOfOrNull { PingOutputParser.failureMessage(it) }
                if (failure != null) {
                    ProbeResult.Failed(failure)
                } else {
                    val sample = output.lineSequence().firstNotNullOfOrNull { PingOutputParser.parseSample(it) }
                    if (sample != null) ProbeResult.Reply(sample) else ProbeResult.Timeout
                }
            }
        } catch (_: IOException) {
            ProbeResult.Timeout
        } finally {
            process.destroy()
        }
    }

    private sealed interface ProbeResult {
        data class Reply(val sample: PingSample) : ProbeResult
        data object Timeout : ProbeResult
        data class Failed(val message: String) : ProbeResult
    }

    private fun toTick(primary: ProbeResult, internet: ProbeResult?, compare: Boolean): WatchTick {
        if (primary is ProbeResult.Failed) return WatchTick.Failed(primary.message)
        if (internet is ProbeResult.Failed) return WatchTick.Failed(internet.message)
        return WatchTick.Round(
            primaryRtt = (primary as? ProbeResult.Reply)?.sample?.rttMs,
            internetRtt = (internet as? ProbeResult.Reply)?.sample?.rttMs,
            compare = compare,
        )
    }

    private fun macIfLocal(ip: String?): String? {
        if (ip.isNullOrBlank() || !Subnet.isPrivateLan(ip)) return null
        return ArpTable.macFor(ip)
    }
}
