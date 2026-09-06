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

            // Adres naszego wirtualnego interfejsu.
            .addAddress("10.0.0.2", 32)

            // Kierujemy cały IPv4 przez VPN.
            .addRoute("0.0.0.0", 0)

            // DNS używany przez interfejs VPN.
            .addDnsServer("1.1.1.1")

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")
        }

        running.set(true)

        packetThread = thread(
            name = "StopHazard-TUN"
        ) {
            readPackets()
        }
    }

    private fun readPackets() {

        val descriptor = vpnInterface ?: return

        try {

            FileInputStream(
                descriptor.fileDescriptor
            ).use { input ->

                val buffer = ByteArray(32767)

                while (running.get()) {

                    val length = input.read(buffer)

                    if (length <= 0) {
                        break
                    }

                    /*
                     * WAŻNE:
                     *
                     * Tutaj otrzymujemy rzeczywiste pakiety
                     * wychodzące z telefonu.
                     *
                     * Na tym etapie NIE przekazujemy ich jeszcze
                     * do Internetu.
                     *
                     * Następny etap:
                     *
                     * TUN -> parser IP -> TCP/UDP -> tun2socks
                     *
                     * Dopiero tam będzie można zrobić rzeczywiste
                     * blokowanie ruchu.
                     */

                    android.util.Log.d(
                        "STOP_HAZARD",
                        "Odebrano pakiet TUN: $length bajtów"
                    )
                }
            }

        } catch (e: Exception) {

            if (running.get()) {

                android.util.Log.e(
                    "STOP_HAZARD",
                    "Błąd TUN",
                    e
                )
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