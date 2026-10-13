package com.pingpoint.net

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Samples from the background monitor, for the screen that is still open. */
object MonitorRelay {
    private val ticksFlow = MutableSharedFlow<WatchTick>(extraBufferCapacity = 32)
    val ticks: SharedFlow<WatchTick> = ticksFlow.asSharedFlow()

    fun publish(tick: WatchTick) {
        ticksFlow.tryEmit(tick)
    }
}
