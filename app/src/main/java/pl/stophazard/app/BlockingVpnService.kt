package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.IOException

class BlockingVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            establishVpnInterface()
            startPacketLoop()
        }
        return START_STICKY
    }

    private fun establishVpnInterface() {
        if (vpnInterface != null) return
        vpnInterface = Builder()
            .setSession("STOP HAZARD")
            .addAddress("10.10.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)
            .establish()
    }

    private fun startPacketLoop() {
        val fd = vpnInterface ?: return
        running = true

        Thread {
            val buffer = ByteArray(32767)
            try {
                while (running) {
                    val count = fd.fileDescriptor.let { descriptor ->
                        java.io.FileInputStream(descriptor).read(buffer)
                    }
                    if (count <= 0) break

                    // Traffic is deliberately not forwarded yet.
                    // The filtering/tunneling layer must be implemented before
                    // production use; dropping packets here prevents accidental
                    // plaintext forwarding while the engine is incomplete.
                }
            } catch (_: IOException) {
                // Expected when the VPN descriptor is closed during shutdown.
            } finally {
                running = false
            }
        }.apply {
            name = "stop-hazard-vpn"
            isDaemon = true
            start()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STOP HAZARD — ochrona",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("STOP HAZARD")
                .setContentText("Ochrona jest aktywna")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("STOP HAZARD")
                .setContentText("Ochrona jest aktywna")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
    }

    companion object {
        private const val CHANNEL_ID = "stop_hazard_protection"
        private const val NOTIFICATION_ID = 1001
    }
}
