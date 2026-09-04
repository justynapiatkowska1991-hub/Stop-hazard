package com.stophazard

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * STOP HAZARD native Android foundation.
 *
 * One native service owns VPN lifecycle and one engine owns runtime state.
 * This keeps the UI/web layer from duplicating native configuration.
 *
 * IMPORTANT:
 * establish() creates a TUN interface; it does NOT automatically forward
 * Internet traffic. upstreamReady therefore remains false until a real,
 * tested upstream packet transport is attached.
 */
class StopHazardVpnService : VpnService() {
    private lateinit var engine: StopHazardVpnEngine
    private val started = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        engine = StopHazardVpnEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProtection()
            ACTION_STOP -> stopProtection()
        }
        return START_STICKY
    }

    private fun startProtection() {
        if (started.get()) return

        val descriptor = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.77.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addAddress("fd77:77:77::2", 128)
            .addRoute("::", 0)
            .establish() ?: return

        if (!engine.start(descriptor)) {
            descriptor.close()
            return
        }
        started.set(true)
    }

    private fun stopProtection() {
        started.set(false)
        if (::engine.isInitialized) engine.stop()
        stopSelf()
    }

    override fun onDestroy() {
        stopProtection()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.stophazard.action.START"
        const val ACTION_STOP = "com.stophazard.action.STOP"
    }
}

class StopHazardVpnEngine(private val service: VpnService) {
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private var tun: ParcelFileDescriptor? = null

    @Volatile var dnsReady = false
        private set
    @Volatile var ipv4Ready = false
        private set
    @Volatile var ipv6Ready = false
        private set
    @Volatile var upstreamReady = false
        private set
    @Volatile var blocklistReady = false
        private set

    fun start(descriptor: ParcelFileDescriptor): Boolean {
        if (running.get()) return true
        tun = descriptor
        ipv4Ready = true
        ipv6Ready = true
        dnsReady = false
        upstreamReady = false
        blocklistReady = false
        running.set(true)

        executor.submit {
            while (running.get()) {
                /*
                 * Reserved for the real packet-forwarding loop.
                 * Do not claim system-wide protection until:
                 * - packets are read from TUN,
                 * - DNS is handled,
                 * - allowed TCP/UDP traffic reaches a real upstream,
                 * - responses are written back,
                 * - IPv4 and IPv6 are handled,
                 * - failures reconnect safely.
                 */
                Thread.sleep(250)
            }
        }
        return true
    }

    fun stop() {
        running.set(false)
        dnsReady = false
        upstreamReady = false
        blocklistReady = false
        ipv4Ready = false
        ipv6Ready = false
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    fun health(): Map<String, Any> = mapOf(
        "supported" to true,
        "permissionGranted" to true,
        "running" to running.get(),
        "dnsReady" to dnsReady,
        "ipv4Ready" to ipv4Ready,
        "ipv6Ready" to ipv6Ready,
        "upstreamReady" to upstreamReady,
        "blocklistReady" to blocklistReady
    )

    fun setDnsReady(value: Boolean) { dnsReady = value }
    fun setUpstreamReady(value: Boolean) { upstreamReady = value }
    fun setBlocklistReady(value: Boolean) { blocklistReady = value }
}

class StopHazardPolicy(rules: Collection<String>) {
    private val rules = rules.asSequence()
        .map(::normalize)
        .filter { it.isNotBlank() }
        .toSet()

    fun shouldBlock(host: String): Boolean {
        val h = normalize(host)
        return h.isNotBlank() && rules.any { h == it || h.endsWith(".$it") }
    }

    fun size(): Int = rules.size

    private fun normalize(value: String): String =
        value.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore(":")
}

class StopHazardCounters {
    private var inspected = 0L
    private var blocked = 0L

    @Synchronized
    fun record(isBlocked: Boolean) {
        inspected++
        if (isBlocked) blocked++
    }

    @Synchronized
    fun snapshot(): Map<String, Long> =
        mapOf("inspected" to inspected, "blocked" to blocked)

    @Synchronized
    fun reset() {
        inspected = 0
        blocked = 0
    }
}