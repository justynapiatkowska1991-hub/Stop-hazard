package pl.stophazard.app

import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Conservative upstream transport adapter.
 *
 * Provides real outbound TCP/UDP sockets for admitted flows, but keeps the
 * packet-to-socket protocol translation outside this class. This separation
 * prevents malformed raw TUN packets from being treated as application data.
 */
class UpstreamTransport(
    private val connectTimeoutMs: Int = 5000
) : AutoCloseable {

    data class TcpHandle(
        val socket: Socket,
        val key: FlowTable.Key
    )

    data class UdpHandle(
        val socket: DatagramSocket,
        val key: FlowTable.Key
    )

    private val tcp = ConcurrentHashMap<FlowTable.Key, TcpHandle>()
    private val udp = ConcurrentHashMap<FlowTable.Key, UdpHandle>()
    private val executor = Executors.newCachedThreadPool()

    fun openTcp(key: FlowTable.Key): TcpHandle? {
        if (key.protocol != 6) return null
        return try {
            val socket = Socket()
            socket.connect(
                InetSocketAddress(key.destinationIp, key.destinationPort),
                connectTimeoutMs
            )
            socket.soTimeout = connectTimeoutMs
            socket.tcpNoDelay = true
            TcpHandle(socket, key).also { tcp[key] = it }
        } catch (_: Exception) {
            null
        }
    }

    fun openUdp(key: FlowTable.Key): UdpHandle? {
        if (key.protocol != 17) return null
        return try {
            val socket = DatagramSocket()
            socket.connect(
                InetSocketAddress(key.destinationIp, key.destinationPort)
            )
            UdpHandle(socket, key).also { udp[key] = it }
        } catch (_: Exception) {
            null
        }
    }

    fun tcpHandle(key: FlowTable.Key): TcpHandle? = tcp[key]
    fun udpHandle(key: FlowTable.Key): UdpHandle? = udp[key]

    fun close(key: FlowTable.Key) {
        tcp.remove(key)?.let { try { it.socket.close() } catch (_: Exception) {} }
        udp.remove(key)?.let { try { it.socket.close() } catch (_: Exception) {} }
    }

    fun closeIdle() {
        // Socket lifetime is owned by the flow table/controller. This method
        // is intentionally explicit rather than guessing application state.
    }

    override fun close() {
        tcp.values.forEach { try { it.socket.close() } catch (_: Exception) {} }
        udp.values.forEach { try { it.socket.close() } catch (_: Exception) {} }
        tcp.clear()
        udp.clear()
        executor.shutdownNow()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }
}