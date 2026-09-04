package pl.stophazard.app

/**
 * Combines the built-in gambling list with user-added domains.
 */
class DomainPolicy(
    private val customDomains: Set<String> = emptySet()
) {
    private val normalizedCustomDomains: Set<String> =
        customDomains
            .map(BlockedDomains::normalize)
            .filter { it.isNotBlank() }
            .toSet()

    fun shouldBlock(host: String): Boolean {
        val normalized = BlockedDomains.normalize(host)
        if (normalized.isBlank()) return false

        return BlockedDomains.isBlocked(normalized) ||
            normalizedCustomDomains.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun allBlockedDomains(): Set<String> =
        normalizedCustomDomains + builtInDomains()

    private fun builtInDomains(): Set<String> = setOf(
        "bet365.com",
        "williamhill.com",
        "unibet.com",
        "betway.com",
        "bwin.com",
        "888.com",
        "pokerstars.com",
        "betsson.com"
    )
}
