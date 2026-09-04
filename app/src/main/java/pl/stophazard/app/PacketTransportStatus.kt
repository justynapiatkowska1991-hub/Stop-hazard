package pl.stophazard.app

/** Explicit status of the VPN packet-transport layer. */
class PacketTransportStatus {
    enum class Phase { IDLE, STARTING, READY, DEGRADED, STOPPED, ERROR }

    @Volatile private var phase = Phase.IDLE
    @Volatile private var lastError: String? = null
    @Volatile private var startedAt: Long? = null

    @Synchronized fun starting() {
        phase = Phase.STARTING
        lastError = null
        startedAt = System.currentTimeMillis()
    }

    @Synchronized fun ready() { phase = Phase.READY; lastError = null }
    @Synchronized fun degraded(reason: String) { phase = Phase.DEGRADED; lastError = reason }
    @Synchronized fun error(reason: String) { phase = Phase.ERROR; lastError = reason }
    @Synchronized fun stopped() { phase = Phase.STOPPED }

    data class Snapshot(val phase: Phase, val lastError: String?, val startedAt: Long?)
    fun snapshot() = Snapshot(phase, lastError, startedAt)
}