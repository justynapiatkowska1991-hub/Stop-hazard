package pl.stophazard.app

object BlockedDomains {
    private val domains = setOf(
        "bet365.com", "betway.com", "williamhill.com", "888.com",
        "pokerstars.com", "unibet.com", "bwin.com", "ladbrokes.com",
        "coral.co.uk", "skybet.com", "casino.com", "leovegas.com",
        "betfair.com", "wincasino.io", "win-casino.io", "wincasino.com",
        "casino1.com", "tikalcasino.com",
        "sts.pl", "fortuna.pl", "superbet.pl", "betters.pl",
        "betfan.pl", "etoto.pl", "lvbet.pl", "forbet.pl",
        "totalbet.pl", "01bet.com", "22bet.com", "1xbet.com",
        "melbet.com", "mostbet.com", "betcris.com"
    )

    fun domains(): Set<String> = domains

    fun isBlocked(host: String): Boolean {
        val normalized = host.lowercase().trimEnd('.')
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }
}