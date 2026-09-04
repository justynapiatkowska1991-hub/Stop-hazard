package pl.stophazard.app

/**
 * Safe DNS-policy adapter.
 * It evaluates a hostname against the existing policy without pretending
 * to be a complete DNS server or network forwarder.
 */
class DnsPolicyResolver(private val policy: DomainPolicy) {

    data class Result(
        val hostname: String,
        val blocked: Boolean,
        val reason: String
    )

    fun resolvePolicy(hostname: String): Result {
        val normalized = DomainClassifier.normalize(hostname)
        if (!normalized.valid || normalized.hostname == null) {
            return Result(hostname, false, normalized.reason)
        }

        val host = normalized.hostname
        val blocked = policy.isBlocked(host)
        return Result(
            host,
            blocked,
            if (blocked) "blocked-by-policy" else "allowed-by-policy"
        )
    }
}