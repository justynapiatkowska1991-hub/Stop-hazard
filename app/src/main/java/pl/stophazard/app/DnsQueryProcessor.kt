package pl.stophazard.app

/**
 * Complete policy pipeline for a DNS payload:
 * parse -> policy decision -> block response OR upstream resolution.
 */
class DnsQueryProcessor(
    private val engine: DnsBlockEngine,
    private val transport: DnsTransport
) {
    data class ProcessingResult(
        val response: ByteArray?,
        val blocked: Boolean,
        val hostname: String?
    )

    fun process(packet: ByteArray, length: Int = packet.size): ProcessingResult {
        val decision = engine.inspect(packet, length)

        if (decision.blocked) {
            return ProcessingResult(
                response = decision.response,
                blocked = true,
                hostname = decision.hostname
            )
        }

        val response = transport.query(packet.copyOf(length))
        return ProcessingResult(
            response = response,
            blocked = false,
            hostname = decision.hostname
        )
    }
}
