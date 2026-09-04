package pl.stophazard.app

/**
 * Central domain policy for the STOP HAZARD DNS filter.
 * Exact domains and their subdomains are blocked; look-alike domains are not.
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
            .removePrefix("www.")
            .removeSuffix(".")
}
