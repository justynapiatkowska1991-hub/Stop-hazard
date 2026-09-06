package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class BlockVpnService : VpnService() {

    companion object {
        const val ACTION_START = "pl.stophazard.app.action.START"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var packetThread: Thread? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (running.get()) {
            return START_STICKY
        }

        startForegroundNotification()

        try {
            startVpn()
        } catch (e: Exception) {
            stopVpn()
        }

        return START_STICKY
    }

    private fun startVpn() {

        val builder = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")

        // Nie kierujemy całego Internetu do TUN.
        // Pełny routing bez forwardera pakietów odcina połączenie.
        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")
        }

        running.set(true)

        packetThread = thread(name = "StopHazard-TUN") {
            readPackets()
        }
    }

    private fun readPackets() {
        // Tymczasowo nie czytamy TUN. W kolejnym etapie dodamy selektywne
        // blokowanie DNS, bez przejmowania całego ruchu internetowego.
        while (running.get()) {
            try {
                Thread.sleep(1000)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun stopVpn() {

        running.set(false)

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }

        vpnInterface = null

        packetThread?.interrupt()
        packetThread = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {

        stopVpn()

        super.onDestroy()
    }

    private fun startForegroundNotification() {

        val channelId = "stop_hazard_vpn"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "STOP HAZARD",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification.Builder(this, channelId)
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

        startForeground(1001, notification)
    }
}