package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Reads data from admitted upstream sockets.
 *
 * The loop returns raw application payloads through a callback. Packet
 * reconstruction remains in Ipv4PacketBuilder so transport I/O and packet
 * framing stay separate.
 */
class TransportReceiveLoop(
    private val transport: UpstreamTransport,
    private val onTcpData: (FlowTable.Key, ByteArray) -> Unit,
    private val onUdpData: (FlowTable.Key, ByteArray) -> Unit,
    private val onClosed: (FlowTable.Key) -> Unit
) : AutoCloseable {

    private val workers: ExecutorService = Executors.newCachedThreadPool()
    private val running = ConcurrentHashMap.newKeySet<FlowTable.Key>()

    fun watchTcp(key: FlowTable.Key) {
        if (key.protocol != 6 || !running.add(key)) return
        workers.execute {
            try {
                while (running.contains(key)) {
                    val data = transport.tcpHandle(key)?.socket?.inputStream?.readBytes(32767)
                    if (data == null || data.isEmpty()) break
                    onTcpData(key, data)
                }
            } catch (_: Exception) {
                // Flow closure is handled below.
            } finally {
                running.remove(key)
                onClosed(key)
            }
        }
    }

    fun watchUdp(key: FlowTable.Key) {
        if (key.protocol != 17 || !running.add(key)) return
        workers.execute {
            try {
                val handle = transport.udpHandle(key) ?: return@execute
                while (running.contains(key)) {
                    val buffer = ByteArray(65507)
                    val packet = java.net.DatagramPacket(buffer, buffer.size)
                    handle.socket.receive(packet)
                    if (packet.length > 0) {
                        onUdpData(key, packet.data.copyOf(packet.length))
                    }
                }
            } catch (_: Exception) {
                // Flow closure is handled below.
            } finally {
                running.remove(key)
                onClosed(key)
            }
        }
    }

    fun stop(key: FlowTable.Key) {
        running.remove(key)
        transport.close(key)
    }

    override fun close() {
        running.clear()
        workers.shutdownNow()
    }
}

private fun java.io.InputStream.readBytes(max: Int): ByteArray? {
    val buffer = ByteArray(max)
    val n = try { read(buffer) } catch (_: Exception) { return null }
    return if (n <= 0) null else buffer.copyOf(n)
}