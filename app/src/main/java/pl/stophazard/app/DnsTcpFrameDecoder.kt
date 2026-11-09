package pl.stophazard.app

/**
 * Incremental DNS-over-TCP frame decoder.
 *
 * Accepts arbitrary TCP read boundaries and can emit multiple complete DNS
 * frames from one read. It retains at most one DNS message (65,537 bytes).
 */
class DnsTcpFrameDecoder {
    private val prefix = ByteArray(2)
    private var prefixSize = 0
    private var frame: ByteArray? = null
    private var frameSize = 0

    @Synchronized
    fun feed(bytes: ByteArray): List<ByteArray> {
        val completed = ArrayList<ByteArray>()
        var offset = 0

        while (offset < bytes.size) {
            if (frame == null) {
                while (prefixSize < 2 && offset < bytes.size) {
                    prefix[prefixSize++] = bytes[offset++]
                }
                if (prefixSize < 2) break

                val messageLength = ((prefix[0].toInt() and 0xff) shl 8) or
                    (prefix[1].toInt() and 0xff)
                if (messageLength < 12) {
                    reset()
                    throw IllegalArgumentException("Invalid DNS-over-TCP message length: $messageLength")
                }
                frame = ByteArray(messageLength + 2)
                frame!![0] = prefix[0]
                frame!![1] = prefix[1]
                frameSize = 2
            }

            val current = frame!!
            val count = minOf(current.size - frameSize, bytes.size - offset)
            bytes.copyInto(current, frameSize, offset, offset + count)
            frameSize += count
            offset += count

            if (frameSize == current.size) {
                completed += current
                frame = null
                frameSize = 0
                prefixSize = 0
            }
        }
        return completed
    }

    @Synchronized
    fun reset() {
        prefixSize = 0
        frame = null
        frameSize = 0
    }
}
