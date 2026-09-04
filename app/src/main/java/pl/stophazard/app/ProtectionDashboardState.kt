package pl.stophazard.app

data class ProtectionDashboardState(
    val enabled: Boolean = false,
    val vpnAuthorized: Boolean = false,
    val tier: SubscriptionTier = SubscriptionTier.BASIC,
    val blockedRequests: Long = 0,
    val lastBlockedHost: String? = null,
    val customDomains: List<String> = emptyList()
) {
    val protectionReady: Boolean
        get() = enabled && vpnAuthorized

    val planLabel: String
        get() = when (tier) {
            SubscriptionTier.BASIC -> "Basic"
            SubscriptionTier.PREMIUM -> "Premium"
        }
}
