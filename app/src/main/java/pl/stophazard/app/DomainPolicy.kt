package pl.stophazard.app

class DomainPolicy private constructor(
    private val repository: DomainRepository?,
    private val customDomains: Set<String>?
) {
    constructor(repository: DomainRepository) : this(repository, null)
    constructor(customDomains: Set<String>) : this(null, customDomains)

    fun isBlocked(host: String): Boolean =
        customDomains?.let { BlockedDomains.isBlocked(host, it) }
            ?: repository?.isBlocked(host)
            ?: false

    fun add(domain: String): Boolean = repository?.add(domain) ?: false
    fun remove(domain: String) { repository?.remove(domain) }
    fun shouldBlock(host: String): Boolean = isBlocked(host)
}