package com.pingpoint

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.pingpoint.net.MonitorRelay
import com.pingpoint.net.PingEngine
import com.pingpoint.net.WatchTick
import com.pingpoint.ui.SideHealth
import com.pingpoint.ui.alertMessage
import com.pingpoint.ui.formatMs
import com.pingpoint.ui.shouldAlert
import com.pingpoint.ui.shouldClearAlert
import com.pingpoint.ui.sideHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var runId = 0
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        MonitorNotifier.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            if (intent?.action == ACTION_STOP) MonitorRelay.publish(WatchTick.Stopped)
            finish()
            return START_NOT_STICKY
        }
        val host = intent.getStringExtra(EXTRA_HOST)
        if (host.isNullOrBlank()) {
            finish()
            return START_NOT_STICKY
        }
        val internet = intent.getStringExtra(EXTRA_INTERNET)?.takeIf { it.isNotBlank() }
        val nearLabel = intent.getStringExtra(EXTRA_NEAR_LABEL) ?: host
        val farLabel = intent.getStringExtra(EXTRA_FAR_LABEL)
        val myRun = ++runId
        startForeground(
            MonitorNotifier.ONGOING_ID,
            MonitorNotifier.ongoing(this, watchingText(nearLabel, farLabel)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        inForeground = true
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            try {
                watch(myRun, host, internet, nearLabel, farLabel)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (myRun == runId) {
                    MonitorRelay.publish(WatchTick.Failed(error.message ?: "Monitor failed."))
                    finish()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runId++
        job?.cancel()
        inForeground = false
        MonitorNotifier.cancelAlerts(this)
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun watch(
        myRun: Int,
        host: String,
        internet: String?,
        nearLabel: String,
        farLabel: String?,
    ) {
        val nearSamples = ArrayDeque<Double?>()
        val farSamples = ArrayDeque<Double?>()
        var nearHealth = SideHealth.Waiting
        var farHealth = SideHealth.Waiting
        PingEngine.watchBoth(host, internet).collect { tick ->
            if (myRun != runId) return@collect
            when (tick) {
                is WatchTick.Round -> {
                    MonitorRelay.publish(tick)
                    nearSamples.addLast(tick.primaryRtt)
                    trim(nearSamples)
                    nearHealth = track(MonitorNotifier.NEAR_ALERT_ID, nearLabel, nearSamples, nearHealth)
                    if (tick.compare && farLabel != null) {
                        farSamples.addLast(tick.internetRtt)
                        trim(farSamples)
                        farHealth = track(MonitorNotifier.FAR_ALERT_ID, farLabel, farSamples, farHealth)
                    }
                    MonitorNotifier.updateOngoing(
                        this,
                        statusText(nearLabel, tick.primaryRtt, farLabel, tick.internetRtt, tick.compare),
                    )
                }
                is WatchTick.Failed -> {
                    MonitorRelay.publish(tick)
                    finish()
                }
                WatchTick.Stopped -> Unit
            }
        }
    }

    private fun track(id: Int, label: String, samples: ArrayDeque<Double?>, previous: SideHealth): SideHealth {
        val recent = samples.toList()
        val next = sideHealth(recent)
        if (shouldAlert(previous, next)) {
            MonitorNotifier.alert(this, id, alertMessage(label, next, recent))
        } else if (shouldClearAlert(previous, next)) {
            MonitorNotifier.cancelAlert(this, id)
        }
        return next
    }

    private fun finish() {
        runId++
        job?.cancel()
        job = null
        if (inForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            inForeground = false
        }
        stopSelf()
    }

    companion object {
        const val ACTION_STOP = "com.pingpoint.STOP_MONITOR"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_INTERNET = "internet"
        private const val EXTRA_NEAR_LABEL = "near_label"
        private const val EXTRA_FAR_LABEL = "far_label"

        fun start(context: Context, host: String, internet: String?, nearLabel: String, farLabel: String?) {
            MonitorNotifier.ensureChannels(context)
            MonitorNotifier.cancelAlerts(context)
            val intent = Intent(context, MonitorService::class.java).apply {
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_INTERNET, internet)
                putExtra(EXTRA_NEAR_LABEL, nearLabel)
                putExtra(EXTRA_FAR_LABEL, farLabel)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}

private fun trim(samples: ArrayDeque<Double?>) {
    while (samples.size > 5) samples.removeFirst()
}

private fun watchingText(nearLabel: String, farLabel: String?): String {
    return if (farLabel.isNullOrBlank()) "Watching $nearLabel" else "Watching $nearLabel and $farLabel"
}

private fun statusText(
    nearLabel: String,
    nearRtt: Double?,
    farLabel: String?,
    farRtt: Double?,
    compare: Boolean,
): String {
    val near = "$nearLabel ${reading(nearRtt)}"
    if (!compare || farLabel.isNullOrBlank()) return near
    return "$near · $farLabel ${reading(farRtt)}"
}

private fun reading(rttMs: Double?): String = if (rttMs == null) "no reply" else formatMs(rttMs)
