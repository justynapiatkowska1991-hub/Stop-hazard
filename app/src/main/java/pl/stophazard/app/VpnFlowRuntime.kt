package pl.stophazard.app

/**
 * Owns the runtime objects that connect TUN flow admission with upstream
 * transport and response routing. Kept separate from the Android Service so
 * lifecycle and transport wiring can be tested independently.
 */
class VpnFlowRuntime(
    private val policy: DomainPolicy,
    private val tunOutput: java.io.FileOutputStream
) : AutoCloseable {

    private val upstream = UpstreamTransport()
    private val coordinator = FlowTransportCoordinator(policy, transport = upstream)
    private val responseRouter = VpnResponseRouter(tunOutput)
    private val receiveLoop = TransportReceiveLoop(
        transport = upstream,
        onTcpData = { key, data -> responseRouter.route(key, data) },
        onUdpData = { key, data -> responseRouter.route(key, data) },
        onClosed = { key ->
            responseRouter.unregister(key)
            coordinator.closeByKey(key)
        }
    )

    fun admit(packet: TunPacketCodec.Packet, hostname: String? = null): FlowTransportCoordinator.Result {
        val result = coordinator.admit(packet, hostname)
        if (!result.allowed) return result

        responseRouter.register(packet)
        if (!coordinator.openUpstream(packet)) {
            responseRouter.unregister(packetKey(packet))
            coordinator.close(packet)
            return FlowTransportCoordinator.Result(false, "upstream-open-failed")
        }

        val key = packetKey(packet)
        if (packet.protocol == 6) receiveLoop.watchTcp(key)
        if (packet.protocol == 17) receiveLoop.watchUdp(key)
        return result
    }

    fun close(packet: TunPacketCodec.Packet) {
        val key = packetKey(packet)
        receiveLoop.stop(key)
        responseRouter.unregister(key)
        coordinator.close(packet)
    }

    fun activeFlows(): Int = coordinator.flowCount()

    private fun packetKey(p: TunPacketCodec.Packet) = FlowTable.Key(
        p.protocol, p.sourceIp, p.sourcePort, p.destinationIp, p.destinationPort
    )

    override fun close() {
        receiveLoop.close()
        responseRouter.clear()
        coordinator.close()
    }
}