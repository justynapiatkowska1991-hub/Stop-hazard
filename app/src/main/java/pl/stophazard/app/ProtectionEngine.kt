package pl.stophazard.app

import android.content.Context

class ProtectionEngine(private val context:Context) {
    private val domains=DomainRepository(context)
    private val policy=DomainPolicy(domains)

    fun inspect(host:String):Boolean {
        val blocked=policy.isBlocked(host)
        ProtectionStatsStore.record(context,blocked)
        if(blocked) BlockedLog.record(context,BlockedDomains.normalize(host))
        return blocked
    }

    fun addCustomDomain(host:String)=policy.add(host)
    fun removeCustomDomain(host:String)=policy.remove(host)
    fun customDomains():Set<String>=domains.getCustom()
}
