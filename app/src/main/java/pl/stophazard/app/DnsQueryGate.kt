package pl.stophazard.app

/** Single gate used before a DNS hostname is allowed to proceed. */
class DnsQueryGate(private val resolver: DnsPolicyResolver) {
    data class Decision(
        val hostname: String,
        val allow: Boolean,
        val reason: String
    )

    fun evaluate(hostname: String): Decision {
        val result = resolver.resolvePolicy(hostname)
        return Decision(
            hostname = result.hostname,
            allow = !result.blocked,
            reason = result.reason
        )
    }
}