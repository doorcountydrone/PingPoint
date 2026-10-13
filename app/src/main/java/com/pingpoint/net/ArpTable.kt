package com.pingpoint.net

import java.io.File
import java.io.IOException

/**
 * Local MAC addresses learned by the phone after it talks to a device.
 * They only exist for hosts on the same network.
 */
object ArpTable {
    private val macPattern = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")

    fun readAll(): Map<String, String> {
        val file = File("/proc/net/arp")
        if (!file.exists()) return emptyMap()
        return try {
            file.readLines().drop(1).mapNotNull { line -> parseLine(line) }.toMap()
        } catch (_: IOException) {
            emptyMap()
        } catch (_: SecurityException) {
            emptyMap()
        }
    }

    fun macFor(ip: String): String? = readAll()[ip]

    private fun parseLine(line: String): Pair<String, String>? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 4) return null
        val flags = parts[2].removePrefix("0x").toIntOrNull(16) ?: 0
        if (flags and 0x2 == 0) return null
        val mac = parts[3].uppercase()
        if (mac == "00:00:00:00:00:00" || !macPattern.matches(mac)) return null
        return parts[0] to mac
    }
}
