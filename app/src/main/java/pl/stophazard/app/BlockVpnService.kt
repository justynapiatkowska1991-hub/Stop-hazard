package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class BlockVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        installCrashDiagnostics()
        startProtectionNotification("Uruchamianie ochrony…")
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create(this)
        engine = selectedEngine

        var failure: Throwable? = null
        val started = try {
            selectedEngine.start()
        } catch (t: Throwable) {
            failure = t
            false
        }

        running.set(started)

        if (!started) {
            engine = null
            startProtectionNotification(
                "Nie udało się uruchomić ochrony VPN. Spróbuj ponownie.",
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startProtectionNotification("Ochrona stron hazardowych jest aktywna")
        return START_STICKY
    }

    override fun onRevoke() {
        startProtectionNotification("VPN został cofnięty przez Androida")
        Log.e(TAG, "VPN_REVOKED")
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

    private fun startProtectionNotification(text: String) {
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
            .setContentText(text)
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

    private fun installCrashDiagnostics() {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Log.e(TAG, "UNCAUGHT_EXCEPTION thread=${thread.name}", error)
            runCatching {
                startProtectionNotification(
                    "Błąd silnika VPN: ${error.javaClass.simpleName}",
                )
            }
        }
    }

    companion object {
        private const val TAG = "STOP_HAZARD_VPN"
    }

    private fun stopVpn() {
        stopExistingEngine()
        stopSelf()
    }
}
