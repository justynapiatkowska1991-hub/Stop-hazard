package pl.stophazard.app

import android.app.Activity

/**
 * View-model-like coordinator for the main protection dashboard.
 * UI can call refresh after every user action.
 */
class ProtectionDashboard(
    private val activity: Activity
) {
    private val manager = ProtectionManager(activity)
    private val subscriptions = SubscriptionManager(activity)

    fun state(): ProtectionDashboardState {
        val snapshot = ProtectionSnapshotProvider(activity).snapshot()
        return ProtectionDashboardState(
            enabled = snapshot.state.enabled,
            vpnAuthorized = snapshot.vpnAuthorized,
            tier = snapshot.state.tier,
            blockedRequests = snapshot.stats.blockedRequests,
            lastBlockedHost = snapshot.stats.lastBlockedHost,
            customDomains = snapshot.customDomains
        )
    }

    fun enable(): ProtectionManager.EnableResult =
        manager.enable(activity)

    fun authorizationCompleted(resultCode: Int): Boolean =
        manager.onAuthorizationResult(resultCode)

    fun disable() {
        manager.disable()
    }

    fun addDomain(domain: String): Boolean =
        manager.addBlockedDomain(domain)

    fun removeDomain(domain: String) =
        manager.removeBlockedDomain(domain)

    fun subscriptionState(): SubscriptionUiState =
        subscriptions.uiState()
}
