package pl.stophazard.app

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Small isolated DNS transport used for resolving allowed queries.
 * It is intentionally independent from the VPN file descriptor so it can be
 * tested and replaced without changing policy logic.
 */
class DnsTransport(
    private val upstream: InetSocketAddress = InetSocketAddress("1.1.1.1", 53),
    private val timeoutMs: Int = 2500
) {
    fun query(request: ByteArray): ByteArray? {
        if (request.isEmpty()) return null

        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val destination = DatagramPacket(
                    request,
                    request.size,
                    upstream.address,
                    upstream.port
                )
                socket.send(destination)

                val buffer = ByteArray(4096)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                response.data.copyOf(response.length)
            }
        } catch (_: Exception) {
            null
        }
    }
}
