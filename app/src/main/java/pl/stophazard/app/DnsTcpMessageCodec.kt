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

    /**
     * Confirms that an upstream DNS/TCP frame is a response to the given query.
     * Transaction IDs are checked so an unrelated or stale frame is never
     * delivered as the answer to the current serialized exchange.
     */
    fun isResponseFor(queryFrame: ByteArray, responseFrame: ByteArray): Boolean {
        if (parseQuery(queryFrame) == null || responseFrame.size < 14) return false
        val responseLength = u16(responseFrame, 0)
        if (responseLength < 12 || responseLength != responseFrame.size - 2) return false
        val queryId = u16(queryFrame, 2)
        val responseId = u16(responseFrame, 2)
        val flags = u16(responseFrame, 4)
        return queryId == responseId && (flags and 0x8000) != 0
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}
