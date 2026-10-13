package com.pingpoint

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object MonitorNotifier {
    const val ONGOING_ID = 40
    const val NEAR_ALERT_ID = 41
    const val FAR_ALERT_ID = 42

    private const val ONGOING_CHANNEL = "monitor"
    private const val ALERT_CHANNEL = "monitor_alerts"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val ongoing = NotificationChannel(
            ONGOING_CHANNEL,
            "Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows while PingPoint is watching an address"
        }
        val alerts = NotificationChannel(
            ALERT_CHANNEL,
            "Monitor alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Fires when a watched address stops answering or stays slow"
        }
        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(alerts)
    }

    fun ongoing(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, ONGOING_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle("PingPoint")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openApp(context))
            .addAction(0, "Stop", stopIntent(context))
            .build()
    }

    fun alert(context: Context, id: Int, text: String) {
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle("PingPoint")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context))
            .build()
        manager(context).notify(id, notification)
    }

    fun updateOngoing(context: Context, text: String) {
        manager(context).notify(ONGOING_ID, ongoing(context, text))
    }

    fun cancelAlerts(context: Context) {
        val manager = manager(context)
        manager.cancel(NEAR_ALERT_ID)
        manager.cancel(FAR_ALERT_ID)
    }

    fun cancelAlert(context: Context, id: Int) {
        manager(context).cancel(id)
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            pendingFlags(),
        )
    }

    private fun stopIntent(context: Context): PendingIntent {
        val intent = Intent(context, MonitorService::class.java).setAction(MonitorService.ACTION_STOP)
        return PendingIntent.getService(context, 2, intent, pendingFlags())
    }

    private fun pendingFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun manager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }
}
