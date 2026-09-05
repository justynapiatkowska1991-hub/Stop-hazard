package pl.stophazard.app

import android.content.Context

/**
 * Persists user-defined blocked domains.
 */
class DomainPolicyStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getCustomBlockedDomains(): Set<String> =
        prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet())
            ?.map(BlockedDomains::normalize)
            ?.filter(::isValidHostname)
            ?.toSet()
            ?: emptySet()

    @Synchronized
    fun addCustomDomain(domain: String): Boolean {
        val normalized = BlockedDomains.normalize(domain)
        if (!isValidHostname(normalized)) return false

        val updated = getCustomBlockedDomains().toMutableSet()
        if (!updated.add(normalized)) return true

        prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, updated).apply()
        return true
    }

    @Synchronized
    fun removeCustomDomain(domain: String) {
        val normalized = BlockedDomains.normalize(domain)
        val updated = getCustomBlockedDomains().toMutableSet()
        if (updated.remove(normalized)) {
            prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, updated).apply()
        }
    }

    @Synchronized
    fun clearCustomDomains() {
        prefs.edit().remove(KEY_CUSTOM_DOMAINS).apply()
    }

    private fun isValidHostname(host: String): Boolean {
        if (host.isBlank() || host.length > 253) return false
        if (host.startsWith(".") || host.endsWith(".")) return false

        return host.split(".").all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                label.first().isLetterOrDigit() &&
                label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    companion object {
        private const val FILE_NAME = "stop_hazard_domain_policy"
        private const val KEY_CUSTOM_DOMAINS = "custom_blocked_domains"
    }
}
