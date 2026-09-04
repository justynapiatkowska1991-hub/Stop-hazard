package com.stophazard

import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * STOP HAZARD — centralized packet/domain policy.
 *
 * Keeps filtering decisions in one native component so future transports,
 * DNS handlers and UI code can reuse the same policy instead of duplicating
 * matching logic.
 *
 * This class only decides policy. It does not perform packet forwarding.
 */
class StopHazardPacketPolicy(
    private val strictMode: Boolean = true
) {
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()

    fun replaceBlocklist(domains: Collection<String>) {
        blockedDomains.clear()
        domains.asSequence()
            .map(::normalizeDomain)
            .filter(::validDomain)
            .forEach(blockedDomains::add)
    }

    fun addBlockedDomain(domain: String) {
        normalizeDomain(domain).takeIf(::validDomain)?.let(blockedDomains::add)
    }

    fun removeBlockedDomain(domain: String) {
        blockedDomains.remove(normalizeDomain(domain))
    }

    fun addAllowedDomain(domain: String) {
        normalizeDomain(domain).takeIf(::validDomain)?.let(allowedDomains::add)
    }

    fun removeAllowedDomain(domain: String) {
        allowedDomains.remove(normalizeDomain(domain))
    }

    fun clearAllowlist() {
        allowedDomains.clear()
    }

    fun shouldBlockHost(host: String?): Boolean {
        val domain = normalizeDomain(host ?: return false)
        if (!validDomain(domain)) return false
        if (allowedDomains.any { domain == it || domain.endsWith(".$it") }) return false
        return blockedDomains.any { domain == it || domain.endsWith(".$it") }
    }

    /**
     * Strict mode is intentionally conservative for malformed hostnames:
     * malformed data is rejected rather than treated as a valid block rule.
     */
    fun shouldBlockDnsName(name: String?): Boolean =
        shouldBlockHost(name)

    fun shouldBlockAddress(address: InetAddress?): Boolean {
        // IP reputation is deliberately not guessed here. Domain blocklists
        // should not accidentally turn into broad IP blocking.
        return false
    }

    fun blockedCount(): Int = blockedDomains.size
    fun allowedCount(): Int = allowedDomains.size
    fun isStrictMode(): Boolean = strictMode

    fun snapshot(): Map<String, Any> = mapOf(
        "strictMode" to strictMode,
        "blockedDomains" to blockedDomains.size,
        "allowedDomains" to allowedDomains.size
    )

    private fun normalizeDomain(value: String): String =
        value.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore(":")
            .trimEnd('.')

    private fun validDomain(value: String): Boolean {
        if (value.length !in 3..253) return false
        if (!value.contains('.')) return false
        if (value.startsWith('.') || value.endsWith('.')) return false
        if (value.contains(Regex("[\\\\s<>]"))) return false
        return value.split('.').all { label ->
            label.isNotEmpty() &&
            label.length <= 63 &&
            !label.startsWith('-') &&
            !label.endsWith('-') &&
            label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}