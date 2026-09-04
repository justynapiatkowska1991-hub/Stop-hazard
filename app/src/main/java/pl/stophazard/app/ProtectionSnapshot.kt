package pl.stophazard.app

data class ProtectionSnapshot(
    val state: ProtectionState,
    val vpnAuthorized: Boolean,
    val stats: ProtectionStats,
    val customDomains: List<String>
) {
    val isReady: Boolean
        get() = state.enabled && vpnAuthorized
}
