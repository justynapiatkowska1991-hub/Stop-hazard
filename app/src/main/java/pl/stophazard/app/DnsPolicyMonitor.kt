package pl.stophazard.app

/**
 * Central DNS policy monitor.
 * Keeps counters and history in one place without performing network I/O.
 */
class DnsPolicyMonitor(
    private val gate: DnsQueryGate
) {
    data class Stats(
        val inspected: Long,
        val blocked: Long,
        val allowed: Long
    )

    private var inspected = 0L
    private var blocked = 0L
    private var allowed = 0L

    @Synchronized
    fun evaluate(hostname: String): DnsQueryGate.Decision {
        val decision = gate.evaluate(hostname)
        inspected++
        if (decision.allow) allowed++ else blocked++
        return decision
    }

    @Synchronized
    fun stats() = Stats(inspected, blocked, allowed)

    @Synchronized
    fun reset() {
        inspected = 0
        blocked = 0
        allowed = 0
    }
}