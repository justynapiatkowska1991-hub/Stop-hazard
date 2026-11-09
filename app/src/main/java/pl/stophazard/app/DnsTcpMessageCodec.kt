package pl.stophazard.app

/**
 * Codec for DNS-over-TCP messages (two-byte network-order length prefix).
 *
 * This codec validates complete length-prefixed DNS frames and creates local
 * NXDOMAIN responses for blocked queries. The experimental VPN relay uses it
 * through DnsTcpFrameFilter; end-to-end and physical-device validation remain
 * required before this path can be considered reliable.
 */
object DnsTcpMessageCodec {
    fun parseQuery(frame: ByteArray): DnsPacketFilter.Query? {
        if (frame.size < 14) return null
        val messageLength = u16(frame, 0)
        if (messageLength < 12 || messageLength != frame.size - 2) return null
        return DnsPacketFilter.parseQuery(frame.copyOfRange(2, frame.size))
    }

    /** Returns a length-prefixed NXDOMAIN frame only for a blocked DNS query. */
    fun blockedResponse(frame: ByteArray): ByteArray? {
        if (parseQuery(frame) == null) return null
        val response = DnsResponseBuilder.responseFor(frame.copyOfRange(2, frame.size)) ?: return null
        if (response.size > 0xffff) return null
        return byteArrayOf((response.size ushr 8).toByte(), response.size.toByte()) + response
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}
