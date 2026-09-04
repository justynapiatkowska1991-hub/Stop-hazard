package pl.stophazard.app

/**
 * Combines the built-in gambling blocklist with domains explicitly added by
 * the user. Premium/paid entitlement is intentionally handled elsewhere.
 */
class DomainPolicy(
    private val customDomains: Set<String> = emptySet()
) {
    fun shouldBlock(host: String): Boolean {
        val normalized = BlockedDomains.normalize(host)
        if (normalized.isBlank()) return false

        return BlockedDomains.isBlocked(normalized) ||
            customDomains.any { normalized == it || normalized.endsWith(".$it") }
    }
}
