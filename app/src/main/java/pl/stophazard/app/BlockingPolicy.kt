package pl.stophazard.app

data class BlockingPolicy(
    val enabled: Boolean = false,
    val allowPause: Boolean = false,
    val trustedPersonProtection: Boolean = false
) {
    fun shouldBlockHost(host: String): Boolean =
        enabled && BlockedDomains.isBlocked(host)
}
