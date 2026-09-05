package pl.stophazard.app

data class ProtectionState(
    val enabled: Boolean,
    val tier: SubscriptionTier
) {
    companion object {
        fun isEnabled(context: android.content.Context): Boolean =
            ProtectionController.isEnabled(context)
    }
}
