package pl.stophazard.app

/**
 * Coordinates policy admission, flow state and upstream socket ownership.
 *
 * It deliberately keeps raw packet translation outside this class. A flow
 * becomes transport-backed only after policy admission succeeds.
 */
class FlowTransportCoordinator(
    private val policy: DomainPolicy,
    private val table: FlowTable = FlowTable(),
    private val tcpStates: TcpFlowStateTable = TcpFlowStateTable(),
    private val transport: UpstreamTransport = UpstreamTransport()
) : AutoCloseable {

    data class Result(
        val allowed: Boolean,
        val reason: String,
        val flow: FlowTable.Flow? = null
    )

    fun admit(packet: TunPacketCodec.Packet, hostname: String? = null): Result {
        val admission = FlowAdmissionController(table, policy).admit(
            protocol = packet.protocol,
            sourceIp = packet.sourceIp,
            sourcePort = packet.sourcePort,
            destinationIp = packet.destinationIp,
            destinationPort = packet.destinationPort,
            hostname = hostname,
            packetBytes = packet.raw.size
        )

        if (admission.decision != FlowAdmissionController.Decision.ALLOW) {
            return Result(false, admission.reason)
        }

        if (packet.protocol == 6) {
            val key = TcpFlowStateTable.Key(
                packet.sourceIp, packet.sourcePort,
                packet.destinationIp, packet.destinationPort
            )
            tcpStates.open(key, tcpSequence(packet))
        }

        return Result(true, "flow-admitted", admission.flow)
    }

    fun openUpstream(packet: TunPacketCodec.Packet): Boolean {
        val key = FlowTable.Key(
            packet.protocol,
            packet.sourceIp, packet.sourcePort,
            packet.destinationIp, packet.destinationPort
        )

        return when (packet.protocol) {
            6 -> (transport.tcpHandle(key) != null || transport.openTcp(key) != null)
            17 -> (transport.udpHandle(key) != null || transport.openUdp(key) != null)
            else -> false
        }
    }

    fun closeByKey(key: FlowTable.Key) {
        table.close(key)
        if (key.protocol == 6) {
            tcpStates.close(
                TcpFlowStateTable.Key(
                    key.sourceIp, key.sourcePort,
                    key.destinationIp, key.destinationPort
                )
            )
        }
    }

    fun close(packet: TunPacketCodec.Packet) {
        val key = FlowTable.Key(
            packet.protocol,
            packet.sourceIp, packet.sourcePort,
            packet.destinationIp, packet.destinationPort
        )
        transport.close(key)
        table.close(key)
        if (packet.protocol == 6) {
            tcpStates.close(
                TcpFlowStateTable.Key(
                    packet.sourceIp, packet.sourcePort,
                    packet.destinationIp, packet.destinationPort
                )
            )
        }
    }

    fun flowCount(): Int = table.size()

    private fun tcpSequence(packet: TunPacketCodec.Packet): Long {
        val o = packet.headerLength
        if (packet.raw.size < o + 8) return 0
        return ((packet.raw[o + 4].toLong() and 255) shl 24) or
            ((packet.raw[o + 5].toLong() and 255) shl 16) or
            ((packet.raw[o + 6].toLong() and 255) shl 8) or
            (packet.raw[o + 7].toLong() and 255)
    }

    override fun close() {
        table.clear()
        tcpStates.clear()
        transport.close()
    }
}