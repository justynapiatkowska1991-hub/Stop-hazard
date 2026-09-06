package pl.stophazard.app

import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

object BlockedDomains {
    private val builtInDomains = setOf(
        "bet365.com", "betway.com", "williamhill.com", "888.com",
        "pokerstars.com", "unibet.com", "bwin.com", "ladbrokes.com",
        "coral.co.uk", "skybet.com", "casino.com", "leovegas.com",
        "betfair.com", "sts.pl", "fortuna.pl", "superbet.pl",
        "betters.pl", "betfan.pl", "etoto.pl", "lvbet.pl",
        "forbet.pl", "totalbet.pl", "1xbet.com", "melbet.com",
        "mostbet.com", "22bet.com", "stake.com", "stake.bet",
        "betclic.com", "betclic.pl", "888casino.com", "888sport.com",
        "pokerstars.eu", "pokerstarscasino.com", "ggpoker.com",
        "partypoker.com", "casino777.com", "casino-x.com",
        "rabona.com", "vulkanvegas.com", "joycasino.com",
        "casumo.com", "mrgreen.com", "bet-at-home.com",
        "10bet.com", "marathonbet.com", "parimatch.com",
        "pin-up.bet", "pinup.com", "betwinner.com", "linebet.com",
        "1win.com", "1xbit.com", "rollbit.com", "roobet.com",
        "bc.game", "cloudbet.com", "bet9ja.com", "sportingbet.com",
        "betvictor.com", "betfred.com", "paddypower.com",
        "virginbet.com", "betsson.com", "nordicbet.com",
        "betsafe.com", "betonline.ag", "bovada.lv", "mybookie.ag",
        "draftkings.com", "fanduel.com", "betmgm.com",
        "betrivers.com", "hardrock.bet", "pointsbet.com", "caesars.com"
    )

    @Volatile
    private var domains: Set<String> = builtInDomains

    fun domains(): Set<String> = domains

    fun normalize(host: String): String =
        host.lowercase().trim().trimEnd('.').removePrefix("www.")

    fun isBlocked(host: String): Boolean {
        val normalized = normalize(host)
        return domains.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun decision(host: String): BlockDecision =
        if (isBlocked(host)) BlockDecision.BLOCK else BlockDecision.ALLOW

    /**
     * Pobiera aktualny, publiczny rejestr Ministerstwa Finansów.
     * Jeśli serwer jest niedostępny, pozostaje lokalna lista awaryjna.
     */
    fun refreshFromOfficialRegistry() {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(REGISTER_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 20_000
                    setRequestProperty("Accept", "application/xml")
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@Thread

                val builder = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                }.newDocumentBuilder()

                val document = connection.inputStream.use { builder.parse(it) }
                val nodes = document.getElementsByTagNameNS("*", "AdresDomeny")
                val fresh = HashSet<String>(nodes.length)

                for (i in 0 until nodes.length) {
                    val value = nodes.item(i).textContent?.trim()
                    if (!value.isNullOrBlank()) {
                        val normalized = normalize(value)
                        if (normalized.isNotBlank()) fresh.add(normalized)
                    }
                }

                if (fresh.isNotEmpty()) {
                    // Oficjalny rejestr + lokalna lista rozszerzona.
                    domains = fresh + builtInDomains
                }
            } catch (_: Exception) {
                // Nie wyłączamy ochrony przy chwilowym braku Internetu.
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private const val REGISTER_URL = "https://hazard.mf.gov.pl/api/Register"
}

enum class BlockDecision { BLOCK, ALLOW }
