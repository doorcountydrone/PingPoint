package com.pingpoint.net

object AppVersion {
    const val DISMISS_RESHOW_OPENS = 3

    /**
     * After Not now, the same update stays hidden until [opensSinceDismiss] reaches 3.
     * A still-newer release always shows right away.
     */
    fun shouldOfferUpdate(
        latest: String,
        installed: String,
        dismissed: String?,
        opensSinceDismiss: Int,
    ): Boolean {
        if (!isNewer(latest, installed)) return false
        if (dismissed == null || isNewer(latest, dismissed)) return true
        return opensSinceDismiss >= DISMISS_RESHOW_OPENS
    }

    /** True when [latest] is a higher dotted version than [current], ignoring a leading v. */
    fun isNewer(latest: String, current: String): Boolean {
        val left = parts(latest)
        val right = parts(current)
        val length = maxOf(left.size, right.size)
        for (index in 0 until length) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    fun normalize(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    private fun parts(raw: String): List<Int> {
        return normalize(raw)
            .split('.', '-', '_')
            .mapNotNull { piece -> piece.takeWhile { it.isDigit() }.toIntOrNull() }
    }
}
