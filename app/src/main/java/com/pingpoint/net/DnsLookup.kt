package com.pingpoint.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress

data class DnsRecord(val kind: String, val value: String)

object DnsLookup {
    fun isIpAddress(host: String): Boolean {
        if (host.contains(':')) return true
        val parts = host.split('.')
        return parts.size == 4 && parts.all { part -> part.toIntOrNull() in 0..255 }
    }

    fun forwardRecords(addresses: List<Pair<String, Boolean>>): List<DnsRecord> {
        return addresses.mapNotNull { (address, ipv4) ->
            val numeric = address.substringBefore('%').trim()
            if (numeric.isEmpty()) null else DnsRecord(if (ipv4) "IPv4" else "IPv6", numeric)
        }.distinct().sortedBy { if (it.kind == "IPv4") 0 else 1 }
    }

    fun reverseName(query: String, resolvedName: String): DnsRecord? {
        val name = resolvedName.trim().removeSuffix(".")
        if (name.isEmpty()) return null
        if (name.equals(query, ignoreCase = true)) return null
        if (name.substringBefore('%').equals(query.substringBefore('%'), ignoreCase = true)) return null
        return DnsRecord("Name", name)
    }

    suspend fun lookup(query: String): List<DnsRecord> = withContext(Dispatchers.IO) {
        if (isIpAddress(query)) {
            val address = InetAddress.getByName(query)
            listOfNotNull(reverseName(query, address.canonicalHostName))
        } else {
            val found = InetAddress.getAllByName(query).mapNotNull { address ->
                val numeric = address.hostAddress ?: return@mapNotNull null
                numeric to (address is Inet4Address)
            }
            forwardRecords(found)
        }
    }
}
