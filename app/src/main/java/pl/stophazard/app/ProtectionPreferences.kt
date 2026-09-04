package pl.stophazard.app

import android.content.Context

/**
 * Small persistence layer for the user's protection setting and subscription tier.
 */
class ProtectionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isProtectionEnabled(): Boolean =
        prefs.getBoolean(KEY_ENABLED, false)

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getTier(): SubscriptionTier {
        val value = prefs.getString(KEY_TIER, SubscriptionTier.BASIC.name)
        return runCatching { SubscriptionTier.valueOf(value ?: SubscriptionTier.BASIC.name) }
            .getOrDefault(SubscriptionTier.BASIC)
    }

    fun setTier(tier: SubscriptionTier) {
        prefs.edit().putString(KEY_TIER, tier.name).apply()
    }

    fun readState(): ProtectionState =
        ProtectionState(
            enabled = isProtectionEnabled(),
            tier = getTier()
        )

    companion object {
        private const val FILE_NAME = "stop_hazard_protection"
        private const val KEY_ENABLED = "protection_enabled"
        private const val KEY_TIER = "subscription_tier"
    }
}
