package pl.stophazard.app

import android.content.Context

/**
 * Stores user-managed additional blocked domains.
 * The built-in list remains immutable; user entries are kept separately.
 */
class DomainPolicyStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getCustomBlockedDomains(): Set<String> =
        prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet())
            ?.map { BlockedDomains.normalize(it) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    fun addCustomDomain(domain: String): Boolean {
        val normalized = BlockedDomains.normalize(domain)
        if (!isValidHostname(normalized)) return false

        val updated = getCustomBlockedDomains().toMutableSet().apply { add(normalized) }
        prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, updated).apply()
        return true
    }

    fun removeCustomDomain(domain: String) {
        val normalized = BlockedDomains.normalize(domain)
        val updated = getCustomBlockedDomains().toMutableSet().apply { remove(normalized) }
        prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, updated).apply()
    }

    private fun isValidHostname(host: String): Boolean {
        if (host.isBlank() || host.length > 253 || host.startsWith(".") || host.endsWith(".")) return false
        return host.split(".").all { label ->
            label.isNotEmpty() && label.length <= 63 &&
                label.firstOrNull()?.isLetterOrDigit() == true &&
                label.lastOrNull()?.isLetterOrDigit() == true &&
                label.all { it.isLetterOrDigit() || it == "-" }
        }
    }

    companion object {
        private const val FILE_NAME = "stop_hazard_domain_policy"
        private const val KEY_CUSTOM_DOMAINS = "custom_blocked_domains"
    }
}
