package com.pingpoint.net

import com.pingpoint.model.HistoryEntry
import com.pingpoint.model.MonitorHistory
import com.pingpoint.model.SpeedHistory

object HistoryCodec {
    const val MAX_ENTRIES = 30

    fun encode(entries: List<HistoryEntry>): String {
        return entries.joinToString("\n") { entry ->
            when (entry) {
                is SpeedHistory -> listOf(
                    "S",
                    entry.atMillis.toString(),
                    entry.downloadMbps.toString(),
                    entry.uploadMbps.toString(),
                ).joinToString("\t")
                is MonitorHistory -> listOf(
                    "M",
                    entry.atMillis.toString(),
                    escape(entry.host),
                    entry.avgMs?.toString().orEmpty(),
                    entry.lossPercent.toString(),
                    entry.sent.toString(),
                    escape(entry.internetHost.orEmpty()),
                    entry.internetAvgMs?.toString().orEmpty(),
                    entry.internetLossPercent?.toString().orEmpty(),
                ).joinToString("\t")
            }
        }
    }

    fun decode(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split('\t')
            when (parts.getOrNull(0)) {
                "S" -> {
                    if (parts.size < 4) return@mapNotNull null
                    SpeedHistory(
                        atMillis = parts[1].toLongOrNull() ?: return@mapNotNull null,
                        downloadMbps = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                        uploadMbps = parts[3].toDoubleOrNull() ?: return@mapNotNull null,
                    )
                }
                "M" -> {
                    if (parts.size < 6) return@mapNotNull null
                    MonitorHistory(
                        atMillis = parts[1].toLongOrNull() ?: return@mapNotNull null,
                        host = unescape(parts[2]),
                        avgMs = parts[3].toDoubleOrNull(),
                        lossPercent = parts[4].toIntOrNull() ?: return@mapNotNull null,
                        sent = parts[5].toIntOrNull() ?: return@mapNotNull null,
                        internetHost = parts.getOrNull(6)?.let(::unescape)?.takeIf { it.isNotBlank() },
                        internetAvgMs = parts.getOrNull(7)?.toDoubleOrNull(),
                        internetLossPercent = parts.getOrNull(8)?.toIntOrNull(),
                    )
                }
                else -> null
            }
        }.take(MAX_ENTRIES).toList()
    }

    fun prepend(entries: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> {
        return (listOf(entry) + entries).take(MAX_ENTRIES)
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")

    private fun unescape(value: String): String = buildString {
        var i = 0
        while (i < value.length) {
            val char = value[i]
            if (char == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> append('\\')
                    't' -> append('\t')
                    'n' -> append('\n')
                    else -> {
                        append('\\')
                        append(value[i + 1])
                    }
                }
                i += 2
            } else {
                append(char)
                i++
            }
        }
    }
}
