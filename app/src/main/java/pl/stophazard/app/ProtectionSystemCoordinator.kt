package pl.stophazard.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * STOP HAZARD — single native lifecycle coordinator.
 *
 * This class is deliberately a coordinator, not a packet-forwarding engine.
 * It keeps permission, persisted state, service lifecycle and policy access
 * behind one API so the UI does not duplicate VPN logic.
 */
class ProtectionSystemCoordinator(context: Context) {

    private val appContext = context.applicationContext
    private val manager = ProtectionManager(appContext)

    data class SystemSnapshot(
        val enabled: Boolean,
        val vpnAuthorized: Boolean,
        val serviceRunning: Boolean,
        val customDomainCount: Int,
        val premium: Boolean
    )

    fun snapshot(): SystemSnapshot {
        val state = manager.state()
        return SystemSnapshot(
            enabled = state.enabled,
            vpnAuthorized = manager.isVpnAuthorized(),
            serviceRunning = ProtectionServiceState.isRunning(appContext),
            customDomainCount = manager.customBlockedDomains().size,
            premium = manager.canUsePremiumFeatures()
        )
    }

    fun authorizationIntent(): Intent? =
        manager.vpnConsentIntent()

    fun enable(activity: Activity): ProtectionManager.EnableResult =
        manager.enable(activity)

    fun authorizationResult(resultCode: Int): Boolean =
        manager.onAuthorizationResult(resultCode)

    fun disable() {
        manager.disable()
    }

    fun sync(): Boolean =
        manager.sync()

    fun addBlockedDomain(domain: String): Boolean =
        manager.addBlockedDomain(domain)

    fun removeBlockedDomain(domain: String) =
        manager.removeBlockedDomain(domain)

    fun clearCustomDomains() =
        manager.clearCustomBlockedDomains()

    fun customDomains(): Set<String> =
        manager.customBlockedDomains()

    fun policy(): DomainPolicy =
        manager.currentPolicy()

    fun isVpnPrepared(): Boolean =
        VpnService.prepare(appContext) == null
}