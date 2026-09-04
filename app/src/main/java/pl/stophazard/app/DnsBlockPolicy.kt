package pl.stophazard.app

/**
 * Small, testable decision layer for DNS filtering.
 *
 * A DNS response is not generated here yet; this class only decides whether
 * a queried hostname must be denied. Keeping the decision separate makes
 * the network layer easier to test and replace.
 */
class DnsBlockPolicy(
    private val enabled: Boolean = true
) {
    fun shouldBlock(hostname: String): Boolean {
        if (!enabled) return false
        return BlockedDomains.isBlocked(hostname)
    }
}
