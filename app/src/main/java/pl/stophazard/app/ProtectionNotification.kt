package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ProtectionNotification {
    const val CHANNEL_ID = "stop_hazard_protection"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "STOP HAZARD — ochrona",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Informuje, że ochrona STOP HAZARD działa."
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(context: Context, protected: Boolean): Notification {
        val text = if (protected) {
            "Ochrona jest aktywna"
        } else {
            "Ochrona jest wyłączona"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("STOP HAZARD")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(protected)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setContentTitle("STOP HAZARD")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(protected)
                .build()
        }
    }
}
