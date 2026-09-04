package pl.stophazard.app

/**
 * Safe boundary between decoded TUN packets and upstream transports.
 *
 * It forwards only payload bytes to the already-admitted socket and returns
 * received bytes to the caller. Packet framing, NAT and TCP sequence handling
 * remain explicit responsibilities of the VPN service.
 */
class TransportBridge(private val transport: UpstreamTransport) {

    fun send(packet: TunPacketCodec.Packet): Boolean {
        val key = FlowTable.Key(
            packet.protocol,
            packet.sourceIp,
            packet.sourcePort,
            packet.destinationIp,
            packet.destinationPort
        )

        return try {
            when (packet.protocol) {
                6 -> {
                    val handle = transport.tcpHandle(key) ?: return false
                    handle.socket.outputStream.use { out ->
                        out.write(packet.payload)
                        out.flush()
                    }
                    true
                }
                17 -> {
                    val handle = transport.udpHandle(key) ?: return false
                    handle.socket.send(
                        java.net.DatagramPacket(
                            packet.payload,
                            packet.payload.size
                        )
                    )
                    true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun receiveTcp(key: FlowTable.Key, maxBytes: Int = 32767): ByteArray? {
        val handle = transport.tcpHandle(key) ?: return null
        return try {
            val buffer = ByteArray(maxBytes.coerceIn(1, 32767))
            val n = handle.socket.inputStream.read(buffer)
            if (n <= 0) null else buffer.copyOf(n)
        } catch (_: Exception) {
            null
        }
    }

    fun receiveUdp(key: FlowTable.Key, maxBytes: Int = 65507): ByteArray? {
        val handle = transport.udpHandle(key) ?: return null
        return try {
            val buffer = ByteArray(maxBytes.coerceIn(1, 65507))
            val packet = java.net.DatagramPacket(buffer, buffer.size)
            handle.socket.receive(packet)
            packet.data.copyOf(packet.length)
        } catch (_: Exception) {
            null
        }
    }
}