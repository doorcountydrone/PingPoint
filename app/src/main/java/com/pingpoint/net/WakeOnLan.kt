package com.pingpoint.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {
    private const val PORT = 9

    fun magicPacket(mac: String): ByteArray? {
        val normalized = SavedDeviceCodec.normalizeMac(mac) ?: return null
        val address = normalized.split(':').map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) {
            System.arraycopy(address, 0, packet, 6 + i * 6, 6)
        }
        return packet
    }

    suspend fun wake(mac: String): String = withContext(Dispatchers.IO) {
        val bytes = magicPacket(mac) ?: return@withContext "Enter a full MAC address to wake this device."
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val packet = DatagramPacket(
                bytes,
                bytes.size,
                InetAddress.getByName("255.255.255.255"),
                PORT,
            )
            socket.send(packet)
        }
        "Wake packet sent to ${SavedDeviceCodec.normalizeMac(mac)}"
    }
}
