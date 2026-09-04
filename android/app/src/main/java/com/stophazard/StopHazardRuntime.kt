package com.stophazard

import android.content.Context
import android.content.Intent
import android.net.VpnService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * STOP HAZARD — consolidated native runtime.
 *
 * One coordinator owns lifecycle, policy, counters and truthful readiness.
 * The actual packet transport remains a separate implementation boundary.
 */
class StopHazardRuntime(private val context: Context) {

    data class Config(
        val strictMode: Boolean = true,
        val mtu: Int = 1500,
        val ipv4Address: String = "10.77.0.2",
        val ipv4Prefix: Int = 32,
        val ipv6Address: String = "fd77:77:77::2",
        val ipv6Prefix: Int = 128
    )

    data class Status(
        val enabled: Boolean,
        val vpnRunning: Boolean,
        val permissionGranted: Boolean,
        val dnsReady: Boolean,
        val ipv4Ready: Boolean,
        val ipv6Ready: Boolean,
        val upstreamReady: Boolean,
        val blocklistReady: Boolean,
        val inspected: Long,
        val blocked: Long
    ) {
        val fullyReady: Boolean
            get() = enabled &&
                vpnRunning &&
                permissionGranted &&
                dnsReady &&
                ipv4Ready &&
                ipv6Ready &&
                upstreamReady &&
                blocklistReady
    }

    private val enabled = AtomicBoolean(false)
    private val vpnRunning = AtomicBoolean(false)
    private val permissionGranted = AtomicBoolean(false)
    private val dnsReady = AtomicBoolean(false)
    private val ipv4Ready = AtomicBoolean(false)
    private val ipv6Ready = AtomicBoolean(false)
    private val upstreamReady = AtomicBoolean(false)
    private val blocklistReady = AtomicBoolean(false)

    private val inspected = AtomicLong(0)
    private val blocked = AtomicLong(0)

    @Volatile private var config = Config()

    fun configure(newConfig: Config) {
        require(newConfig.mtu in 576..65535)
        config = newConfig
    }

    fun setEnabled(value: Boolean) {
        enabled.set(value)
    }

    fun setPermissionGranted(value: Boolean) {
        permissionGranted.set(value)
    }

    fun setDnsReady(value: Boolean) {
        dnsReady.set(value)
    }

    fun setIpv4Ready(value: Boolean) {
        ipv4Ready.set(value)
    }

    fun setIpv6Ready(value: Boolean) {
        ipv6Ready.set(value)
    }

    fun setUpstreamReady(value: Boolean) {
        upstreamReady.set(value)
    }

    fun setBlocklistReady(value: Boolean) {
        blocklistReady.set(value)
    }

    fun recordInspection(wasBlocked: Boolean) {
        inspected.incrementAndGet()
        if (wasBlocked) blocked.incrementAndGet()
    }

    fun resetCounters() {
        inspected.set(0)
        blocked.set(0)
    }

    fun status(): Status = Status(
        enabled = enabled.get(),
        vpnRunning = vpnRunning.get(),
        permissionGranted = permissionGranted.get(),
        dnsReady = dnsReady.get(),
        ipv4Ready = ipv4Ready.get(),
        ipv6Ready = ipv6Ready.get(),
        upstreamReady = upstreamReady.get(),
        blocklistReady = blocklistReady.get(),
        inspected = inspected.get(),
        blocked = blocked.get()
    )

    fun requestVpnIntent(): Intent? =
        VpnService.prepare(context)

    fun startService() {
        val intent = Intent(context, StopHazardVpnService::class.java)
            .setAction(StopHazardVpnService.ACTION_START)
        context.startService(intent)
    }

    fun stopService() {
        val intent = Intent(context, StopHazardVpnService::class.java)
            .setAction(StopHazardVpnService.ACTION_STOP)
        context.startService(intent)
    }

    fun markVpnStarted() {
        vpnRunning.set(true)
    }

    fun markVpnStopped() {
        vpnRunning.set(false)
        dnsReady.set(false)
        ipv4Ready.set(false)
        ipv6Ready.set(false)
        upstreamReady.set(false)
    }

    /**
     * Protection may only be advertised as fully active when every required
     * native component confirms readiness.
     */
    fun canAdvertiseProtection(): Boolean = status().fullyReady

    fun snapshot(): Map<String, Any> {
        val s = status()
        return mapOf(
            "enabled" to s.enabled,
            "vpnRunning" to s.vpnRunning,
            "permissionGranted" to s.permissionGranted,
            "dnsReady" to s.dnsReady,
            "ipv4Ready" to s.ipv4Ready,
            "ipv6Ready" to s.ipv6Ready,
            "upstreamReady" to s.upstreamReady,
            "blocklistReady" to s.blocklistReady,
            "inspected" to s.inspected,
            "blocked" to s.blocked,
            "fullyReady" to s.fullyReady,
            "strictMode" to config.strictMode,
            "mtu" to config.mtu
        )
    }
}