package pl.stophazard.app

object BlockedDomains {
    private val domains = setOf(
        "bet365.com", "betway.com", "williamhill.com", "888.com",
        "pokerstars.com", "unibet.com", "bwin.com", "ladbrokes.com",
        "coral.co.uk", "skybet.com", "casino.com", "leovegas.com",
        "betfair.com"
    )

    fun isBlocked(host: String): Boolean {
        val normalized = host.lowercase().trimEnd('.')
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }
}