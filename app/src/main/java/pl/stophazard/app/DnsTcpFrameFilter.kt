package pl.stophazard.app

/**
 * Applies the domain policy to one complete DNS-over-TCP frame.
 *
 * Blocked queries are replaced with a locally generated NXDOMAIN response.
 * Allowed queries, DNS responses, and unrecognized payloads are returned
 * unchanged for upstream forwarding.
 */
object DnsTcpFrameFilter {
    data class Decision(val frame: ByteArray, val blocked: Boolean)

    fun inspect(frame: ByteArray): Decision {
        val blockedResponse = DnsTcpMessageCodec.blockedResponse(frame)
        return if (blockedResponse != null) {
            Decision(blockedResponse, true)
        } else {
            Decision(frame, false)
        }
    }
}
