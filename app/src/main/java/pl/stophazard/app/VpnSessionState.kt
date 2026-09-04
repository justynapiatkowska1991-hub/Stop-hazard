package pl.stophazard.app

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe state shared by the native VPN service and diagnostics.
 * Routing is reported as active only after the service explicitly marks it ready.
 */
class VpnSessionState {
    enum class Phase { STOPPED, STARTING, WAITING_FOR_CONSENT, RUNNING, ERROR }

    private val phase = java.util.concurrent.atomic.AtomicReference(Phase.STOPPED)
    private val ready = AtomicBoolean(false)
    private val read = AtomicLong(0)
    private val forwarded = AtomicLong(0)
    private val dropped = AtomicLong(0)

    @Volatile var lastError: String? = null
        private set

    fun starting() {
        lastError = null
        ready.set(false)
        phase.set(Phase.STARTING)
    }

    fun waitingForConsent() {
        ready.set(false)
        phase.set(Phase.WAITING_FOR_CONSENT)
    }

    fun running() {
        lastError = null
        ready.set(true)
        phase.set(Phase.RUNNING)
    }

    fun error(message: String) {
        ready.set(false)
        lastError = message
        phase.set(Phase.ERROR)
    }

    fun stopped() {
        ready.set(false)
        phase.set(Phase.STOPPED)
    }

    fun packetRead() = read.incrementAndGet()
    fun packetForwarded() = forwarded.incrementAndGet()
    fun packetDropped() = dropped.incrementAndGet()

    data class Snapshot(
        val phase: Phase,
        val ready: Boolean,
        val packetsRead: Long,
        val packetsForwarded: Long,
        val packetsDropped: Long,
        val lastError: String?
    )

    fun snapshot() = Snapshot(
        phase.get(),
        ready.get(),
        read.get(),
        forwarded.get(),
        dropped.get(),
        lastError
    )

    fun resetCounters() {
        read.set(0)
        forwarded.set(0)
        dropped.set(0)
    }
}