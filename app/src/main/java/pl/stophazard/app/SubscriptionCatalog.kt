package pl.stophazard.app

data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val title: String,
    val monthlyPricePln: Double?,
    val yearlyPricePln: Double?,
    val description: String
)

object SubscriptionCatalog {
    val basic = SubscriptionPlan(
        tier = SubscriptionTier.BASIC,
        title = "Bloker Basic",
        monthlyPricePln = Pricing.BASIC_MONTHLY_PLN,
        yearlyPricePln = null,
        description = "Podstawowa ochrona przed domenami hazardowymi."
    )

    val premium = SubscriptionPlan(
        tier = SubscriptionTier.PREMIUM,
        title = "STOP HAZARD Premium",
        monthlyPricePln = Pricing.PREMIUM_MONTHLY_PLN,
        yearlyPricePln = Pricing.PREMIUM_YEARLY_PLN,
        description = "Rozszerzona ochrona i funkcje Premium."
    )

    val all: List<SubscriptionPlan> = listOf(basic, premium)
}
