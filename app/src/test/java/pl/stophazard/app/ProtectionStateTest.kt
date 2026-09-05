package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionStateTest {

    @Test
    fun basicTierDoesNotHavePremiumFeatures() {
        val state = ProtectionState(enabled = true, tier = SubscriptionTier.BASIC)
        assertFalse(state.canUsePremiumFeatures)
    }

    @Test
    fun premiumTierHasPremiumFeatures() {
        val state = ProtectionState(enabled = true, tier = SubscriptionTier.PREMIUM)
        assertTrue(state.canUsePremiumFeatures)
    }

    @Test
    fun protectionCanBeDisabledIndependently() {
        val state = ProtectionState(enabled = false, tier = SubscriptionTier.PREMIUM)
        assertTrue(state.canUsePremiumFeatures)
    }
}
