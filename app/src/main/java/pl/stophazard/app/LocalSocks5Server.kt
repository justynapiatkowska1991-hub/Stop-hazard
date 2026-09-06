package pl.stophazard.app

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Minimal local SOCKS5 CONNECT server used as the direct upstream for tun2socks.
 *
 * It intentionally listens only on 127.0.0.1. The upstream socket is protected
 * by VpnService before connecting, so the connection does not loop back into TUN.
 *
 * SOCKS5 wire format follows RFC 1928.
 */
class LocalSocks5Server(
    private val protectSocket: (Socket) -> Boolean,
    private val isBlocked: (String) -> Boolean
) {
    companion object {
        const val PORT = 1080
    }

    @Volatile
    private var running = false

    private var server: ServerSocket? = null
    private val workers = Executors.newCachedThreadPool()

    fun start() {
        if (running) return
        running = true
        server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress("127.0.0.1", PORT))
        }

        thread(name = "StopHazard-SOCKS5") {
            while (running) {
                try {
                    val client = server?.accept() ?: break
                    workers.execute { handleClient(client) }
                } catch (_: Exception) {
                    if (!running) break
                }
            }
        }
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
        workers.shutdownNow()
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 15000
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // Greeting: VER, NMETHODS, METHODS. We support NO AUTH.
            if (input.readUnsignedByte() != 5) return
            val methodCount = input.readUnsignedByte()
            val methods = ByteArray(methodCount)
            input.readFully(methods)
            if (!methods.contains(0.toByte())) {
                output.write(byteArrayOf(5, 0xFF.toByte()))
                output.flush()
                return
            }

            output.write(byteArrayOf(5, 0))
            output.flush()

            if (input.readUnsignedByte() != 5) return
            val command = input.readUnsignedByte()
            input.readUnsignedByte() // RSV
            val addressType = input.readUnsignedByte()

            val host = when (addressType) {
                1 -> {
                    val bytes = ByteArray(4)
                    input.readFully(bytes)
                    InetAddress.getByAddress(bytes).hostAddress
                }
                3 -> {
                    val length = input.readUnsignedByte()
                    val bytes = ByteArray(length)
                    input.readFully(bytes)
                    String(bytes, Charsets.US_ASCII)
                }
                4 -> {
                    val bytes = ByteArray(16)
                    input.readFully(bytes)
                    InetAddress.getByAddress(bytes).hostAddress
                }
                else -> {
                    sendFailure(output, 8)
                    return
                }
            }

            val port = input.readUnsignedShort()

            if (command != 1) {
                // This first implementation intentionally supports CONNECT only.
                // Hev can fall back from UDP/QUIC to TCP for web traffic.
                sendFailure(output, 7)
                return
            }

            if (isBlocked(host)) {
                sendFailure(output, 2)
                return
            }

            val upstream = Socket()
            try {
                if (!protectSocket(upstream)) {
                    sendFailure(output, 1)
                    upstream.close()
                    return
                }

                upstream.connect(InetSocketAddress(host, port), 10000)
                upstream.soTimeout = 0

                sendSuccess(output, upstream.localAddress, upstream.localPort)

                relay(socket, upstream)
            } catch (_: Exception) {
                sendFailure(output, 5)
            } finally {
                try { upstream.close() } catch (_: Exception) {}
            }
        }
    }

    private fun relay(client: Socket, upstream: Socket) {
        val a = thread(name = "StopHazard-SOCKS-c2u") {
            try {
                client.getInputStream().copyTo(upstream.getOutputStream())
            } catch (_: Exception) {}
            try { upstream.shutdownOutput() } catch (_: Exception) {}
        }

        val b = thread(name = "StopHazard-SOCKS-u2c") {
            try {
                upstream.getInputStream().copyTo(client.getOutputStream())
            } catch (_: Exception) {}
            try { client.shutdownOutput() } catch (_: Exception) {}
        }

        a.join()
        b.join()
    }

    private fun sendSuccess(output: DataOutputStream, address: InetAddress, port: Int) {
        val bytes = address.address
        output.writeByte(5)
        output.writeByte(0)
        output.writeByte(0)
        output.writeByte(if (bytes.size == 16) 4 else 1)
        output.write(bytes)
        output.writeShort(port)
        output.flush()
    }

    private fun sendFailure(output: DataOutputStream, code: Int) {
        output.write(byteArrayOf(5, code.toByte(), 0, 1, 0, 0, 0, 0, 0, 0))
        output.flush()
    }
}
