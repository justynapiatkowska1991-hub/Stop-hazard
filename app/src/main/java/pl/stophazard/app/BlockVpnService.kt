package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class BlockVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null
    private var heartbeatThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        installCrashDiagnostics()
        startProtectionNotification("Uruchamianie ochrony…")
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create(this)
        engine = selectedEngine

        val started = try {
            selectedEngine.start()
        } catch (t: Throwable) {
            Log.e(TAG, "ENGINE_START_FAILED", t)
            writeState("START_FAILED:" + t.javaClass.simpleName)
            false
        }

        running.set(started)

        if (!started) {
            engine = null
            stopHeartbeat()
            startProtectionNotification("Nie udało się uruchomić ochrony VPN. Spróbuj ponownie.")
            writeState("STOPPED_START_FAILED")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        writeState("RUNNING")
        startHeartbeat()
        startProtectionNotification("Ochrona stron hazardowych jest aktywna")
        return START_STICKY
    }

    override fun onRevoke() {
        writeState("VPN_REVOKED")
        startProtectionNotification("VPN został cofnięty przez Androida")
        Log.e(TAG, "VPN_REVOKED")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        writeState(if (running.get()) "DESTROYED_WHILE_RUNNING" else "DESTROYED")
        stopHeartbeat()
        stopVpn()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

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
        } catch (t: Throwable) {
            Log.w(TAG, "ENGINE_STOP_FAILED", t)
        }
        engine = null
        running.set(false)
    }

    private fun installCrashDiagnostics() {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Log.e(TAG, "UNCAUGHT_EXCEPTION thread=" + thread.name, error)
            writeState("UNCAUGHT_EXCEPTION:" + error.javaClass.simpleName)
            runCatching {
                startProtectionNotification("Błąd silnika VPN: " + error.javaClass.simpleName)
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatThread = thread(name = "stop-hazard-heartbeat", isDaemon = true) {
            while (running.get()) {
                writeState("HEARTBEAT")
                try {
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatThread?.interrupt()
        heartbeatThread = null
    }

    private fun writeState(state: String) {
        runCatching {
            File(filesDir, "vpn_state.txt").writeText(
                System.currentTimeMillis().toString() + "|" + state,
            )
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
