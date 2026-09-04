package pl.stophazard.app

/**
 * Conservative validation for UDP flows entering the VPN runtime.
 *
 * UDP has no handshake, so validation is limited to valid addressing,
 * ports and payload size. This keeps malformed datagrams out of the
 * upstream transport without pretending UDP has TCP-like state.
 */
object UdpFlowValidator {
    private const val MAX_UDP_PAYLOAD = 65507

    fun validate(packet: TunPacketCodec.Packet): Boolean {
        if (packet.protocol != 17) return false
        if (!validIp(packet.sourceIp) || !validIp(packet.destinationIp)) return false
        if (packet.sourcePort !in 1..65535) return false
        if (packet.destinationPort !in 1..65535) return false
        return packet.payload.size <= MAX_UDP_PAYLOAD
    }

    private fun validIp(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all(Char::isDigit) &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }
}