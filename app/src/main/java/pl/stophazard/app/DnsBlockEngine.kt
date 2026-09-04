package pl.stophazard.app

/**
 * Decides whether DNS queries should be blocked and creates a DNS response
 * for denied requests. Networking/packet transport remains outside this class.
 */
class DnsBlockEngine(
    private val policy: DomainPolicy,
    private val statsStore: ProtectionStatsStore
) {
    data class Result(
        val blocked: Boolean,
        val hostname: String? = null,
        val response: ByteArray? = null
    )

    fun inspect(packet: ByteArray, length: Int = packet.size): Result {
        val query = DnsPacketCodec.parseQuery(packet, length)
            ?: return Result(blocked = false)

        val blocked = policy.shouldBlock(query.hostname)
        if (!blocked) {
            return Result(blocked = false, hostname = query.hostname)
        }

        statsStore.recordBlocked(query.hostname)

        return Result(
            blocked = true,
            hostname = query.hostname,
            response = DnsPacketCodec.buildNxDomainResponse(query)
        )
    }
}
