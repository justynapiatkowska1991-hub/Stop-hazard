package pl.stophazard.app

/**
 * Central packet decision layer.
 *
 * It does not attempt to extract hostnames from arbitrary encrypted IP
 * packets. Host/domain metadata must be supplied by a trusted classifier.
 */
class PacketDecisionEngine(
    private val policy: DomainPolicy
) {
    enum class Decision { ALLOW, BLOCK, UNKNOWN }

    data class Input(
        val packet: ByteArray,
        val hostname: String? = null
    )

    data class Result(
        val decision: Decision,
        val reason: String
    )

    fun decide(input: Input): Result {
        val host = input.hostname?.trim()?.lowercase()?.trimEnd('.')
        if (host.isNullOrEmpty()) {
            return Result(Decision.UNKNOWN, "hostname-unavailable")
        }

        return if (policy.isBlocked(host)) {
            Result(Decision.BLOCK, "blocked-domain")
        } else {
            Result(Decision.ALLOW, "domain-not-blocked")
        }
    }
}