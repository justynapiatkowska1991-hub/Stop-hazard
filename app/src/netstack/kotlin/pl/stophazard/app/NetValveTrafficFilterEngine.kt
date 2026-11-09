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
                0 -> Log.d(TAG, "NetValve: $msg")
                1 -> Log.i(TAG, "NetValve: $msg")
                2 -> Log.w(TAG, "NetValve: $msg")
                else -> Log.e(TAG, "NetValve: $msg")
            }
        }

        private fun relayTcp(destinationHost: String, destinationPort: Int, appSide: TCPConn) {
            if (destinationPort == 53) {
                relayDnsTcp(destinationHost, appSide)
                return
            }

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
         * Filters classic DNS-over-TCP queries. The stream decoder handles
         * TCP fragmentation and coalescing; only complete DNS frames are
         * inspected. This path is experimental until exercised on a device.
         */
        private fun relayDnsTcp(destinationHost: String, appSide: TCPConn) {
            var upstream: Socket? = null
            val appSideWriteLock = Any()
            var downstream: Thread? = null
            try {
                val socket = Socket()
                upstream = socket
                if (!vpnService.protect(socket)) {
                    Log.e(TAG, "VpnService.protect returned false for DNS/TCP $destinationHost:53")
                    return
                }
                socket.tcpNoDelay = true
                socket.connect(
                    InetSocketAddress(destinationHost, 53),
                    TCP_CONNECT_TIMEOUT_MS,
                )
                Log.d(TAG, "DNS/TCP upstream connected: $destinationHost:53")

                downstream = Thread({
                    val buffer = ByteArray(RELAY_BUFFER)
                    try {
                        while (running.get()) {
                            val count = socket.getInputStream().read(buffer)
                            if (count <= 0) break
                            synchronized(appSideWriteLock) {
                                appSide.write(buffer.copyOf(count))
                            }
                        }
                    } catch (error: Throwable) {
                        if (running.get()) {
                            Log.w(TAG, "DNS/TCP downstream relay ended for $destinationHost:53", error)
                        }
                    }
                }, "StopHazard-DNS-TCP-downstream").apply {
                    isDaemon = true
                    start()
                }

                val decoder = DnsTcpStreamDecoder()
                val buffer = ByteArray(RELAY_BUFFER)
                while (running.get()) {
                    val count = readTcp(appSide, buffer)
                    if (count <= 0) break
                    val frames = decoder.feed(buffer.copyOf(count))
                    for (frame in frames) {
                        val decision = DnsTcpFrameFilter.inspect(frame)
                        if (decision.blocked) {
                            synchronized(appSideWriteLock) {
                                appSide.write(decision.frame)
                            }
                            Log.i(TAG, "Blocked DNS/TCP query")
                        } else {
                            socket.getOutputStream().write(decision.frame)
                            socket.getOutputStream().flush()
                        }
                    }
                }
            } catch (error: Throwable) {
                Log.e(TAG, "DNS/TCP relay failed for $destinationHost:53", error)
            } finally {
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
                runCatching { downstream?.join(TCP_JOIN_TIMEOUT_MS.toLong()) }
            }
        }

        /**
         * Uses the readWithTimeout(byte[], long) method added to the pinned
         * NetValve gomobile binding. A timeout prevents an incomplete TLS
         * ClientHello or HTTP header from holding this relay thread indefinitely.
         */
        private fun readTcpWithTimeout(conn: TCPConn, buffer: ByteArray, timeoutMs: Long): Int =
            try {
                conn.readWithTimeout(buffer, timeoutMs).toInt()
            } catch (error: Throwable) {
                Log.e(TAG, "TCP timed read failed", error)
                -1
            }

        /**
         * Reads application-side TCP data using the Long-returning method
         * exposed by the generated gomobile AAR.
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
            val relayActive = AtomicBoolean(true)
            val appSideWriteLock = Any()
            var responseThread: Thread? = null

            try {
                val socket = DatagramSocket()
                upstream = socket
                if (!vpnService.protect(socket)) {
                    Log.e(TAG, "VpnService.protect returned false for UDP $destinationHost:$destinationPort")
                    return
                }

                // Restrict replies to this flow's destination and let request and
                // response traffic progress independently. A UDP service is not
                // required to answer every datagram; waiting inline for each reply
                // used to stall the whole association after the first lost packet.
                socket.connect(InetSocketAddress(destinationHost, destinationPort))
                socket.soTimeout = 1_000
                Log.d(TAG, "UDP upstream connected: $destinationHost:$destinationPort")

                responseThread = Thread({
                    val responseBuffer = ByteArray(65_535)
                    while (running.get() && relayActive.get()) {
                        try {
                            val response = DatagramPacket(responseBuffer, responseBuffer.size)
                            socket.receive(response)
                            if (running.get() && relayActive.get()) {
                                synchronized(appSideWriteLock) {
                                    appSide.send(responseBuffer.copyOf(response.length))
                                }
                            }
                        } catch (_: java.net.SocketTimeoutException) {
                            // Periodically re-check shutdown state.
                        } catch (error: Throwable) {
                            if (running.get() && relayActive.get()) {
                                Log.w(TAG, "UDP response relay ended for $destinationHost:$destinationPort", error)
                            }
                            break
                        }
                    }
                }, "StopHazard-UDP-$destinationPort").apply {
                    isDaemon = true
                    start()
                }

                while (running.get() && relayActive.get()) {
                    val data = appSide.receive() ?: break
                    if (destinationPort == 53) {
                        val filtered = DnsResponseBuilder.responseFor(data)
                        if (filtered != null) {
                            synchronized(appSideWriteLock) {
                                appSide.send(filtered)
                            }
                            continue
                        }
                    }

                    socket.send(DatagramPacket(data, data.size))
                }
            } catch (error: Throwable) {
                if (running.get() && relayActive.get()) {
                    Log.e(TAG, "UDP relay failed for $destinationHost:$destinationPort", error)
                }
            } finally {
                relayActive.set(false)
                runCatching { upstream?.close() }
                runCatching { appSide.close() }
                runCatching { responseThread?.join(1_000) }
            }
        }

        private companion object {
            const val TAG = "StopHazardNetValve"
        }
    }
}
