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
    private val transportStatus = PacketTransportStatus()
    private var packetLoop: VpnPacketLoop? = null
    private var dnsMonitor: DnsPolicyMonitor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    fun transportSnapshot(): PacketTransportStatus.Snapshot = transportStatus.snapshot()

    private fun startVpn() {
        if (tun != null) return
        state.starting()
        transportStatus.starting()
        try {
            tun = VpnConfiguration.build(Builder()).establish()
            if (tun == null) {
                state.error("tun-establish-failed")
                stopSelf()
                return
            }
            val policy = ProtectionManager(applicationContext).currentPolicy()
            dnsMonitor = DnsPolicyMonitor(DnsQueryGate(DnsPolicyResolver(policy)))
            packetLoop = VpnPacketLoop(state) { packet ->
                // The packet loop is now connected to the native VPN lifecycle.
                // Full IP forwarding remains a separate transport implementation.
                packet.isNotEmpty()
            }
            packetLoop?.start(tun!!)
            transportStatus.degraded("tun-capture-active-forwarder-not-configured")
            state.running()
        } catch (e: Exception) {
            transportStatus.error(e.message ?: "vpn-start-failed")
            state.error(e.message ?: "vpn-start-failed")
            tun?.close()
            tun = null
            stopSelf()
        }
    }

    fun sessionSnapshot(): VpnSessionState.Snapshot = state.snapshot()

    fun dnsStats(): DnsPolicyMonitor.Stats? = dnsMonitor?.stats()

    private fun stopVpn() {
        transportStatus.stopped()
        packetLoop?.stop()
        packetLoop = null
        dnsMonitor = null
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