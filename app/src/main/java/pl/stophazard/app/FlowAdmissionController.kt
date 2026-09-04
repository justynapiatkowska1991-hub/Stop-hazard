package pl.stophazard.app

/**
 * Admission layer for IP flows.
 *
 * It combines flow tracking with the blocking policy. It deliberately does
 * not perform socket/NAT forwarding; a flow is admitted only after policy
 * evaluation and is then handed to the transport implementation.
 */
class FlowAdmissionController(
    private val table: FlowTable,
    private val policy: DomainPolicy
) {
    enum class Decision { ALLOW, BLOCK, UNKNOWN }

    data class Result(
        val decision: Decision,
        val flow: FlowTable.Flow?,
        val reason: String
    )

    fun admit(
        protocol: Int,
        sourceIp: String,
        sourcePort: Int,
        destinationIp: String,
        destinationPort: Int,
        hostname: String? = null,
        packetBytes: Int = 0
    ): Result {
        val normalized = hostname?.let { DomainClassifier.normalize(it) }
        if (hostname != null && (normalized == null || !normalized.valid)) {
            return Result(Decision.UNKNOWN, null, "invalid-hostname")
        }

        if (normalized?.hostname != null && policy.isBlocked(normalized.hostname)) {
            return Result(Decision.BLOCK, null, "blocked-domain")
        }

        val key = FlowTable.Key(
            protocol = protocol,
            sourceIp = sourceIp,
            sourcePort = sourcePort,
            destinationIp = destinationIp,
            destinationPort = destinationPort
        )

        val flow = table.observe(key, packetBytes)
            ?: return Result(Decision.UNKNOWN, null, "flow-capacity-reached")

        return Result(Decision.ALLOW, flow, "flow-admitted")
    }

    fun close(
        protocol: Int,
        sourceIp: String,
        sourcePort: Int,
        destinationIp: String,
        destinationPort: Int
    ) {
        table.close(
            FlowTable.Key(
                protocol, sourceIp, sourcePort, destinationIp, destinationPort
            )
        )
    }
}