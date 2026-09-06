package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import cc.hev.socks5.tunnel.HevSocks5Tunnel
import cc.hev.socks5.tunnel.TunnelConfig
import cc.hev.socks5.tunnel.TunnelException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class BlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnel: HevSocks5Tunnel? = null
    private var socks5: LocalSocks5Server? = null
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

        // Local SOCKS5 is the controlled upstream. Its outbound sockets are
        // protected by this VpnService, so they bypass the VPN and avoid loops.
        socks5 = LocalSocks5Server(
            protectSocket = { socket: Socket -> protect(socket) },
            isBlocked = { host -> BlockedDomains.isBlocked(host) }
        ).also { it.start() }

        val builder = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            .addAddress("fd00:stop:hazard::2", 64)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")
            .setBlocking(false)

        vpnInterface = builder.establish()
            ?: throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")

        val config = TunnelConfig.Builder()
            .setSocks5Address("127.0.0.1")
            .setSocks5Port(LocalSocks5Server.PORT)
            .setTunMtu(1500)
            .setTunIPv4Address("10.0.0.2")
            .setTunIPv4Gateway("10.0.0.1")
            .setTunIPv6Address("fd00:stop:hazard::2")
            .setTunIPv6Gateway("fd00:stop:hazard::1")
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")
            .build()

        try {
            tunnel = HevSocks5Tunnel().also {
                it.startAsync(config, vpnInterface!!.fileDescriptor)
            }
        } catch (e: TunnelException) {
            throw IllegalStateException("Nie udało się uruchomić silnika tun2socks", e)
        }

        running.set(true)
    }

    private fun stopVpn() {
        running.set(false)

        try { tunnel?.stop() } catch (_: Exception) {}
        tunnel = null

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
