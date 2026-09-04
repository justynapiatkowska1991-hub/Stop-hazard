package pl.stophazard.app

import android.content.Context

class ProtectionSnapshotProvider(context: Context) {
    private val appContext = context.applicationContext
    private val repository = ProtectionRepository(appContext)
    private val controller = ProtectionController(appContext)
    private val statsStore = ProtectionStatsStore(appContext)
    private val domainStore = DomainPolicyStore(appContext)

    fun snapshot(): ProtectionSnapshot =
        ProtectionSnapshot(
            state = repository.state(),
            vpnAuthorized = controller.isAuthorized(),
            stats = statsStore.read(),
            customDomains = domainStore.getCustomBlockedDomains().sorted()
        )
}
