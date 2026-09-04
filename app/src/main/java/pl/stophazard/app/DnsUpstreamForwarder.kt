package pl.stophazard.app

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Real upstream DNS transport for allowed DNS queries.
 * This is intentionally limited to DNS; it is not a fake "full Internet"
 * forwarder. General TCP/UDP IP forwarding still requires a user-space
 * transport/proxy implementation.
 */
class DnsUpstreamForwarder(
    private val upstreamHost: String = "1.1.1.1",
    private val upstreamPort: Int = 53,
    private val timeoutMs: Int = 2500
) : AutoCloseable {

    private var socket: DatagramSocket? = null

    @Synchronized
    fun start() {
        if (socket == null) {
            socket = DatagramSocket().apply { soTimeout = timeoutMs }
        }
    }

    @Synchronized
    fun forward(dnsPayload: ByteArray): ByteArray? {
        if (dnsPayload.isEmpty()) return null
        start()
        val s = socket ?: return null

        return try {
            val request = DatagramPacket(
                dnsPayload,
                dnsPayload.size,
                InetSocketAddress(upstreamHost, upstreamPort)
            )
            s.send(request)

            val buffer = ByteArray(4096)
            val response = DatagramPacket(buffer, buffer.size)
            s.receive(response)
            response.data.copyOf(response.length)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    override fun close() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}