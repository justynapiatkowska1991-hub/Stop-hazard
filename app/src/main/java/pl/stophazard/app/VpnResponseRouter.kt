package pl.stophazard.app

import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Routes upstream payloads back to the matching TUN flow.
 *
 * Only the flow's original 5-tuple is used for lookup. This prevents data
 * from one upstream connection being written as a response to another flow.
 * TCP sequence/flag synthesis remains outside this small routing layer.
 */
class VpnResponseRouter(
    private val tunOutput: FileOutputStream
) {
    private val flows = ConcurrentHashMap<FlowTable.Key, TunPacketCodec.Packet>()

    fun register(packet: TunPacketCodec.Packet) {
        val key = FlowTable.Key(
            packet.protocol,
            packet.sourceIp,
            packet.sourcePort,
            packet.destinationIp,
            packet.destinationPort
        )
        flows[key] = packet
    }

    fun unregister(key: FlowTable.Key) {
        flows.remove(key)
    }

    @Synchronized
    fun route(key: FlowTable.Key, payload: ByteArray): Boolean {
        val original = flows[key] ?: return false
        val response = Ipv4PacketBuilder.response(original, payload) ?: return false
        return try {
            tunOutput.write(response)
            tunOutput.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clear() = flows.clear()
    fun size(): Int = flows.size
}