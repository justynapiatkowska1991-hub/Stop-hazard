package pl.stophazard.app

/**
 * Czysty, bezpieczny moduł decyzji dla przyszłego filtra DNS.
 * Nie uruchamia VPN, nie zmienia tras i nie przejmuje ruchu internetowego.
 */
object DomainFilter {
    fun shouldBlock(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return BlockedDomains.isBlocked(host)
    }

    fun decision(host: String?): BlockDecision {
        return if (shouldBlock(host)) {
            BlockDecision.BLOCK
        } else {
            BlockDecision.ALLOW
        }
    }
}
