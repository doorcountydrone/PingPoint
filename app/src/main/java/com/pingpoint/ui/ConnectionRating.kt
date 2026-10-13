package com.pingpoint.ui

enum class ConnectionFit {
    Good,
    Fair,
    Poor,
}

data class ActivityRating(
    val activity: String,
    val fit: ConnectionFit,
)

/**
 * Bandwidth fit for everyday uses. Gaming lag still depends on ping time,
 * which this rating does not measure.
 */
fun rateConnection(downloadMbps: Double, uploadMbps: Double): List<ActivityRating> {
    return listOf(
        ActivityRating("Browsing", fit(downloadMbps, good = 5.0, fair = 1.0)),
        ActivityRating("HD streaming", fit(downloadMbps, good = 8.0, fair = 5.0)),
        ActivityRating("4K streaming", fit(downloadMbps, good = 25.0, fair = 15.0)),
        ActivityRating("Video calls", worse(fit(downloadMbps, 5.0, 2.0), fit(uploadMbps, 3.0, 1.0))),
        ActivityRating("Gaming", worse(fit(downloadMbps, 15.0, 3.0), fit(uploadMbps, 3.0, 1.0))),
    )
}

fun fitLabel(fit: ConnectionFit): String = when (fit) {
    ConnectionFit.Good -> "Good"
    ConnectionFit.Fair -> "Fair"
    ConnectionFit.Poor -> "Poor"
}

private fun fit(mbps: Double, good: Double, fair: Double): ConnectionFit = when {
    mbps >= good -> ConnectionFit.Good
    mbps >= fair -> ConnectionFit.Fair
    else -> ConnectionFit.Poor
}

private fun worse(first: ConnectionFit, second: ConnectionFit): ConnectionFit {
    return if (first.ordinal >= second.ordinal) first else second
}
