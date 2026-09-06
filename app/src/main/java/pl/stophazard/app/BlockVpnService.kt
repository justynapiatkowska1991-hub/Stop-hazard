package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.LondonX.tun2socks.Tun2Socks
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class BlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var socks5: LocalSocks5Server? = null
    private var tunnelThread: Thread? = null
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.get()) return START_STICKY

        startForegroundNotification()
        BlockedDomains.refreshFromOfficialRegistry()

        try {
            startVpn()
        } catch (_: Exception) {
            stopVpn()
        }

        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        socks5 = LocalSocks5Server(
            protectSocket = { socket: Socket -> protect(socket) },
            isBlocked = { host -> BlockedDomains.isBlocked(host) }
        ).also { it.start() }

        val builder = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")
            .setBlocking(false)

        vpnInterface = builder.establish()
            ?: throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")

        Tun2Socks.initialize(applicationContext)

        running.set(true)

        val tun = vpnInterface ?: throw IllegalStateException("Brak interfejsu VPN")
        val socksPort = LocalSocks5Server.PORT

        tunnelThread = Thread {
            try {
                val ok = Tun2Socks.startTun2Socks(
                    Tun2Socks.LogLevel.INFO,
                    tun,
                    1500,
                    "127.0.0.1",
                    socksPort,
                    "10.0.0.2",
                    null,
                    "255.255.255.0",
                    false,
                    emptyList()
                )

                if (!ok && running.get()) {
                    stopVpn()
                }
            } catch (_: Exception) {
                if (running.get()) {
                    stopVpn()
                }
            }
        }.apply {
            name = "stop-hazard-tun2socks"
            start()
        }
    }

    private fun stopVpn() {
        running.set(false)

        try { Tun2Socks.stopTun2Socks() } catch (_: Exception) {}

        tunnelThread = null

        try { socks5?.stop() } catch (_: Exception) {}
        socks5 = null

        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
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
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
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
