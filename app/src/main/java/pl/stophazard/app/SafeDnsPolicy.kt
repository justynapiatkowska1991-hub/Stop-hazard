package pl.stophazard.app

object SafeDnsPolicy {
    // Resolver addresses are configuration data, not a claim that the VPN
    // service currently forwards all traffic through them.
    val resolvers=listOf("1.1.1.1","1.0.0.1","8.8.8.8","8.8.4.4")
    fun shouldBlock(host:String,custom:Set<String>)=
        BlockedDomains.isBlocked(host,custom)
}
