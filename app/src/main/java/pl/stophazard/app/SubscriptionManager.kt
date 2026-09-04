package pl.stophazard.app

import android.content.Context

/**
 * Local entitlement facade. Real Google Play billing will replace the
 * entitlement update path; pricing identifiers stay centralized in Pricing.
 */
class SubscriptionManager(context: Context) {
    private val repository = ProtectionRepository(context.applicationContext)

    fun currentTier(): SubscriptionTier = repository.state().tier

    fun uiState(): SubscriptionUiState {
        val tier = currentTier()
        return SubscriptionUiState(
            currentTier = tier,
            basicMonthlyPrice = Pricing.BASIC_MONTHLY_PLN,
            premiumMonthlyPrice = Pricing.PREMIUM_MONTHLY_PLN,
            premiumYearlyPrice = Pricing.PREMIUM_YEARLY_PLN,
            canUsePremiumFeatures = tier == SubscriptionTier.PREMIUM
        )
    }

    fun setLocalEntitlement(tier: SubscriptionTier) {
        repository.setTier(tier)
    }
}
