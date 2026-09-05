package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Single native VPN entry point.
 *
 * This service owns TUN lifecycle and DNS-policy decisions. It deliberately
 * does not discard arbitrary non-DNS packets: until an upstream IP forwarder
 * exists they are reported as transport-unavailable.
 */
class HazardVpnService : VpnService() {
    private var vpn: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private val session = VpnSessionState()
    private val transport = PacketTransportStatus()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        session.starting()
        transport.starting()

        vpn = try {
            VpnConfiguration.build(Builder())
                .setMtu(1500)
                .addDnsServer("10.8.0.1")
                .establish()
        } catch (_: Exception) { null }

        if (vpn == null) {
            session.error("tun-establish-failed")
            transport.error("tun-establish-failed")
            return
        }

        running = true
        ProtectionServiceState.setRunning(this, true)

        Thread {
            FileInputStream(vpn!!.fileDescriptor).use { input ->
                FileOutputStream(vpn!!.fileDescriptor).use { output ->
                    val buffer = ByteArray(32767)
                    val engine = ProtectionEngine(this)
                    val policy = ProtectionManager(this@HazardVpnService).currentPolicy()
                    val runtime = VpnFlowRuntime(policy, output)
                    transport.degraded("upstream-forwarder-not-configured")

                    try {
                        while (running) {
                            val n = input.read(buffer)
                            if (n <= 0) continue
                            session.packetRead()

                            val packet = buffer.copyOf(n)
                            val host = DnsPacket.readQuestionHost(packet)

                            if (host != null && engine.inspect(host)) {
                                DnsResponse.nxdomain(packet)?.let {
                                    output.write(it)
                                    output.flush()
                                }
                                session.packetDropped()
                            } else {
                                val decoded = TunPacketCodec.parse(packet)
                                if (decoded != null && runtime.send(decoded)) {
                                    session.packetForwarded()
                                    transport.ready()
                                } else {
                                    session.packetDropped()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (running) {
                            session.error(e.message ?: "vpn-loop-failed")
                            transport.error(e.message ?: "vpn-loop-failed")
                        }
                    } finally {
                        ProtectionServiceState.setRunning(this@HazardVpnService, false)
                    }
                }
            }
        }.apply {
            name = "StopHazard-VpnLoop"
            isDaemon = true
            start()
        }

        session.running()
    }

    private fun stopVpn() {
        running = false
        transport.stopped()
        session.stopped()
        try { vpn?.close() } catch (_: Exception) {}
        vpn = null
        ProtectionServiceState.setRunning(this, false)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "pl.stophazard.app.STOP_VPN"
    }
}