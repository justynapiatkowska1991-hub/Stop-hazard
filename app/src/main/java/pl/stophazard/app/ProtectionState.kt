package pl.stophazard.app

data class ProtectionState(
    val enabled: Boolean,
    val tier: SubscriptionTier
) {
    val canUsePremiumFeatures: Boolean
        get() = tier == SubscriptionTier.PREMIUM

    companion object {
        fun isEnabled(context: android.content.Context): Boolean =
            ProtectionController.isEnabled(context)
    }
}
