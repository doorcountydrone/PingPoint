package com.pingpoint.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.pingpoint.model.LocalNetworkInfo
import java.net.Inet4Address

object LocalNetwork {
    fun read(context: Context): LocalNetworkInfo {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork
        val capabilities = network?.let { connectivity.getNetworkCapabilities(it) }
        val link = network?.let { connectivity.getLinkProperties(it) }

        val transport = when {
            capabilities == null -> "Offline"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Other"
        }

        val ipv4Link = link?.linkAddresses?.firstOrNull { linkAddress ->
            val address = linkAddress.address
            address is Inet4Address && !address.isLoopbackAddress
        }
        val ipv4 = (ipv4Link?.address as? Inet4Address)?.hostAddress
        val prefix = ipv4Link?.prefixLength
        val gateway = link?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress
        val scanCidr = if (ipv4 != null && prefix != null) Subnet.scanCidr(ipv4, prefix) else null
        val onLan = transport == "Wi-Fi" || transport == "Ethernet"
        val canScan = onLan && ipv4 != null && prefix != null
        val statusNote = when {
            transport == "Offline" -> "No network connection."
            transport == "Cellular" ->
                "Connect to Wi-Fi to find devices and their MAC addresses. You can still ping an address."
            ipv4 == null -> "This network has no IPv4 address."
            else -> null
        }

        return LocalNetworkInfo(
            ipv4 = ipv4,
            prefixLength = prefix,
            scanCidr = scanCidr,
            gateway = gateway,
            gatewayMac = gateway?.let { ArpTable.macFor(it) },
            interfaceName = link?.interfaceName,
            transport = transport,
            canScan = canScan,
            statusNote = statusNote,
        )
    }
}
