package com.pingpoint.net

object Subnet {
    fun parseIpv4(ip: String): Long? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val n = part.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            result = (result shl 8) or n.toLong()
        }
        return result
    }

    fun formatIpv4(value: Long): String {
        val v = value and 0xFFFFFFFFL
        return "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"
    }

    fun isPrivateLan(ip: String): Boolean {
        val value = parseIpv4(ip) ?: return false
        val a = ((value shr 24) and 0xFF).toInt()
        val b = ((value shr 16) and 0xFF).toInt()
        return a == 10 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254)
    }

    /** Home scans stay on one /24 so a /16 Wi-Fi network does not fan out across the whole private range. */
    fun effectivePrefix(prefixLength: Int): Int = when {
        prefixLength < 24 -> 24
        prefixLength > 30 -> 30
        else -> prefixLength
    }

    fun prefixMask(prefix: Int): Long {
        if (prefix <= 0) return 0L
        if (prefix >= 32) return 0xFFFFFFFFL
        val hostBits = 32 - prefix
        return (0xFFFFFFFFL ushr hostBits) shl hostBits
    }

    fun scanCidr(localIp: String, prefixLength: Int): String? {
        val ip = parseIpv4(localIp) ?: return null
        val prefix = effectivePrefix(prefixLength)
        val network = ip and prefixMask(prefix)
        return "${formatIpv4(network)}/$prefix"
    }

    fun hostsForScan(localIp: String, prefixLength: Int): List<String> {
        val ip = parseIpv4(localIp) ?: return emptyList()
        val prefix = effectivePrefix(prefixLength)
        val mask = prefixMask(prefix)
        val network = ip and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val hosts = ArrayList<String>()
        var addr = network + 1
        while (addr < broadcast) {
            if (addr != ip) hosts.add(formatIpv4(addr))
            addr++
        }
        return hosts
    }
}
