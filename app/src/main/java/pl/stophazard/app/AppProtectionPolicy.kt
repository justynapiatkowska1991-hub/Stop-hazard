package pl.stophazard.app

data class AppProtectionPolicy(
    val enabled: Boolean,
    val tier: SubscriptionTier,
    val blockKnownGamblingDomains: Boolean = true,
    val allowCustomDomains: Boolean = true
) {
    fun domainPolicy(customDomains: Set<String>): DomainPolicy =
        if (blockKnownGamblingDomains) {
            DomainPolicy(if (allowCustomDomains) customDomains else emptySet())
        } else {
            DomainPolicy(if (allowCustomDomains) customDomains else emptySet())
        }
}
