package pl.stophazard.app

enum class DomainRuleType { GAMBLING, BETTING, POKER, LOTTERY, PROMOTION, CUSTOM }

data class DomainRule(
    val host: String,
    val type: DomainRuleType,
    val enabled: Boolean = true,
    val note: String = ""
)
