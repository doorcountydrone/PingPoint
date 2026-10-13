package com.pingpoint.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

enum class SpeedPhase {
    Download,
    Upload,
}

sealed interface SpeedEvent {
    data class Live(val phase: SpeedPhase, val megabitsPerSecond: Double) : SpeedEvent
    data class DownloadDone(val megabitsPerSecond: Double) : SpeedEvent
    data class Finished(val downloadMbps: Double, val uploadMbps: Double) : SpeedEvent
    data class Failure(val message: String) : SpeedEvent
}

object SpeedMath {
    /** Megabits per second from a byte count and a duration. Uses decimal megabits, as ISPs do. */
    fun mbps(bytes: Long, nanos: Long): Double {
        if (bytes <= 0L || nanos <= 0L) return 0.0
        val seconds = nanos / 1_000_000_000.0
        return bytes * 8.0 / seconds / 1_000_000.0
    }
}

object SpeedTest {
    private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=40000000"
    private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
    private const val STREAMS = 3
    private const val DOWNLOAD_CAP = 40_000_000L
    private const val DOWNLOAD_WINDOW_NS = 8_000_000_000L
    private const val UPLOAD_PROBE = 4_000_000
    private const val UPLOAD_FULL = 16_000_000
    private const val FAST_UPLOAD_NS = 2_000_000_000L

    fun run(): Flow<SpeedEvent> = flow {
        try {
            val download = measureDownload { mbps ->
                emit(SpeedEvent.Live(SpeedPhase.Download, mbps))
            }
            emit(SpeedEvent.DownloadDone(download))
            val upload = measureUpload { mbps ->
                emit(SpeedEvent.Live(SpeedPhase.Upload, mbps))
            }
            emit(SpeedEvent.Finished(download, upload))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emit(SpeedEvent.Failure("Could not reach the test server."))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun measureDownload(onProgress: suspend (Double) -> Unit): Double = supervisorScope {
        val bytes = AtomicLong()
        val firstByte = AtomicLong()
        val jobs = List(STREAMS) {
            launch(Dispatchers.IO) { readDownload(bytes, firstByte) }
        }
        val wallStart = System.nanoTime()
        while (jobs.any { it.isActive } && coroutineContext.isActive) {
            val origin = firstByte.get()
            if (origin != 0L) {
                val elapsed = System.nanoTime() - origin
                onProgress(SpeedMath.mbps(bytes.get(), elapsed))
                if (elapsed >= DOWNLOAD_WINDOW_NS || bytes.get() >= DOWNLOAD_CAP) {
                    jobs.forEach { it.cancel() }
                    break
                }
            } else if (System.nanoTime() - wallStart > 12_000_000_000L) {
                jobs.forEach { it.cancel() }
                break
            }
            delay(200)
        }
        jobs.forEach { it.join() }
        val origin = firstByte.get()
        val total = bytes.get()
        if (origin == 0L || total <= 0L) error("no download")
        SpeedMath.mbps(total, System.nanoTime() - origin)
    }

    private suspend fun readDownload(bytes: AtomicLong, firstByte: AtomicLong) {
        val connection = open(DOWNLOAD_URL, "GET")
        try {
            coroutineContext.job.invokeOnCompletion { connection.disconnect() }
            if (connection.responseCode !in 200..299) return
            connection.inputStream.use { input ->
                val buffer = ByteArray(32 * 1024)
                while (coroutineContext.isActive && bytes.get() < DOWNLOAD_CAP) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    firstByte.compareAndSet(0L, System.nanoTime())
                    bytes.addAndGet(read.toLong())
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun measureUpload(onProgress: suspend (Double) -> Unit): Double {
        val probe = uploadOnce(UPLOAD_PROBE, onProgress)
        if (probe.nanos >= FAST_UPLOAD_NS) return probe.mbps
        return uploadOnce(UPLOAD_FULL, onProgress).mbps
    }

    private suspend fun uploadOnce(size: Int, onProgress: suspend (Double) -> Unit): Transfer = supervisorScope {
        val bytes = AtomicLong()
        val firstByte = AtomicLong()
        val job = launch(Dispatchers.IO) {
            val connection = open(UPLOAD_URL, "POST").apply {
                doOutput = true
                setFixedLengthStreamingMode(size)
                setRequestProperty("Content-Type", "application/octet-stream")
                readTimeout = 60_000
            }
            try {
                coroutineContext.job.invokeOnCompletion { connection.disconnect() }
                connection.outputStream.use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var sent = 0
                    while (sent < size && coroutineContext.isActive) {
                        val count = minOf(buffer.size, size - sent)
                        output.write(buffer, 0, count)
                        firstByte.compareAndSet(0L, System.nanoTime())
                        sent += count
                        bytes.addAndGet(count.toLong())
                    }
                }
                if (connection.responseCode !in 200..299) error("upload")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                connection.disconnect()
            }
        }
        while (job.isActive && coroutineContext.isActive) {
            val origin = firstByte.get()
            if (origin != 0L) onProgress(SpeedMath.mbps(bytes.get(), System.nanoTime() - origin))
            delay(200)
        }
        job.join()
        val origin = firstByte.get()
        val total = bytes.get()
        if (origin == 0L || total <= 0L) error("no upload")
        val nanos = System.nanoTime() - origin
        Transfer(SpeedMath.mbps(total, nanos), nanos)
    }

    private fun open(url: String, method: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 12_000
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-store")
            setRequestProperty("User-Agent", "PingPoint")
        }
    }

    private data class Transfer(val mbps: Double, val nanos: Long)
}
