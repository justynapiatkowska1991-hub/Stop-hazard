package pl.stophazard.app

data class ProtectionSettings(
    val enabled: Boolean = true,
    val mode: ProtectionMode = ProtectionMode.STANDARD,
    val blockGambling: Boolean = true,
    val blockBetting: Boolean = true,
    val blockPoker: Boolean = true,
    val blockLotteries: Boolean = true,
    val blockPromotions: Boolean = true,
    val safeSearch: Boolean = true,
    val notifications: Boolean = true
)
