package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Native VPN service bootstrap.
 * Creates the TUN interface and reports lifecycle state.
 * Packet forwarding is intentionally left to the routing engine.
 */
class ProtectionVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private val state = VpnSessionState()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (tun != null) return
        state.starting()
        try {
            tun = VpnConfiguration.build(Builder()).establish()
            if (tun == null) {
                state.error("tun-establish-failed")
                stopSelf()
                return
            }
            state.running()
        } catch (e: Exception) {
            state.error(e.message ?: "vpn-start-failed")
            tun?.close()
            tun = null
            stopSelf()
        }
    }

    private fun stopVpn() {
        state.stopped()
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "pl.stophazard.app.STOP_VPN"
    }
}