package pl.stophazard.app

import android.content.Context

/**
 * Single entry point for protection settings used by the UI and service.
 */
class ProtectionRepository(context: Context) {
    private val preferences = ProtectionPreferences(context.applicationContext)

    fun state(): ProtectionState = preferences.readState()

    fun setProtectionEnabled(enabled: Boolean) {
        preferences.setProtectionEnabled(enabled)
    }

    fun setTier(tier: SubscriptionTier) {
        preferences.setTier(tier)
    }

    fun isPremium(): Boolean =
        state().tier == SubscriptionTier.PREMIUM
}
