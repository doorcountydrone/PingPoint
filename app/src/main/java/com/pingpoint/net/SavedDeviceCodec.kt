package com.pingpoint.net

import com.pingpoint.model.SavedDevice

object SavedDeviceCodec {
    const val MAX_DEVICES = 40

    private val macPattern = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")

    fun normalizeMac(raw: String?): String? {
        val mac = raw?.trim()?.uppercase()?.replace('-', ':') ?: return null
        return mac.takeIf { macPattern.matches(it) }
    }

    fun normalizeName(name: String, host: String): String {
        val cleaned = name.trim().replace(Regex("\\s+"), " ").take(40)
        return cleaned.ifBlank { host }
    }

    fun upsert(devices: List<SavedDevice>, device: SavedDevice): List<SavedDevice> {
        val index = devices.indexOfFirst { it.host.equals(device.host, ignoreCase = true) }
        val next = if (index >= 0) {
            devices.toMutableList().apply { this[index] = device }
        } else {
            listOf(device) + devices
        }
        return next.take(MAX_DEVICES)
    }

    fun encode(devices: List<SavedDevice>): String {
        return devices.joinToString("\n") { device ->
            listOf(escape(device.host), escape(device.mac.orEmpty()), escape(device.name)).joinToString("\t")
        }
    }

    fun decode(raw: String): List<SavedDevice> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = splitFields(line)
            if (parts.size < 3) return@mapNotNull null
            val host = HostInput.normalize(unescape(parts[0])) ?: return@mapNotNull null
            val mac = normalizeMac(unescape(parts[1]))
            val name = unescape(parts[2]).ifBlank { host }
            SavedDevice(name = name, host = host, mac = mac)
        }.take(MAX_DEVICES).toList()
    }

    private fun splitFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var escaping = false
        for (char in line) {
            if (escaping) {
                current.append('\\')
                current.append(char)
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else if (char == '\t') {
                fields += current.toString()
                current.clear()
            } else {
                current.append(char)
            }
        }
        if (escaping) current.append('\\')
        fields += current.toString()
        return fields
    }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
    }

    private fun unescape(value: String): String = buildString {
        var escaping = false
        value.forEach { char ->
            if (escaping) {
                append(
                    when (char) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> char
                    }
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                append(char)
            }
        }
        if (escaping) append('\\')
    }
}
