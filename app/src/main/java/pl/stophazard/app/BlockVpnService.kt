package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean

class BlockVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startProtectionNotification()
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create(this)
        engine = selectedEngine

        val started = try {
            selectedEngine.start()
        } catch (_: Throwable) {
            false
        }

        running.set(started)

        if (!started) {
            engine = null
            stopSelf(startId)
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    private fun startProtectionNotification() {
        val channelId = "stop_hazard_protection"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "STOP HAZARD — ochrona",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("STOP HAZARD")
            .setContentText("Ochrona stron hazardowych jest aktywna")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun stopExistingEngine() {
        val previousEngine = engine ?: return
        try {
            previousEngine.stop()
        } catch (_: Throwable) {
        }
        engine = null
        running.set(false)
    }

    private fun stopVpn() {
        stopExistingEngine()
        stopSelf()
    }
}
