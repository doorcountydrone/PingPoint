package com.pingpoint.net

import com.pingpoint.model.PingSample
import com.pingpoint.model.PingSummary

object PingOutputParser {
    private val timePattern = Regex("""time[=<]\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val bytesPattern = Regex("""(\d+)\s+bytes""", RegexOption.IGNORE_CASE)
    private val ttlPattern = Regex("""ttl[=](\d+)""", RegexOption.IGNORE_CASE)
    private val seqPattern = Regex("""(?:icmp_seq|seq)[=](\d+)""", RegexOption.IGNORE_CASE)
    private val fromPattern = Regex(
        """from\s+(.+?):\s+(?:icmp_seq|seq)=""",
        RegexOption.IGNORE_CASE,
    )
    private val parenIpPattern = Regex("""\(([0-9a-fA-F:.]+)\)""")

    fun parseSample(line: String): PingSample? {
        val timeMatch = timePattern.find(line) ?: return null
        val rtt = timeMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val fromRaw = fromPattern.find(line)?.groupValues?.get(1)?.trim().orEmpty()
        val from = parenIpPattern.find(fromRaw)?.groupValues?.get(1) ?: fromRaw
        return PingSample(
            sequence = seqPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            bytes = bytesPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            ttl = ttlPattern.find(line)?.groupValues?.get(1)?.toIntOrNull(),
            rttMs = rtt,
            rttIsUpperBound = timeMatch.value.contains('<'),
            from = from,
        )
    }

    fun failureMessage(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        return when {
            "unknown host" in lower || "name or service not known" in lower ||
                "no address associated" in lower -> "Could not find that address."
            "network is unreachable" in lower -> "Network is unreachable."
            "permission denied" in lower || "operation not permitted" in lower ->
                "This phone blocked the ping."
            lower.startsWith("ping:") && "usage" !in lower ->
                trimmed.removePrefix("ping:").removePrefix("Ping:").trim()
            else -> null
        }
    }

    fun summarize(host: String, transmitted: Int, samples: List<PingSample>): PingSummary {
        val received = samples.size
        val loss = if (transmitted <= 0) {
            0
        } else {
            ((transmitted - received).coerceAtLeast(0) * 100) / transmitted
        }
        val times = samples.map { it.rttMs }
        return PingSummary(
            host = host,
            transmitted = transmitted,
            received = received,
            lossPercent = loss,
            minMs = times.minOrNull(),
            avgMs = times.takeIf { it.isNotEmpty() }?.average(),
            maxMs = times.maxOrNull(),
        )
    }
}
