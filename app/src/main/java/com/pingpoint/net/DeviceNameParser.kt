package com.pingpoint.net

import java.io.ByteArrayOutputStream

object DeviceNameParser {
    fun ptrQuery(ip: String, requestUnicast: Boolean): ByteArray? {
        val parts = ip.split('.')
        if (parts.size != 4 || parts.any { it.toIntOrNull() !in 0..255 }) return null
        val labels = listOf(parts[3], parts[2], parts[1], parts[0], "in-addr", "arpa")
        val out = ByteArrayOutputStream()
        out.writeU16(0x0001)
        out.writeU16(0x0000)
        out.writeU16(1)
        out.writeU16(0)
        out.writeU16(0)
        out.writeU16(0)
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.writeU16(12)
        out.writeU16(if (requestUnicast) 0x8001 else 0x0001)
        return out.toByteArray()
    }

    fun netbiosQuery(): ByteArray {
        val encoded = "CKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        val out = ByteArrayOutputStream()
        out.writeU16(0x1234)
        out.writeU16(0x0000)
        out.writeU16(1)
        out.writeU16(0)
        out.writeU16(0)
        out.writeU16(0)
        out.write(32)
        out.write(encoded.toByteArray(Charsets.US_ASCII))
        out.write(0)
        out.writeU16(0x0021)
        out.writeU16(0x0001)
        return out.toByteArray()
    }

    fun parsePtr(data: ByteArray): String? {
        if (data.size < 12) return null
        val questions = u16(data, 4) ?: return null
        val answers = u16(data, 6) ?: return null
        val authorities = u16(data, 8) ?: return null
        val additional = u16(data, 10) ?: return null
        var offset = 12
        repeat(questions) {
            offset = skipName(data, offset) ?: return null
            offset += 4
        }
        repeat(answers + authorities + additional) {
            offset = skipName(data, offset) ?: return null
            if (offset + 10 > data.size) return null
            val type = u16(data, offset) ?: return null
            val length = u16(data, offset + 8) ?: return null
            offset += 10
            if (offset + length > data.size) return null
            if (type == 12) {
                val name = readName(data, offset)?.first?.let { clean(it) }
                if (name != null) return name
            }
            offset += length
        }
        return null
    }

    fun parseNetbios(data: ByteArray): String? {
        if (data.size < 12) return null
        val questions = u16(data, 4) ?: return null
        val answers = u16(data, 6) ?: return null
        var offset = 12
        repeat(questions) {
            offset = skipName(data, offset) ?: return null
            offset += 4
        }
        repeat(answers) {
            offset = skipName(data, offset) ?: return null
            if (offset + 10 > data.size) return null
            val type = u16(data, offset) ?: return null
            val length = u16(data, offset + 8) ?: return null
            offset += 10
            if (offset + length > data.size) return null
            if (type == 0x21) {
                parseNetbiosNames(data, offset, length)?.let { return it }
            }
            offset += length
        }
        return null
    }

    fun clean(raw: String): String? {
        var name = raw.trim().trimEnd('.')
        if (name.endsWith(".local", ignoreCase = true)) {
            name = name.dropLast(".local".length).trimEnd('.')
        }
        if (name.isBlank() || name.any { it.isISOControl() }) return null
        if (Subnet.parseIpv4(name) != null) return null
        if (name.equals("localhost", ignoreCase = true)) return null
        return name
    }

    private fun parseNetbiosNames(data: ByteArray, start: Int, length: Int): String? {
        if (length < 1 || start >= data.size) return null
        val count = data[start].toInt() and 0xFF
        var pos = start + 1
        var workstation: String? = null
        var fallback: String? = null
        repeat(count) {
            if (pos + 18 > start + length || pos + 18 > data.size) return workstation ?: fallback
            val suffix = data[pos + 15].toInt() and 0xFF
            val flags = u16(data, pos + 16) ?: return workstation ?: fallback
            val group = flags and 0x8000 != 0
            val name = data.decodeToString(pos, pos + 15).trim()
            if (!group && name.isNotEmpty()) {
                if (suffix == 0x00 && workstation == null) workstation = name
                if (fallback == null && suffix !in setOf(0x1B, 0x1C, 0x1D, 0x1E)) fallback = name
            }
            pos += 18
        }
        return workstation ?: fallback
    }

    private fun skipName(data: ByteArray, offset: Int): Int? = readName(data, offset)?.second

    private fun readName(data: ByteArray, start: Int, jumps: Int = 0): Pair<String, Int>? {
        if (jumps > 8 || start !in data.indices) return null
        val labels = ArrayList<String>()
        var pos = start
        var end = start
        var jumped = false
        while (pos < data.size) {
            val length = data[pos].toInt() and 0xFF
            when {
                length == 0 -> {
                    if (!jumped) end = pos + 1
                    return labels.joinToString(".") to end
                }
                length and 0xC0 == 0xC0 -> {
                    if (pos + 1 >= data.size) return null
                    val pointer = ((length and 0x3F) shl 8) or (data[pos + 1].toInt() and 0xFF)
                    if (!jumped) end = pos + 2
                    val pointed = readName(data, pointer, jumps + 1) ?: return null
                    if (pointed.first.isNotEmpty()) labels += pointed.first
                    return labels.joinToString(".") to end
                }
                else -> {
                    if (pos + 1 + length > data.size) return null
                    labels += data.decodeToString(pos + 1, pos + 1 + length)
                    pos += 1 + length
                    if (!jumped) end = pos
                }
            }
        }
        return null
    }

    private fun u16(data: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset + 1 >= data.size) return null
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun ByteArrayOutputStream.writeU16(value: Int) {
        write((value shr 8) and 0xFF)
        write(value and 0xFF)
    }
}
