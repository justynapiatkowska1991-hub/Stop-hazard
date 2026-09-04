package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground VPN service.
 *
 * The packet loop currently routes only IPv4 UDP DNS traffic through the
 * DNS policy engine. Other traffic is not silently forwarded.
 */
class BlockingVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    override fun onCreate() {
        super.onCreate()
        ProtectionNotification.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(
                ProtectionNotification.NOTIFICATION_ID,
                ProtectionNotification.build(this, true)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) {
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

        input = vpnInterface?.fileDescriptor?.let(::FileInputStream)
        output = vpnInterface?.fileDescriptor?.let(::FileOutputStream)
    }

    private fun startPacketLoop() {
        val reader = input ?: run {
            running.set(false)
            return
        }
        val writer = output ?: run {
            running.set(false)
            return
        }

        worker = Thread {
            val context = applicationContext
            val stats = ProtectionStatsStore(context)
            val policyStore = DomainPolicyStore(context)
            val engine = DnsBlockEngine(
                policy = DomainPolicy(policyStore.getCustomBlockedDomains()),
                statsStore = stats
            )
            val processor = DnsQueryProcessor(engine, DnsTransport())
            val router = VpnPacketRouter(processor)
            val buffer = ByteArray(32767)

            try {
                while (running.get()) {
                    val count = reader.read(buffer)
                    if (count <= 0) break

                    val result = router.route(buffer, count)
                    val response = result.responsePacket ?: continue

                    writer.write(response)
                    writer.flush()
                }
            } catch (_: IOException) {
                // Descriptor closure is expected during normal shutdown.
            } finally {
                running.set(false)
            }
        }.apply {
            name = "stop-hazard-vpn-router"
            isDaemon = true
            start()
        }
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
        if (!running.getAndSet(false)) {
            closeResources()
            return
        }
        worker?.interrupt()
        worker = null
        closeResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun closeResources() {
        try { input?.close() } catch (_: IOException) {}
        try { output?.close() } catch (_: IOException) {}
        input = null
        output = null

        try { vpnInterface?.close() } catch (_: IOException) {}
        vpnInterface = null
    }
}
