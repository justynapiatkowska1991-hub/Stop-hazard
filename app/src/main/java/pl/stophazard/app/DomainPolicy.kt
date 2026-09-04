package pl.stophazard.app

data class DomainRule(val domain:String,val category:String="hazard",val enabled:Boolean=true)

class DomainPolicy(private val repository:DomainRepository) {
    fun isBlocked(host:String):Boolean=repository.isBlocked(host)
    fun add(domain:String):Boolean=repository.add(domain)
    fun remove(domain:String)=repository.remove(domain)
}
