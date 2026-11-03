package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Usługa ochrony ruchu. Silnik uruchamia się wyłącznie po zgodzie użytkownika
 * i tylko w osobnym buildzie testowym z aktywną flagą runtime.
 */
class BlockVpnService : VpnService() {
    companion object {
        const val ACTION_START = "pl.stophazard.app.action.START"
        const val ACTION_STOP = "pl.stophazard.app.action.STOP"
        private const val CHANNEL_ID = "stop_hazard_vpn"
        private const val NOTIFICATION_ID = 4108
        private const val PREFS = "stop_hazard_protection"
        private const val KEY_ACTIVE = "active"
    }

    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        if (intent?.action != ACTION_START) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startAsForegroundService()
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create(this)
        engine = selectedEngine

        val started = try {
            selectedEngine.start() && selectedEngine.isRunning()
        } catch (_: Throwable) {
            false
        }

        running.set(started)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, started).apply()

        if (!started) {
            engine = null
            stopForegroundCompat()
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

    private fun startAsForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "STOP HAZARD — ochrona", NotificationManager.IMPORTANCE_LOW),
            )
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("STOP HAZARD")
            .setContentText("Ochrona VPN jest aktywna")
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopExistingEngine() {
        val previousEngine = engine ?: return
        try {
            previousEngine.stop()
        } catch (_: Throwable) {
            // Cleanup failure must not keep the service alive.
        }
        engine = null
        running.set(false)
    }

    private fun stopVpn() {
        stopExistingEngine()
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
