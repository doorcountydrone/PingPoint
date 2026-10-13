package com.pingpoint.ui

enum class SideHealth {
    Waiting,
    Ok,
    Silent,
    Slow,
}

/** A side is down when at least half of the last few samples are bad. */
fun sideHealth(samples: List<Double?>): SideHealth {
    val recent = samples.takeLast(5)
    if (recent.size < 3) return SideHealth.Waiting
    val bad = recent.count { latencyBand(it) == LatencyBand.Bad }
    if (bad * 2 < recent.size) return SideHealth.Ok
    val silent = recent.count { it == null }
    return if (silent * 2 >= recent.size) SideHealth.Silent else SideHealth.Slow
}

/** True when this side just became silent or slow, including a change from one to the other. */
fun shouldAlert(previous: SideHealth, next: SideHealth): Boolean {
    val bad = next == SideHealth.Silent || next == SideHealth.Slow
    return bad && next != previous
}

fun shouldClearAlert(previous: SideHealth, next: SideHealth): Boolean {
    val wasBad = previous == SideHealth.Silent || previous == SideHealth.Slow
    return wasBad && next == SideHealth.Ok
}

fun alertMessage(label: String, health: SideHealth, recent: List<Double?>): String {
    return when (health) {
        SideHealth.Silent -> "$label stopped answering"
        SideHealth.Slow -> {
            val slow = recent.takeLast(5).lastOrNull { it != null && latencyBand(it) == LatencyBand.Bad }
            if (slow != null) "$label is staying slow · ${formatMs(slow)}" else "$label is staying slow"
        }
        SideHealth.Waiting, SideHealth.Ok -> ""
    }
}
