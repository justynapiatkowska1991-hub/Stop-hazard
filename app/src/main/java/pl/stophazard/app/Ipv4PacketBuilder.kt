package pl.stophazard.app

/**
 * Builds IPv4 TCP/UDP response packets for the TUN boundary.
 *
 * Checksums are recalculated after swapping the flow direction.
 * This class only constructs packets; it does not perform network I/O.
 */
object Ipv4PacketBuilder {
    fun response(query: TunPacketCodec.Packet, payload: ByteArray): ByteArray? {
        if (query.raw.size < query.headerLength + 8) return null
        return when (query.protocol) {
            17 -> udpResponse(query, payload)
            6 -> tcpPayloadResponse(query, payload)
            else -> null
        }
    }

    private fun udpResponse(q: TunPacketCodec.Packet, payload: ByteArray): ByteArray {
        val ihl = q.headerLength
        val total = ihl + 8 + payload.size
        val out = ByteArray(total)
        q.raw.copyInto(out, 0, 0, ihl)
        q.raw.copyOfRange(16, 20).copyInto(out, 12)
        q.raw.copyOfRange(12, 16).copyInto(out, 16)
        out[9] = 17

        val off = ihl
        put16(out, off, q.destinationPort)
        put16(out, off + 2, q.sourcePort)
        put16(out, off + 4, 8 + payload.size)
        put16(out, off + 6, 0)
        payload.copyInto(out, off + 8)

        finalizeIp(out, ihl)
        put16(out, off + 6, checksumUdp(out, off))
        return out
    }

    private fun tcpPayloadResponse(q: TunPacketCodec.Packet, payload: ByteArray): ByteArray {
        val ihl = q.headerLength
        val tcpOff = ihl
        val headerLen = ((q.raw[tcpOff + 12].toInt() ushr 4) and 0x0f) * 4
        val total = ihl + headerLen + payload.size
        val out = ByteArray(total)
        q.raw.copyInto(out, 0, 0, ihl)
        q.raw.copyOfRange(16, 20).copyInto(out, 12)
        q.raw.copyOfRange(12, 16).copyInto(out, 16)
        out[9] = 6
        q.raw.copyInto(out, ihl, ihl, ihl + headerLen)
        put16(out, ihl, q.destinationPort)
        put16(out, ihl + 2, q.sourcePort)
        payload.copyInto(out, ihl + headerLen)

        finalizeIp(out, ihl)
        put16(out, ihl + 16, 0)
        put16(out, ihl + 16, checksumTcp(out, ihl, headerLen + payload.size))
        return out
    }

    private fun finalizeIp(p: ByteArray, ihl: Int) {
        put16(p, 2, p.size)
        put16(p, 10, 0)
        put16(p, 10, checksum(p, 0, ihl))
    }

    private fun checksumUdp(p: ByteArray, off: Int): Int {
        put16(p, off + 6, 0)
        return transportChecksum(p, off, p.size - off, 17)
    }

    private fun checksumTcp(p: ByteArray, off: Int, len: Int): Int =
        transportChecksum(p, off, len, 6)

    private fun transportChecksum(p: ByteArray, off: Int, len: Int, protocol: Int): Int {
        var sum = pseudoHeaderSum(p, protocol, len)
        var i = off
        val end = off + len
        while (i + 1 < end) {
            sum += ((p[i].toInt() and 255) shl 8) or (p[i + 1].toInt() and 255)
            i += 2
        }
        if (i < end) sum += (p[i].toInt() and 255) shl 8
        while ((sum ushr 16) != 0L) sum = (sum and 0xffffL) + (sum ushr 16)
        val result = (sum.inv() and 0xffffL).toInt()
        return if (result == 0) 0xffff else result
    }

    private fun pseudoHeaderSum(p: ByteArray, protocol: Int, len: Int): Long {
        var sum = 0L
        for (i in 12 until 20 step 2)
            sum += ((p[i].toInt() and 255) shl 8) or (p[i + 1].toInt() and 255)
        sum += protocol
        sum += len
        return sum
    }

    private fun checksum(p: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var i = off
        val end = off + len
        while (i + 1 < end) {
            sum += ((p[i].toInt() and 255) shl 8) or (p[i + 1].toInt() and 255)
            i += 2
        }
        if (i < end) sum += (p[i].toInt() and 255) shl 8
        while ((sum ushr 16) != 0L) sum = (sum and 0xffffL) + (sum ushr 16)
        return (sum.inv() and 0xffffL).toInt()
    }

    private fun put16(p: ByteArray, i: Int, v: Int) {
        p[i] = (v ushr 8).toByte()
        p[i + 1] = v.toByte()
    }
}