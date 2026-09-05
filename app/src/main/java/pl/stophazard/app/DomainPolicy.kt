package pl.stophazard.app

class DomainPolicy(private val repository: DomainRepository) {
    fun isBlocked(host: String): Boolean = repository.isBlocked(host)
    fun add(domain: String): Boolean = repository.add(domain)
    fun remove(domain: String) = repository.remove(domain)
    fun shouldBlock(host: String): Boolean = isBlocked(host)
}
