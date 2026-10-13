package com.pingpoint.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.wifi.WifiManager
import android.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object DeviceNameResolver {
    private val dnsExecutor = Executors.newCachedThreadPool()

    suspend fun lookup(context: Context, ip: String): String? {
        if (!Subnet.isPrivateLan(ip)) return null
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val advertised = async { queryAdvertisedName(context, ip) }
                val dns = async { dnsPtr(context, ip) }
                val fromDevice = advertised.await()
                if (!fromDevice.isNullOrBlank()) {
                    dns.cancel()
                    fromDevice
                } else {
                    dns.await()
                }
            }
        }
    }

    private fun queryAdvertisedName(context: Context, ip: String): String? {
        val query = DeviceNameParser.ptrQuery(ip, requestUnicast = true) ?: return null
        val netbios = DeviceNameParser.netbiosQuery()
        val lock = multicastLock(context)
        val socket = DatagramSocket()
        try {
            lock?.acquire()
            socket.soTimeout = 250
            val host = InetAddress.getByName(ip)
            sendQuietly(socket, DatagramPacket(query, query.size, host, 5353))
            sendQuietly(socket, DatagramPacket(query, query.size, InetAddress.getByName("224.0.0.251"), 5353))
            sendQuietly(socket, DatagramPacket(netbios, netbios.size, host, 137))
            val deadline = System.nanoTime() + 700_000_000L
            val buffer = ByteArray(2048)
            while (System.nanoTime() < deadline) {
                val remainingMs = ((deadline - System.nanoTime()) / 1_000_000L).toInt().coerceAtLeast(1)
                socket.soTimeout = remainingMs
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    break
                }
                val data = buffer.copyOf(packet.length)
                val name = if (packet.port == 137) {
                    DeviceNameParser.parseNetbios(data)
                } else {
                    DeviceNameParser.parsePtr(data)
                }
                if (name != null) return name
            }
            return null
        } catch (_: Exception) {
            return null
        } finally {
            socket.close()
            if (lock?.isHeld == true) lock.release()
        }
    }

    private suspend fun dnsPtr(context: Context, ip: String): String? {
        val query = DeviceNameParser.ptrQuery(ip, requestUnicast = false) ?: return null
        val network = context.getSystemService(ConnectivityManager::class.java).activeNetwork
        return suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            val finished = AtomicBoolean(false)
            continuation.invokeOnCancellation { signal.cancel() }
            fun finish(name: String?) {
                if (finished.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(name)
                }
            }
            try {
                DnsResolver.getInstance().rawQuery(
                    network,
                    query,
                    DnsResolver.FLAG_NO_RETRY or DnsResolver.FLAG_NO_CACHE_LOOKUP,
                    dnsExecutor,
                    signal,
                    object : DnsResolver.Callback<ByteArray> {
                        override fun onAnswer(answer: ByteArray, rcode: Int) {
                            finish(if (rcode == 0) DeviceNameParser.parsePtr(answer) else null)
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            finish(null)
                        }
                    },
                )
            } catch (_: Exception) {
                finish(null)
            }
        }
    }

    private fun sendQuietly(socket: DatagramSocket, packet: DatagramPacket) {
        try {
            socket.send(packet)
        } catch (_: Exception) {
            // One query path can fail without giving up on the others.
        }
    }

    private fun multicastLock(context: Context): WifiManager.MulticastLock? {
        return try {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
            wifi.createMulticastLock("pingpoint-names").apply { setReferenceCounted(true) }
        } catch (_: Exception) {
            null
        }
    }
}
