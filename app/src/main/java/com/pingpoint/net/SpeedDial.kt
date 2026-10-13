package com.pingpoint.net

import kotlin.math.ln

object SpeedDial {
    const val MAX_MBPS = 1000.0

    /** Where the needle sits, from 0 at the left to 1 at the right. The scale is logarithmic. */
    fun fraction(mbps: Double): Float {
        if (mbps <= 0.0) return 0f
        val clamped = mbps.coerceAtMost(MAX_MBPS)
        return (ln(1.0 + clamped) / ln(1.0 + MAX_MBPS)).toFloat()
    }
}
