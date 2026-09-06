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
        "stake.com",
        "stake.bet",
        "bet365.co.uk",
        "bet365.com.au",
        "bet365.es",
        "betclic.fr",
        "betclic.pl",
        "betway.com",
        "1xbet.pl",
        "melbet.pl",
        "mostbet.pl",
        "22bet.pl",
        "parimatch.com",
        "pokerstars.eu",
        "pokerstarscasino.com",
        "ggpoker.com",
        "casino777.com",
        "casino-x.com",
        "rabona.com",
        "22bet.com",
        "betonline.ag",
        "draftkings.com",
        "fanduel.com",
        "betsson.com",
        "mrgreen.com",
        "casumo.com",
        "bet-at-home.com",
        "10bet.com",
        "bet9ja.com",
        "betway.pl",
        "totalbet.pl",
        "e-toto.pl",
        "etoto.pl",
    )

    fun domains(): Set<String> = domains

    fun normalize(host: String): String =
        host.lowercase().trim().trimEnd('.').removePrefix("www.")

    fun isBlocked(host: String): Boolean {
        val normalized = normalize(host)
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun decision(host: String): BlockDecision =
        if (isBlocked(host)) BlockDecision.BLOCK else BlockDecision.ALLOW
}

enum class BlockDecision { BLOCK, ALLOW }
