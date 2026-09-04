package pl.stophazard.app

/**
 * Built-in gambling domain policy.
 * Matching is case-insensitive and covers only the exact domain or its subdomains.
 */
object BlockedDomains {
    private val domains: Set<String> = setOf(
        "bet365.com",
        "williamhill.com",
        "unibet.com",
        "betway.com",
        "bwin.com",
        "888.com",
        "pokerstars.com",
        "betsson.com"
    )

    fun isBlocked(host: String): Boolean {
        val normalized = normalize(host)
        return normalized.isNotEmpty() &&
            domains.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun normalize(host: String): String =
        host.trim()
            .lowercase()
            .removeSuffix(".")
            .removePrefix("www.")
}
