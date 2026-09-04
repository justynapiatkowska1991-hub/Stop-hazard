package pl.stophazard.app

/**
 * Conservative IPv4 codec for the VPN transport boundary.
 * Validates packet structure and exposes TCP/UDP payload offsets.
 */
object TunPacketCodec {
    data class Packet(
        val raw: ByteArray,
        val headerLength: Int,
        val protocol: Int,
        val sourceIp: String,
        val destinationIp: String,
        val sourcePort: Int,
        val destinationPort: Int,
        val payloadOffset: Int,
        val payload: ByteArray
    )

    fun parse(raw: ByteArray): Packet? {
        if (raw.size < 28) return null
        if ((raw[0].toInt() ushr 4) != 4) return null
        val ihl = (raw[0].toInt() and 0x0f) * 4
        if (ihl < 20 || raw.size < ihl + 8) return null

        val protocol = raw[9].toInt() and 0xff
        if (protocol != 6 && protocol != 17) return null

        val totalLength = ((raw[2].toInt() and 255) shl 8) or
            (raw[3].toInt() and 255)
        if (totalLength < ihl + 8 || totalLength > raw.size) return null

        val sourceIp = ip(raw, 12)
        val destinationIp = ip(raw, 16)
        val sourcePort = u16(raw, ihl)
        val destinationPort = u16(raw, ihl + 2)
        val transportHeader = if (protocol == 6) {
            if (raw.size < ihl + 20) return null
            ((raw[ihl + 12].toInt() ushr 4) and 0x0f) * 4
        } else 8

        if (transportHeader < 8 || totalLength < ihl + transportHeader) return null
        val payloadOffset = ihl + transportHeader
        val payload = raw.copyOfRange(payloadOffset, totalLength)

        return Packet(
            raw.copyOf(totalLength), ihl, protocol, sourceIp, destinationIp,
            sourcePort, destinationPort, payloadOffset, payload
        )
    }

    private fun u16(p: ByteArray, i: Int): Int =
        ((p[i].toInt() and 255) shl 8) or (p[i + 1].toInt() and 255)

    private fun ip(p: ByteArray, i: Int): String =
        (i until i + 4).joinToString(".") { (p[it].toInt() and 255).toString() }
}