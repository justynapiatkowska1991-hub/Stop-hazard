package pl.stophazard.app

/** Connects hostname normalization with the central blocking policy. */
class DomainDecisionService(
    private val policy: DomainPolicy
) {
    data class Result(
        val hostname: String?,
        val blocked: Boolean,
        val reason: String
    )

    fun check(hostname: String?): Result {
        val normalized = DomainClassifier.normalize(hostname)
        if (!normalized.valid || normalized.hostname == null) {
            return Result(null, false, normalized.reason)
        }

        val host = normalized.hostname
        val blocked = policy.isBlocked(host)
        return Result(host, blocked, if (blocked) "blocked-domain" else "allowed-domain")
    }
}