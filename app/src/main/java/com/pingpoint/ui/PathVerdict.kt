package com.pingpoint.ui

import com.pingpoint.model.MonitorPoint

enum class PathVerdict {
    Waiting,
    BothFine,
    BeyondRouter,
    LocalPath,
    RouterOnly,
}

/**
 * Compares the last few paired samples.
 * A side counts as down when at least half of those samples are bad.
 */
fun pathVerdict(points: List<MonitorPoint>): PathVerdict {
    val recent = points.takeLast(5)
    if (recent.size < 3) return PathVerdict.Waiting
    val localDown = recent.count { latencyBand(it.rttMs) == LatencyBand.Bad } * 2 >= recent.size
    val internetDown = recent.count { latencyBand(it.internetRttMs) == LatencyBand.Bad } * 2 >= recent.size
    return when {
        localDown && internetDown -> PathVerdict.LocalPath
        internetDown -> PathVerdict.BeyondRouter
        localDown -> PathVerdict.RouterOnly
        else -> PathVerdict.BothFine
    }
}

fun seriesLabel(host: String?, gateway: String?, fallback: String): String {
    if (host.isNullOrBlank()) return fallback
    if (gateway != null && host.equals(gateway, ignoreCase = true)) return "Router"
    return host
}

fun verdictText(verdict: PathVerdict, near: String, far: String): String = when (verdict) {
    PathVerdict.Waiting -> "A few more packets, then this can tell $near from $far."
    PathVerdict.BothFine -> "$near and $far both look fine."
    PathVerdict.BeyondRouter -> "$near answers. $far does not, so the problem is past $near."
    PathVerdict.LocalPath -> "$near and $far are both bad, so the problem is on the path to $near."
    PathVerdict.RouterOnly -> "$far answers, and $near does not."
}
