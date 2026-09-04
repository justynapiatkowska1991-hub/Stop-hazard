package pl.stophazard.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * Application-level facade for STOP HAZARD protection.
 *
 * Keeps UI, persisted settings and VPN lifecycle behind one API.
 */
class ProtectionManager(context: Context) {

    private val appContext = context.applicationContext
    private val repository = ProtectionRepository(appContext)
    private val controller = ProtectionControllerFacade(appContext)
    private val domainStore = DomainPolicyStore(appContext)

    fun state(): ProtectionState = repository.state()

    fun isVpnAuthorized(): Boolean = controller.isAuthorized()

    fun vpnConsentIntent(): Intent? = controller.prepareVpn()

    fun enable(activity: Activity): EnableResult {
        repository.setProtectionEnabled(true)

        val consent = controller.prepareVpn()
        if (consent != null) {
            return EnableResult.AuthorizationRequired(consent)
        }

        return if (controller.start()) {
            EnableResult.Started
        } else {
            EnableResult.Failed
        }
    }

    fun onAuthorizationResult(resultCode: Int): Boolean {
        if (resultCode != Activity.RESULT_OK) {
            repository.setProtectionEnabled(false)
            return false
        }

        return controller.start()
    }

    fun disable() {
        repository.setProtectionEnabled(false)
        controller.stop()
    }

    fun sync(): Boolean =
        controller.syncWithState(repository.state().enabled)

    fun addBlockedDomain(domain: String): Boolean =
        domainStore.addCustomDomain(domain)

    fun removeBlockedDomain(domain: String) =
        domainStore.removeCustomDomain(domain)

    fun clearCustomBlockedDomains() =
        domainStore.clearCustomDomains()

    fun customBlockedDomains(): Set<String> =
        domainStore.getCustomBlockedDomains()

    fun canUsePremiumFeatures(): Boolean =
        repository.canUsePremiumFeatures()

    fun currentPolicy(): DomainPolicy =
        DomainPolicy(DomainRepository(appContext))

    sealed class EnableResult {
        data object Started : EnableResult()
        data object Failed : EnableResult()
        data class AuthorizationRequired(val intent: Intent) : EnableResult()
    }
}
