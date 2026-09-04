package pl.stophazard.app

sealed class ProtectionEvent {
    data object ProtectionStarted : ProtectionEvent()
    data object ProtectionStopped : ProtectionEvent()
    data object VpnAuthorizationRequired : ProtectionEvent()
    data class DomainBlocked(val hostname: String) : ProtectionEvent()
    data class Error(val message: String) : ProtectionEvent()
}
