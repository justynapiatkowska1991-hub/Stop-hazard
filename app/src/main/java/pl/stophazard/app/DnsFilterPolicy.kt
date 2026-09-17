package pl.stophazard.app

/**
 * Polityka decyzji dla filtra DNS.
 *
 * Zasada bezpieczeństwa: blokujemy wyłącznie domeny z listy hazardowej.
 * Wszystkie pozostałe domeny są dozwolone. Błąd filtra nie może oznaczać
 * blokady całego internetu — kod odpowiedzialny za transport DNS musi
 * stosować tę politykę tylko do zapytań DNS i przepuszczać resztę ruchu.
 */
object DnsFilterPolicy {
    enum class Decision { BLOCK, ALLOW }

    fun decide(host: String?): Decision {
        val normalized = normalize(host) ?: return Decision.ALLOW
        return if (BlockedDomains.isBlocked(normalized)) {
            Decision.BLOCK
        } else {
            Decision.ALLOW
        }
    }

    fun normalize(host: String?): String? {
        val value = host?.trim()?.lowercase() ?: return null
        if (value.isEmpty() || value.length > 253) return null
        return value.trimEnd('.')
            .removePrefix("[")
            .removeSuffix("]")
            .takeIf { it.isNotEmpty() && !it.contains('/') && !it.contains(' ') }
    }
}
