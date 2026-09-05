package pl.stophazard.app

class DomainPolicy(private val repository: DomainRepository) {
    private val customDomains: Set<String>? = null
    constructor(customDomains: Set<String>) : this(DomainRepositoryProxy(customDomains))

    fun isBlocked(host: String): Boolean =
        repository.isBlocked(host)

    fun add(domain: String): Boolean = repository.add(domain)
    fun remove(domain: String) = repository.remove(domain)
    fun shouldBlock(host: String): Boolean = isBlocked(host)

    private class DomainRepositoryProxy(private val domains: Set<String>) : DomainRepositoryProxyBase()
    private open class DomainRepositoryProxyBase {
        fun isBlocked(host: String): Boolean = BlockedDomains.isBlocked(host, emptySet())
    }
}
