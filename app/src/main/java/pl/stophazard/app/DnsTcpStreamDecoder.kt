package pl.stophazard.app

/**
 * Incremental decoder for the two-byte length-prefixed DNS-over-TCP stream.
 *
 * TCP reads are arbitrary byte chunks: a length prefix or DNS message may be
 * split across reads, and one read may contain multiple DNS messages. This
 * class only frames messages; callers must still validate each frame with
 * [DnsTcpMessageCodec] and must not assume the payload is a valid DNS query.
 */
class DnsTcpStreamDecoder {
    private var pending = ByteArray(0)

    /**
     * Appends bytes and returns every complete DNS frame in stream order.
     * Throws on an impossible DNS message length and clears buffered state.
     */
    @Synchronized
    fun feed(chunk: ByteArray): List<ByteArray> {
        if (chunk.isEmpty()) return emptyList()

        val combined = ByteArray(pending.size + chunk.size)
        pending.copyInto(combined)
        chunk.copyInto(combined, destinationOffset = pending.size)

        val frames = ArrayList<ByteArray>()
        var offset = 0
        while (combined.size - offset >= 2) {
            val length = ((combined[offset].toInt() and 0xff) shl 8) or
                (combined[offset + 1].toInt() and 0xff)
            if (length < 12) {
                pending = ByteArray(0)
                throw IllegalArgumentException("Invalid DNS-over-TCP message length: $length")
            }
            val frameEnd = offset + 2 + length
            if (frameEnd > combined.size) break
            frames += combined.copyOfRange(offset, frameEnd)
            offset = frameEnd
        }

        pending = combined.copyOfRange(offset, combined.size)
        return frames
    }

    @Synchronized
    fun reset() {
        pending = ByteArray(0)
    }

    @Synchronized
    fun bufferedByteCount(): Int = pending.size
}
