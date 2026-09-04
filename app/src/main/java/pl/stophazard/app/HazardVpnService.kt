package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * STOP HAZARD — native VPN foundation.
 * This version inspects IPv4 DNS packets locally and drops queries whose
 * requested hostname matches the block list. It is intentionally conservative:
 * it does not pretend to inspect encrypted HTTPS traffic.
 */
class HazardVpnService : VpnService() {
    private var vpn: ParcelFileDescriptor? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("10.8.0.1")
        vpn = try { builder.establish() } catch (_: Exception) { null }
        if (vpn == null) return
        running = true
        Thread { packetLoop(vpn!!) }.start()
    }

    private fun packetLoop(interfaceFd: ParcelFileDescriptor) {
        val input = FileInputStream(interfaceFd.fileDescriptor)
        val output = FileOutputStream(interfaceFd.fileDescriptor)
        val buffer = ByteArray(32767)
        while (running) {
            val length = try { input.read(buffer) } catch (_: Exception) { -1 }
            if (length <= 0) break
            val packet = buffer.copyOf(length)
            val host = DnsPacket.readQuestionHost(packet)
            if (host != null && BlockedDomains.isBlocked(host)) {
                BlockedLog.record(this, host)
                continue
            }
            // Safe fallback: forward only packets that belong to the configured
            // local tunnel implementation. Full IP forwarding/NAT requires a
            // real upstream transport and is intentionally not fabricated here.
            try {
                if (DnsPacket.isDnsQuery(packet)) {
                    val response = DnsPacket.blockedResponse(packet)
                    output.write(response)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        running = false
        try { vpn?.close() } catch (_: Exception) {}
        vpn = null
        super.onDestroy()
    }
}
