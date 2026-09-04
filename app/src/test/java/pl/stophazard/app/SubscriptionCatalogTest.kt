package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionCatalogTest {

    @Test
    fun basicPriceIsCorrect() {
        assertEquals(19.99, SubscriptionCatalog.basic.monthlyPricePln!!, 0.001)
    }

    @Test
    fun premiumPricesAreCorrect() {
        assertEquals(29.99, SubscriptionCatalog.premium.monthlyPricePln!!, 0.001)
        assertEquals(159.00, SubscriptionCatalog.premium.yearlyPricePln!!, 0.001)
    }

    @Test
    fun catalogContainsBothTiers() {
        assertTrue(SubscriptionCatalog.all.any { it.tier == SubscriptionTier.BASIC })
        assertTrue(SubscriptionCatalog.all.any { it.tier == SubscriptionTier.PREMIUM })
    }
}
