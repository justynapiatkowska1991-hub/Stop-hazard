package pl.stophazard.app

object BlockedDomains {
    val domains: Set<String> = setOf(
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
        val normalized = host.trim().lowercase().removeSuffix(".")
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }
}
