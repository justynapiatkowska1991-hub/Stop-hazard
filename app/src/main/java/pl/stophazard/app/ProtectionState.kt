package pl.stophazard.app

/**
 * Persistable protection state. The network service can use this state
 * without coupling itself to the UI or billing layer.
 */
data class ProtectionState(
    val enabled: Boolean = false,
    val tier: SubscriptionTier = SubscriptionTier.BASIC
) {
    fun canUsePremiumFeatures(): Boolean = tier == SubscriptionTier.PREMIUM
}
