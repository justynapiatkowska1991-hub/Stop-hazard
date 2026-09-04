package pl.stophazard.app

data class SubscriptionUiState(
    val currentTier: SubscriptionTier,
    val basicMonthlyPrice: Double,
    val premiumMonthlyPrice: Double,
    val premiumYearlyPrice: Double,
    val canUsePremiumFeatures: Boolean
)
