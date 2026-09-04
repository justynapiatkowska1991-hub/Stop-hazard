package pl.stophazard.app

import android.content.Context

class DomainRepository(context: Context) {
    private val prefs=context.getSharedPreferences("stop_hazard_domains",Context.MODE_PRIVATE)
    fun getCustom():Set<String> = prefs.getStringSet("custom",emptySet()) ?: emptySet()
    fun add(host:String):Boolean {
        val normalized=BlockedDomains.normalize(host)
        if(normalized.isBlank() || !normalized.contains(".")) return false
        val next=getCustom().toMutableSet(); next.add(normalized)
        prefs.edit().putStringSet("custom",next).apply(); return true
    }
    fun remove(host:String) {
        val next=getCustom().toMutableSet(); next.remove(BlockedDomains.normalize(host))
        prefs.edit().putStringSet("custom",next).apply()
    }
    fun isBlocked(host:String)=BlockedDomains.isBlocked(host,getCustom())
}
