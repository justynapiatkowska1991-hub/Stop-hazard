package pl.stophazard.app

enum class SubscriptionTier {
    BASIC,
    PREMIUM
}

object Pricing {
    const val BASIC_MONTHLY_PLN = 19.99
    const val PREMIUM_MONTHLY_PLN = 29.99
    const val PREMIUM_YEARLY_PLN = 159.00
}
