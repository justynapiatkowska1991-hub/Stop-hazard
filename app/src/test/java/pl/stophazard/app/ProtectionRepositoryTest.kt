package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProtectionRepositoryTest {

    private lateinit var repository: ProtectionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("stop_hazard_protection", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = ProtectionRepository(context)
    }

    @Test
    fun defaultsToBasic() {
        assertFalse(repository.isPremium())
        assertFalse(repository.canUsePremiumFeatures())
    }

    @Test
    fun premiumEntitlementIsReported() {
        repository.setTier(SubscriptionTier.PREMIUM)
        assertTrue(repository.isPremium())
        assertTrue(repository.canUsePremiumFeatures())
    }
}
