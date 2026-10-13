package com.pingpoint.net

object HostInput {
    private val pattern = Regex("^[A-Za-z0-9._:\\[\\]-]{1,253}$")

    fun normalize(raw: String): String? {
        var host = raw.trim()
        if (host.startsWith("[") && host.endsWith("]") && host.length > 2) {
            host = host.substring(1, host.length - 1)
        }
        if (host.isEmpty() || host.startsWith("-") || host.startsWith(".")) return null
        if (!pattern.matches(host)) return null
        return host
    }
}
