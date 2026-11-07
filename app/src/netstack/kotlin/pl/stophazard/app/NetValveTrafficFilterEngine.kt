package pl.stophazard.app

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
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
        const val TAG = "StopHazardNetValve"
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
            Log.i(TAG, "Starting NetValve VPN engine")
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
            Log.i(TAG, "Android TUN established; fd=${tun.fd}, mtu=$MTU")
            val handler = TrafficHandler(vpnService, running)
            tunnel = Bridge.newTunnel(tun.fd.toLong(), MTU.toLong(), "RELAY", handler)
            Log.i(TAG, "NetValve bridge created successfully")
            true
        } catch (error: Throwable) {
            Log.e(TAG, "NetValve startup failed", error)
            stop()
            false
        }
    }

    override fun stop() {
        running.set(false)
        runCatching { tunnel?.stop() }
            .onFailure { Log.e(TAG, "NetValve tunnel stop failed", it) }
        tunnel = null
        runCatching { tunInterface?.close() }
            .onFailure { Log.e(TAG, "TUN interface close failed", it) }
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
            Log.d(TAG, "TCP flow: $dstIP:$dstPort")
            Thread { relayTcp(dstIP, dstPort.toInt(), conn) }.start()
        }

        override fun handleUDP(
            srcIP: String,
            srcPort: Long,
            dstIP: String,
            dstPort: Long,
            conn: UDPConn,
        ) {
            Log.d(TAG, "UDP flow: $dstIP:$dstPort")
            Thread { relayUdp(dstIP, dstPort.toInt(), conn) }.start()
        }

        override fun log(level: Long, msg: String) {
            when (level.toInt()) {
                0, 1 -> Log.e(TAG, "NetValve: $msg")
                2 -> Log.w(TAG, "NetValve: $msg")
                else -> Log.d(TAG, "NetValve: $msg")
            }
        }

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
                    // Never pass a TLS flow upstream if its ClientHello is
                    // malformed or still incomplete at the inspection limit.
                    if (result == TlsClientHelloParser.Result.Invalid ||
                        result == TlsClientHelloParser.Result.NeedMore
                    ) return
                    host = (result as? TlsClientHelloParser.Result.Host)?.value
                } else if (HttpRequestInspector.looksLikeRequest(first, firstLength)) {
                    // HTTP headers can arrive in multiple TCP reads. Do not forward
                    // an incomplete request before the Host header has been inspected.
                    while (!HttpRequestInspector.hasHeaderTerminator(first, firstLength)) {
                        if (firstLength >= HTTP_HEADER_MAX_BYTES) return
                        val chunk = ByteArray(minOf(RELAY_BUFFER, HTTP_HEADER_MAX_BYTES - firstLength))
                        val count = readTcpWithTimeout(appSide, chunk, INITIAL_INSPECTION_TIMEOUT_MS)
                        if (count <= 0) return
                        val copied = minOf(count, HTTP_HEADER_MAX_BYTES - firstLength)
                        chunk.copyInto(first, firstLength, 0, copied)
                        firstLength += copied
                    }
                    host = HttpRequestInspector.parseHost(first, firstLength)
                }
                if (host != null && BlockedDomains.isBlocked(host)) return

                upstream = Socket()
                if (!vpnService.protect(upstream)) {
                    Log.e(TAG, "VpnService.protect returned false for TCP $destinationHost:$destinationPort")
                    return
                }
                upstream.tcpNoDelay = true
                upstream.connect(
                    InetSocketAddress(destinationHost, destinationPort),
                    TCP_CONNECT_TIMEOUT_MS,
                )
                Log.d(TAG, "TCP upstream connected: $destinationHost:$destinationPort")

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
                    } catch (error: Throwable) {
                        Log.w(TAG, "TCP downstream relay ended for $destinationHost:$destinationPort", error)
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
            } catch (error: Throwable) {
                Log.e(TAG, "TCP relay failed for $destinationHost:$destinationPort", error)
            } finally {
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
            }
        }

        /**
         * The pinned gomobile TCPConn binding exposes read(byte[]) but does not
         * expose readWithTimeout(byte[], long). Calling a guessed timeout method
         * made every TCP flow fail before it could be relayed. Use the actual
         * supported read method; this runs on the per-connection relay thread.
         *
         * The timeout parameter is retained at the call sites for readability,
         * but is not enforceable through this NetValve binding.
         */
        private fun readTcpWithTimeout(conn: TCPConn, buffer: ByteArray, timeoutMs: Long): Int =
            try {
                conn.readWithTimeout(buffer, timeoutMs).toInt()
            } catch (error: Throwable) {
                Log.e(TAG, "TCP timed read failed", error)
                -1
            }

        /**
         * Isolates the gomobile binding quirk seen in the generated AAR.
         * The pinned NetValve API exposes TCPConn.read(byte[]) as a Long-returning
         * Java method; reflection here avoids Kotlin signature drift between
         * gomobile toolchain versions while preserving the same byte[] call.
         */
        private fun readTcp(conn: TCPConn, buffer: ByteArray): Int =
            try {
                conn.read(buffer).toInt()
            } catch (error: Throwable) {
                Log.e(TAG, "TCP read failed", error)
                -1
            }

        private fun relayUdp(destinationHost: String, destinationPort: Int, appSide: UDPConn) {
            var upstream: DatagramSocket? = null
            try {
                upstream = DatagramSocket()
                if (!vpnService.protect(upstream)) {
                    Log.e(TAG, "VpnService.protect returned false for UDP $destinationHost:$destinationPort")
                    return
                }
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
            } catch (error: Throwable) {
                Log.e(TAG, "UDP relay failed for $destinationHost:$destinationPort", error)
            } finally {
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
            }
        }

        private companion object {
            const val TAG = "StopHazardNetValve"
        }
    }
}
