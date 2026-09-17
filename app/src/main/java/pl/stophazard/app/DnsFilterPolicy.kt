package pl.stophazard.app

/**
 * Polityka dla przyszłego transportu DNS.
 * Blokuje wyłącznie domeny hazardowe; pozostałe są dozwolone.
 * Ta klasa nie przejmuje ruchu i sama nie uruchamia VPN.
 */
object DnsFilterPolicy {
    enum class Decision { BLOCK, ALLOW }

    fun decide(host: String?): Decision {
        val normalized = normalize(host) ?: return Decision.ALLOW
        return if (BlockedDomains.isBlocked(normalized)) Decision.BLOCK else Decision.ALLOW
    }

    fun normalize(host: String?): String? {
        var value = host?.trim()?.lowercase() ?: return null
        if (value.isEmpty() || value.length > 253) return null
        if (value.endsWith('.')) value = value.dropLast(1)
        if (value.startsWith('[') && value.endsWith(']')) return null
        if (value.contains('/') || value.any { it.isWhitespace() }) return null
        if (value.contains(':')) return null // adres IPv6, nie nazwa domenowa
        if (value.startsWith('.') || value.endsWith('.') || value.contains("..")) return null

        val labels = value.split('.')
        if (labels.any { it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-') }) {
            return null
        }
        if (labels.any { label -> label.any { ch -> !(ch.isLetterOrDigit() || ch == '-' || ch == '_') } }) {
            return null
        }
        return value
    }
}
