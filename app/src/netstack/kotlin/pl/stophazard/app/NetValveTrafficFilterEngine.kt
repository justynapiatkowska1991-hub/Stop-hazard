package pl.stophazard.app

import android.net.VpnService
import android.os.ParcelFileDescriptor
import dev.netvalve.bridge.Bridge
import dev.netvalve.bridge.Handler
import dev.netvalve.bridge.TCPConn
import dev.netvalve.bridge.Tunnel
import dev.netvalve.bridge.UDPConn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NetValveTrafficFilterEngine(
    private val vpnService: VpnService,
) : TrafficFilterEngine {

    private companion object {
        const val MTU = 1500
        const val RELAY_BUFFER = 16 * 1024
        const val HTTP_HEADER_MAX_BYTES = 16 * 1024
        const val TCP_CONNECT_TIMEOUT_MS = 10_000
        const val INITIAL_INSPECTION_TIMEOUT_MS = 10_000L
        const val TCP_JOIN_TIMEOUT_MS = 2_000
    }

    private val running = AtomicBoolean(false)
    private var tunnel: Tunnel? = null
    private var tunInterface: ParcelFileDescriptor? = null

    override fun start(): Boolean {
        if (!running.compareAndSet(false, true)) return true

        return try {
            val tun = vpnService.Builder()
                .setSession("STOP HAZARD")
                .setMtu(MTU)
                .addAddress("10.66.0.2", 32)
                .addAddress("fd00:5a48:2::2", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("2606:4700:4700::1111")
                .establish() ?: throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")

            tunInterface = tun
            val handler = TrafficHandler(vpnService, running)
            tunnel = Bridge.newTunnel(tun.fd.toLong(), MTU.toLong(), "RELAY", handler)
            true
        } catch (_: Throwable) {
            stop()
            false
        }
    }

    override fun stop() {
        running.set(false)
        runCatching { tunnel?.stop() }
        tunnel = null
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    override fun isRunning(): Boolean = running.get() && tunnel != null

    private class TrafficHandler(
        private val vpnService: VpnService,
        private val running: AtomicBoolean,
    ) : Handler {
        override fun handleTCP(
            srcIP: String,
            srcPort: Long,
            dstIP: String,
            dstPort: Long,
            conn: TCPConn,
        ) {
            Thread { relayTcp(dstIP, dstPort.toInt(), conn) }.start()
        }

        override fun handleUDP(
            srcIP: String,
            srcPort: Long,
            dstIP: String,
            dstPort: Long,
            conn: UDPConn,
        ) {
            Thread { relayUdp(dstIP, dstPort.toInt(), conn) }.start()
        }

        override fun log(level: Long, msg: String) = Unit

        private fun relayTcp(destinationHost: String, destinationPort: Int, appSide: TCPConn) {
            var upstream: Socket? = null
            try {
                val first = ByteArray(64 * 1024)
                // Retain all initial bytes until the TLS ClientHello is complete.
                val firstChunk = ByteArray(RELAY_BUFFER)
                var firstLength = readTcpWithTimeout(appSide, firstChunk, INITIAL_INSPECTION_TIMEOUT_MS)
                if (firstLength <= 0) return
                firstChunk.copyInto(first, 0, 0, minOf(firstLength, first.size))
                firstLength = minOf(firstLength, first.size)

                var host: String? = null
                if ((first[0].toInt() and 0xff) == 22) {
                    var result = TlsClientHelloParser.parse(first, firstLength)
                    while (result == TlsClientHelloParser.Result.NeedMore && firstLength < first.size && running.get()) {
                        val chunk = ByteArray(minOf(RELAY_BUFFER, first.size - firstLength))
                        val count = readTcpWithTimeout(appSide, chunk, INITIAL_INSPECTION_TIMEOUT_MS)
                        if (count <= 0) return
                        val copied = minOf(count, first.size - firstLength)
                        chunk.copyInto(first, firstLength, 0, copied)
                        firstLength += copied
                        result = TlsClientHelloParser.parse(first, firstLength)
                    }
                    host = (result as? TlsClientHelloParser.Result.Host)?.value
                } else if (looksLikeHttpRequest(first, firstLength)) {
                    // HTTP headers can arrive in multiple TCP reads. Do not forward
                    // an incomplete request before the Host header has been inspected.
                    while (!hasHttpHeaderTerminator(first, firstLength)) {
                        if (firstLength >= HTTP_HEADER_MAX_BYTES) return
                        val chunk = ByteArray(minOf(RELAY_BUFFER, HTTP_HEADER_MAX_BYTES - firstLength))
                        val count = readTcpWithTimeout(appSide, chunk, INITIAL_INSPECTION_TIMEOUT_MS)
                        if (count <= 0) return
                        val copied = minOf(count, HTTP_HEADER_MAX_BYTES - firstLength)
                        chunk.copyInto(first, firstLength, 0, copied)
                        firstLength += copied
                    }
                    host = parseHttpHost(first, firstLength)
                }
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
                    }
                }
                downstream.start()

                val buffer = ByteArray(RELAY_BUFFER)
                while (running.get()) {
                    val count = readTcp(appSide, buffer)
                    if (count <= 0) break
                    socket.getOutputStream().write(buffer, 0, count)
                    socket.getOutputStream().flush()
                }
                runCatching { downstream.join(TCP_JOIN_TIMEOUT_MS.toLong()) }
            } catch (_: Throwable) {
            } finally {
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
            }
        }

        /** Reads initial protocol bytes with a hard deadline; regular relay reads remain blocking. */
        private fun readTcpWithTimeout(conn: TCPConn, buffer: ByteArray, timeoutMs: Long): Int {
            val method = conn.javaClass.methods.firstOrNull {
                it.name == "readWithTimeout" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == ByteArray::class.java &&
                    (it.parameterTypes[1] == java.lang.Long.TYPE ||
                        it.parameterTypes[1] == java.lang.Long::class.java)
            } ?: return -1

            return try {
                (method.invoke(conn, buffer, timeoutMs) as Number).toInt()
            } catch (_: Throwable) {
                -1
            }
        }

        /**
         * Isolates the gomobile binding quirk seen in the generated AAR.
         * The pinned NetValve API exposes TCPConn.read(byte[]) as a Long-returning
         * Java method; reflection here avoids Kotlin signature drift between
         * gomobile toolchain versions while preserving the same byte[] call.
         */
        private fun readTcp(conn: TCPConn, buffer: ByteArray): Int {
            val method = conn.javaClass.methods.firstOrNull {
                it.name == "read" &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == ByteArray::class.java
            } ?: return -1

            return try {
                (method.invoke(conn, buffer) as Number).toInt()
            } catch (_: Throwable) {
                -1
            }
        }

        private fun relayUdp(destinationHost: String, destinationPort: Int, appSide: UDPConn) {
            var upstream: DatagramSocket? = null
            try {
                upstream = DatagramSocket()
                if (!vpnService.protect(upstream)) return
                upstream.soTimeout = TCP_CONNECT_TIMEOUT_MS

                while (running.get()) {
                    val data = appSide.receive() ?: break
                    if (destinationPort == 53) {
                        val filtered = DnsResponseBuilder.responseFor(data)
                        if (filtered != null) {
                            appSide.send(filtered)
                            continue
                        }
                    }

                    upstream.send(
                        DatagramPacket(
                            data,
                            data.size,
                            InetSocketAddress(destinationHost, destinationPort),
                        ),
                    )

                    val responseBuffer = ByteArray(65_535)
                    val response = DatagramPacket(responseBuffer, responseBuffer.size)
                    upstream.receive(response)
                    appSide.send(responseBuffer.copyOf(response.length))
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
            }
        }

        private fun detectHost(data: ByteArray, length: Int): String? {
            parseTlsSni(data, length)?.let { return it }
            return parseHttpHost(data, length)
        }

        private fun looksLikeHttpRequest(data: ByteArray, length: Int): Boolean {
            if (length <= 0) return false
            val prefix = data.copyOf(minOf(length, 16)).toString(Charsets.ISO_8859_1)
            val method = prefix.takeWhile { !it.isWhitespace() }
            if (method.isEmpty() || method.length > 10) return false
            val knownMethods = listOf("GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS", "PATCH", "CONNECT", "TRACE")
            return knownMethods.any { it.startsWith(method) || method.startsWith(it) }
        }

        private fun hasHttpHeaderTerminator(data: ByteArray, length: Int): Boolean {
            if (length < 4) return false
            for (i in 0..length - 4) {
                if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() &&
                    data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()
                ) return true
            }
            return false
        }

        private fun parseHttpHost(data: ByteArray, length: Int): String? {
            val text = runCatching {
                data.copyOf(length).toString(Charsets.ISO_8859_1)
            }.getOrNull() ?: return null
            val match = Regex("(?im)^Host\\s*:\\s*([^\\s:]+)").find(text) ?: return null
            return match.groupValues[1].trimEnd('.').lowercase()
        }

        private fun parseTlsSni(data: ByteArray, length: Int): String? {
            if (length < 5 || data[0].toInt() != 22) return null
            val recordLength = ((data[3].toInt() and 0xff) shl 8) or (data[4].toInt() and 0xff)
            if (recordLength + 5 > length) return null

            var p = 5
            if (p + 4 > length) return null
            p += 4
            if (p >= length) return null

            val sessionIdLength = data[p].toInt() and 0xff
            p += 1 + sessionIdLength
            if (p + 2 > length) return null

            val cipherSuitesLength = ((data[p].toInt() and 0xff) shl 8) or (data[p + 1].toInt() and 0xff)
            p += 2 + cipherSuitesLength
            if (p >= length) return null

            val compressionMethodsLength = data[p].toInt() and 0xff
            p += 1 + compressionMethodsLength
            if (p + 2 > length) return null

            val extensionsLength = ((data[p].toInt() and 0xff) shl 8) or (data[p + 1].toInt() and 0xff)
            p += 2
            val end = minOf(p + extensionsLength, length)

            while (p + 4 <= end) {
                val type = ((data[p].toInt() and 0xff) shl 8) or (data[p + 1].toInt() and 0xff)
                val extLength = ((data[p + 2].toInt() and 0xff) shl 8) or (data[p + 3].toInt() and 0xff)
                p += 4
                if (p + extLength > end) return null

                if (type == 0 && extLength >= 5) {
                    var q = p + 2
                    if (q + 2 > p + extLength) return null
                    val nameType = data[q].toInt() and 0xff
                    q += 1
                    if (nameType != 0) return null
                    val nameLength = ((data[q].toInt() and 0xff) shl 8) or (data[q + 1].toInt() and 0xff)
                    q += 2
                    if (q + nameLength > p + extLength) return null
                    return data.copyOfRange(q, q + nameLength)
                        .toString(Charsets.UTF_8)
                        .trimEnd('.')
                        .lowercase()
                }
                p += extLength
            }
            return null
        }
    }
}
