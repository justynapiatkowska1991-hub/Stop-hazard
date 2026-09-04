package pl.stophazard.app

data class ProtectionStats(
    val blockedRequests: Long = 0,
    val lastBlockedHost: String? = null
)
