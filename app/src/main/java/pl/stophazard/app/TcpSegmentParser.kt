package pl.stophazard.app

/**
 * Parses TCP control bits and sequence information from an IPv4/TCP packet.
 * Performs strict bounds checks and never mutates the input packet.
 */
object TcpSegmentParser {
    data class Segment(
        val sequence: Long,
        val acknowledgement: Long,
        val syn: Boolean,
        val ack: Boolean,
        val fin: Boolean,
        val rst: Boolean,
        val payloadLength: Int
    )

    fun parse(packet: TunPacketCodec.Packet): Segment? {
        if (packet.protocol != 6) return null
        val ip = packet.headerLength
        if (packet.raw.size < ip + 20) return null

        val flags = packet.raw[ip + 13].toInt() and 0xff
        val dataOffset = ((packet.raw[ip + 12].toInt() ushr 4) and 0x0f) * 4
        if (dataOffset < 20 || packet.raw.size < ip + dataOffset) return null

        val sequence = u32(packet.raw, ip + 4)
        val acknowledgement = u32(packet.raw, ip + 8)
        val payloadLength = packet.raw.size - ip - dataOffset

        return Segment(
            sequence = sequence,
            acknowledgement = acknowledgement,
            syn = flags and 0x02 != 0,
            ack = flags and 0x10 != 0,
            fin = flags and 0x01 != 0,
            rst = flags and 0x04 != 0,
            payloadLength = payloadLength
        )
    }

    private fun u32(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 255) shl 24) or
        ((bytes[offset + 1].toLong() and 255) shl 16) or
        ((bytes[offset + 2].toLong() and 255) shl 8) or
        (bytes[offset + 3].toLong() and 255)
}