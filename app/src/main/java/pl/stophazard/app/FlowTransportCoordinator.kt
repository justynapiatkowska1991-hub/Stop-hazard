package pl.stophazard.app

class FlowTransportCoordinator(
    private val policy: DomainPolicy,
    private val table: FlowTable = FlowTable(),
    private val tcpStates: TcpFlowStateTable = TcpFlowStateTable(),
    private val transport: UpstreamTransport = UpstreamTransport()
) : AutoCloseable {
    private val registry = FlowTransportRegistry(transport)

    data class Result(val allowed: Boolean, val reason: String, val flow: FlowTable.Flow? = null)

    fun admit(packet: TunPacketCodec.Packet, hostname: String? = null): Result {
        val admission = FlowAdmissionController(table, policy).admit(
            packet.protocol, packet.sourceIp, packet.sourcePort,
            packet.destinationIp, packet.destinationPort, hostname, packet.raw.size
        )
        if (admission.decision != FlowAdmissionController.Decision.ALLOW) {
            return Result(false, admission.reason)
        }
        if (packet.protocol == 6) {
            tcpStates.open(
                TcpFlowStateTable.Key(packet.sourceIp, packet.sourcePort, packet.destinationIp, packet.destinationPort),
                tcpSequence(packet)
            )
        }
        return Result(true, "flow-admitted", admission.flow)
    }

    fun openUpstream(packet: TunPacketCodec.Packet): Boolean {
        val key = FlowTable.Key(packet.protocol, packet.sourceIp, packet.sourcePort, packet.destinationIp, packet.destinationPort)
        val opened = when (packet.protocol) {
            6 -> transport.tcpHandle(key) != null || transport.openTcp(key) != null
            17 -> transport.udpHandle(key) != null || transport.openUdp(key) != null
            else -> false
        }
        if (opened) registry.register(key)
        return opened
    }

    fun closeByKey(key: FlowTable.Key) {
        registry.remove(key)
        table.close(key)
        if (key.protocol == 6) tcpStates.close(
            TcpFlowStateTable.Key(key.sourceIp, key.sourcePort, key.destinationIp, key.destinationPort)
        )
    }

    fun close(packet: TunPacketCodec.Packet) {
        val key = FlowTable.Key(packet.protocol, packet.sourceIp, packet.sourcePort, packet.destinationIp, packet.destinationPort)
        registry.remove(key)
        table.close(key)
        if (packet.protocol == 6) tcpStates.close(
            TcpFlowStateTable.Key(packet.sourceIp, packet.sourcePort, packet.destinationIp, packet.destinationPort)
        )
    }

    fun flowCount(): Int = table.size()

    private fun tcpSequence(packet: TunPacketCodec.Packet): Long {
        val o = packet.headerLength
        if (packet.raw.size < o + 8) return 0L
        return ((packet.raw[o + 4].toLong() and 255L) shl 24) or
            ((packet.raw[o + 5].toLong() and 255L) shl 16) or
            ((packet.raw[o + 6].toLong() and 255L) shl 8) or
            (packet.raw[o + 7].toLong() and 255L)
    }

    override fun close() {
        registry.clear()
        table.clear()
        tcpStates.clear()
        transport.close()
    }
}