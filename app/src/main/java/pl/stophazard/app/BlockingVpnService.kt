package pl.stophazard.app

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
        ProtectionNotification.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(ProtectionNotification.NOTIFICATION_ID, ProtectionNotification.build(this, true))
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
            .addDnsServer("10.10.0.1")
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

    /**
     * Returns true when a hostname is denied by the current domain policy.
     * Packet forwarding is intentionally kept separate from this decision
     * until a complete TCP/UDP forwarding implementation is in place.
     */
    private fun shouldBlockHost(hostname: String): Boolean =
        DomainPolicy().shouldBlock(hostname)

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

}
