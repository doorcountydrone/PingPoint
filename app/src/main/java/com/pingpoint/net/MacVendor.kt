package com.pingpoint.net

sealed interface MacLabel {
    data class Maker(val name: String) : MacLabel
    data object Private : MacLabel
    data object Unknown : MacLabel
}

object MacVendor {
    private val prefix = Regex(
        "^([0-9A-Fa-f]{2})[:\\-]([0-9A-Fa-f]{2})[:\\-]([0-9A-Fa-f]{2})",
    )

    fun identify(mac: String?, vendors: Map<String, String>): MacLabel {
        val key = ouiKey(mac) ?: return MacLabel.Unknown
        val first = key.substring(0, 2).toInt(16)
        if (first and 0x01 != 0) return MacLabel.Unknown
        if (first and 0x02 != 0) return MacLabel.Private
        val name = vendors[key] ?: return MacLabel.Unknown
        return MacLabel.Maker(name)
    }

    fun maker(mac: String?, vendors: Map<String, String>): String? {
        return (identify(mac, vendors) as? MacLabel.Maker)?.name
    }

    /** Text for the MAC line. The maker is omitted when it is already the row title. */
    fun macLine(mac: String?, vendors: Map<String, String>, makerAlreadyShown: Boolean = false): String {
        if (mac.isNullOrBlank()) return "MAC unavailable"
        return when (val label = identify(mac, vendors)) {
            is MacLabel.Maker -> if (makerAlreadyShown) mac else "${label.name} · $mac"
            MacLabel.Private -> "Private MAC · $mac"
            MacLabel.Unknown -> mac
        }
    }

    private fun ouiKey(mac: String?): String? {
        if (mac.isNullOrBlank()) return null
        val match = prefix.find(mac.trim()) ?: return null
        return (match.groupValues[1] + match.groupValues[2] + match.groupValues[3]).uppercase()
    }
}
