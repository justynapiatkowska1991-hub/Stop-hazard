package pl.stophazard.app

/**
 * Minimal IPv4 classifier used by the VPN transport layer.
 * It identifies TCP/UDP packets and extracts their destination tuple.
 */
object IpPacketClassifier {
    data class Flow(
        val protocol: Int,
        val destinationAddress: String,
        val destinationPort: Int,
        val headerLength: Int
    )

    fun classify(packet: ByteArray): Flow? {
        if (packet.size < 20) return null
        if ((packet[0].toInt() ushr 4) != 4) return null
        val ihl = (packet[0].toInt() and 0x0f) * 4
        if (ihl < 20 || packet.size < ihl) return null
        val protocol = packet[9].toInt() and 0xff
        if (protocol != 6 && protocol != 17) return null
        val portOffset = ihl
        if (packet.size < portOffset + 4) return null
        val port = ((packet[portOffset + 2].toInt() and 255) shl 8) or
            (packet[portOffset + 3].toInt() and 255)
        val ip = (12 until 16).joinToString(".") { (packet[it].toInt() and 255).toString() }
        return Flow(protocol, ip, port, ihl)
    }
}