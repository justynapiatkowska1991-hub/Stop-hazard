package pl.stophazard.app

import android.content.Context
import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * DNS upstream transport used by the VPN engine.
 *
 * The socket is protected from the VPN itself to avoid routing the upstream
 * request back through the same tunnel. If protection fails, the request is
 * rejected rather than creating a routing loop.
 */
class DnsTransport(
    private val vpnService: VpnService? = null,
    private val upstream: InetSocketAddress = InetSocketAddress(
        InetAddress.getByName("1.1.1.1"),
        53
    ),
    private val timeoutMs: Int = 2500,
    private val maxResponseBytes: Int = 4096
) {

    fun query(request: ByteArray): ByteArray? {
        if (request.isEmpty() || request.size > maxResponseBytes) return null

        return try {
            DatagramSocket().use { socket ->
                if (vpnService != null && !vpnService.protect(socket)) {
                    return null
                }

                socket.soTimeout = timeoutMs

                val outgoing = DatagramPacket(
                    request,
                    request.size,
                    upstream.address,
                    upstream.port
                )
                socket.send(outgoing)

                val buffer = ByteArray(maxResponseBytes)
                val incoming = DatagramPacket(buffer, buffer.size)
                socket.receive(incoming)

                if (incoming.length < 12) return null
                incoming.data.copyOf(incoming.length)
            }
        } catch (_: Exception) {
            null
        }
    }
}
