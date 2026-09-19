package pl.stophazard.app

import android.net.VpnService
import android.os.ParcelFileDescriptor
import dev.netvalve.bridge.Bridge
import dev.netvalve.bridge.Handler
import dev.netvalve.bridge.TCPConn
import dev.netvalve.bridge.Tunnel
import dev.netvalve.bridge.UDPConn
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production traffic engine adapter for the pinned NetValve/gVisor AAR.
 *
 * This source set is compiled only for the production integration build.
 * The factory remains disabled until the transport and blocking tests pass.
 */
class NetValveTrafficFilterEngine(
    private val vpnService: VpnService,
) : TrafficFilterEngine {
    private val running = AtomicBoolean(false)
    private val workers: ExecutorService = Executors.newCachedThreadPool()
    private var tunnel: Tunnel? = null
    private var tun: ParcelFileDescriptor? = null

    override fun start(): Boolean {
        if (running.get()) return true
        if (VpnService.prepare(vpnService) != null) return false

        val localTun = try {
            vpnService.Builder()
                .setSession("STOP HAZARD")
                .setMtu(MTU)
                .addAddress(TUN_IPV4, 32)
                .addAddress(TUN_IPV6, 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer(DNS_IPV4)
                .addDnsServer(DNS_IPV6)
                .setBlocking(true)
                .establish()
        } catch (_: Throwable) {
            null
        } ?: return false

        tun = localTun

        return try {
            val fd = localTun.detachFd()
            tunnel = Bridge.newTunnel(fd.toLong(), MTU.toLong(), "RELAY", TrafficHandler())
            running.set(true)
            true
        } catch (_: Throwable) {
            runCatching { localTun.close() }
            tun = null
            false
        }
    }

    override fun stop() {
        running.set(false)
        runCatching { tunnel?.stop() }
        tunnel = null
        runCatching { tun?.close() }
        tun = null
        workers.shutdownNow()
    }

    override fun isRunning(): Boolean = running.get()

    private inner class TrafficHandler : Handler {
        override fun handleTCP(srcIp: String, srcPort: Long, dstIp: String, dstPort: Long, conn: TCPConn) {
            workers.execute { relayTcp(dstIp, dstPort.toInt(), conn) }
        }

        override fun handleUDP(srcIp: String, srcPort: Long, dstIp: String, dstPort: Long, conn: UDPConn) {
            workers.execute { relayUdp(dstIp, dstPort.toInt(), conn) }
        }

        override fun log(level: Long, msg: String) = Unit
    }

    private fun relayTcp(destinationHost: String, destinationPort: Int, appSide: TCPConn) {
        val first = ByteArray(FIRST_PACKET_LIMIT)
        var upstream: Socket? = null
        try {
            val firstLength = appSide.read(first).toInt()
            if (firstLength <= 0) return

            val host = detectHost(first, firstLength)
            if (host != null && BlockedDomains.isBlocked(host)) return

            upstream = Socket()
            if (!vpnService.protect(upstream)) return
            upstream.tcpNoDelay = true
            upstream.connect(
                InetSocketAddress(destinationHost, destinationPort),
                TCP_CONNECT_TIMEOUT_MS,
            )

            val socket = upstream
            socket.getOutputStream().write(first, 0, firstLength)
            socket.getOutputStream().flush()

            val downstream = Thread {
                val buffer = ByteArray(RELAY_BUFFER)
                try {
                    while (running.get()) {
                        val count = socket.getInputStream().read(buffer)
                        if (count <= 0) break
                        appSide.write(buffer.copyOf(count))
                    }
                } catch (_: Throwable) {
                    // Closing one direction closes the flow below.
                }
            }
            downstream.start()

            val buffer = ByteArray(RELAY_BUFFER)
            while (running.get()) {
                val count = appSide.read(buffer)
                if (count <= 0L) break
                socket.getOutputStream().write(buffer, 0, count.toInt())
                socket.getOutputStream().flush()
            }
            runCatching { downstream.join(TCP_JOIN_TIMEOUT_MS) }
        } catch (_: Throwable) {
            // One bad flow must never stop the VPN engine.
        } finally {
            runCatching { upstream?.close() }
            runCatching { appSide.close() }
        }
    }

    private fun relayUdp(destinationHost: String, destinationPort: Int, appSide: UDPConn) {
        var upstream: DatagramSocket? = null
        try {
            upstream = DatagramSocket()
            if (!vpnService.protect(upstream)) return
            upstream.connect(InetSocketAddress(destinationHost, destinationPort))
            upstream.soTimeout = UDP_TIMEOUT_MS

            while (running.get()) {
                val data = appSide.receive() ?: break
                if (data.isEmpty()) continue

                if (destinationPort == DNS_PORT) {
                    val blockedResponse = DnsResponseBuilder.responseFor(data)
                    if (blockedResponse != null) {
                        appSide.send(blockedResponse)
                        continue
                    }
                }

                upstream.send(DatagramPacket(data, data.size))
                val buffer = ByteArray(UDP_BUFFER)
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    upstream.receive(packet)
                    appSide.send(packet.data.copyOf(packet.length))
                } catch (_: IOException) {
                    // Timeout: wait for the next app datagram.
                }
            }
        } catch (_: Throwable) {
            // One bad flow must never stop the VPN engine.
        } finally {
            runCatching { upstream?.close() }
            runCatching { appSide.close() }
        }
    }

    private fun detectHost(data: ByteArray, length: Int): String? {
        val tlsHost = parseTlsSni(data, length)
        if (tlsHost != null) return tlsHost

        val text = runCatching {
            data.copyOf(length).toString(Charset.forName("ISO-8859-1"))
        }.getOrNull() ?: return null

        return Regex("(?im)^Host\\s*:\\s*([^\\s:/]+)(?::\\d+)?\\s*$")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trimEnd('.')
    }

    private fun parseTlsSni(data: ByteArray, length: Int): String? {
        if (length < 5 || (data[0].toInt() and 0xff) != 22) return null
        val recordLength = u16(data, 3)
        if (recordLength <= 0 || 5 + recordLength > length) return null

        var p = 5
        if (p + 4 > length || (data[p].toInt() and 0xff) != 1) return null
        val helloLength = u24(data, p + 1)
        p += 4
        if (p + helloLength > length || p + 34 > length) return null

        p += 2 + 32
        if (p >= length) return null
        val sessionLength = data[p].toInt() and 0xff
        p += 1 + sessionLength
        if (p + 2 > length) return null

        val cipherLength = u16(data, p)
        p += 2 + cipherLength
        if (p >= length) return null
        val compressionLength = data[p].toInt() and 0xff
        p += 1 + compressionLength
        if (p + 2 > length) return null

        val extensionsLength = u16(data, p)
        p += 2
        val extensionsEnd = p + extensionsLength
        if (extensionsEnd > length) return null

        while (p + 4 <= extensionsEnd) {
            val type = u16(data, p)
            val size = u16(data, p + 2)
            p += 4
            if (p + size > extensionsEnd) return null

            if (type == 0 && size >= 5) {
                var q = p
                val listLength = u16(data, q)
                q += 2
                val listEnd = q + listLength
                if (listEnd > p + size) return null

                while (q + 3 <= listEnd) {
                    val nameType = data[q].toInt() and 0xff
                    val nameLength = u16(data, q + 1)
                    q += 3
                    if (q + nameLength > listEnd) return null
                    if (nameType == 0) {
                        return data.copyOfRange(q, q + nameLength)
                            .toString(Charset.forName("US-ASCII"))
                            .trimEnd('.')
                    }
                    q += nameLength
                }
            }
            p += size
        }
        return null
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)

    private fun u24(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 16) or
            ((data[offset + 1].toInt() and 0xff) shl 8) or
            (data[offset + 2].toInt() and 0xff)

    private companion object {
        const val MTU = 1500
        const val TUN_IPV4 = "10.66.0.2"
        const val TUN_IPV6 = "fd00:5a48:2::2"
        const val DNS_IPV4 = "1.1.1.1"
        const val DNS_IPV6 = "2606:4700:4700::1111"
        const val DNS_PORT = 53
        const val TCP_CONNECT_TIMEOUT_MS = 10_000
        const val TCP_JOIN_TIMEOUT_MS = 1_000
        const val UDP_TIMEOUT_MS = 2_000
        const val UDP_BUFFER = 64 * 1024
        const val RELAY_BUFFER = 16 * 1024
        const val FIRST_PACKET_LIMIT = 16 * 1024
    }
}
