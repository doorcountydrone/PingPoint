package com.pingpoint.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pingpoint.ui.theme.Fair
import com.pingpoint.ui.theme.Signal
import com.pingpoint.ui.theme.SignalDark
import com.pingpoint.ui.theme.Sky
import com.pingpoint.ui.theme.SkyDark
import com.pingpoint.ui.theme.Slow
import com.pingpoint.ui.theme.isAppInDarkTheme
import java.util.Locale

fun formatMbps(mbps: Double): String {
    val number = if (mbps < 10) {
        String.format(Locale.US, "%.2f", mbps)
    } else {
        String.format(Locale.US, "%.1f", mbps)
    }
    return "$number Mb/s"
}

fun formatMs(ms: Double, upperBound: Boolean = false): String {
    val number = if (ms < 10) {
        String.format(Locale.US, "%.2f", ms)
    } else {
        String.format(Locale.US, "%.1f", ms)
    }
    return "${if (upperBound) "<" else ""}$number ms"
}

fun latencyLabel(ms: Double): String = when {
    ms < 20 -> "Fast"
    ms < 60 -> "Good"
    ms < 150 -> "Fair"
    else -> "Slow"
}

enum class LatencyBand {
    Good,
    Fair,
    Bad,
}

/** Good is under 60 ms, fair is 60–150 ms, and bad is 150 ms or a lost packet. */
fun latencyBand(rttMs: Double?): LatencyBand = when {
    rttMs == null -> LatencyBand.Bad
    rttMs < 60.0 -> LatencyBand.Good
    rttMs < 150.0 -> LatencyBand.Fair
    else -> LatencyBand.Bad
}

fun bandLabel(band: LatencyBand): String = when (band) {
    LatencyBand.Good -> "Good"
    LatencyBand.Fair -> "Fair"
    LatencyBand.Bad -> "Bad"
}

@Composable
fun latencyColor(ms: Double): Color {
    val dark = isAppInDarkTheme()
    return when {
        ms < 20 -> if (dark) Signal else SignalDark
        ms < 60 -> if (dark) Sky else SkyDark
        ms < 150 -> if (dark) Fair else Color(0xFF8A5A00)
        else -> if (dark) Slow else Color(0xFFBA1A1A)
    }
}
