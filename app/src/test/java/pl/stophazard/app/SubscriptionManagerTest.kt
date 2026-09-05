package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class SubscriptionManagerTest {
    private lateinit var manager: SubscriptionManager
    private lateinit var repository: ProtectionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("stop_hazard_protection", Context.MODE_PRIVATE)
            .edit().clear().commit()
        manager = SubscriptionManager(context)
        repository = ProtectionRepository(context)
    }

    @Test
    fun defaultsToBasic() {
        assertEquals(SubscriptionTier.BASIC, manager.currentTier())
        assertFalse(manager.uiState().canUsePremiumFeatures)
    }

    @Test
    fun premiumEntitlementUpdatesUiState() {
        repository.setTier(SubscriptionTier.PREMIUM)
        val state = manager.uiState()
        assertEquals(SubscriptionTier.PREMIUM, state.currentTier)
        assertTrue(state.canUsePremiumFeatures)
        assertEquals(159.0, state.premiumYearlyPrice, 0.001)
    }
}
