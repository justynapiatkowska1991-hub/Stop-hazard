package pl.stophazard.app

data class ProtectionState(
    val enabled: Boolean,
    val tier: SubscriptionTier
)
