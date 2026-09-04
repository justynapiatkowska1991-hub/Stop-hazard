package pl.stophazard.app

enum class BlockDecision {
    ALLOW,
    BLOCK
}

/**
 * Pure decision function used by the future DNS transport layer.
 */
fun decideForHost(host: String, policy: DomainPolicy): BlockDecision =
    if (policy.shouldBlock(host)) BlockDecision.BLOCK else BlockDecision.ALLOW
