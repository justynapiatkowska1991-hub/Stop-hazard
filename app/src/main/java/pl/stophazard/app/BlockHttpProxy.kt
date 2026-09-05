package pl.stophazard.app

import android.net.VpnService
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class BlockHttpProxy(private val vpn: VpnService) {
    @Volatile private var running = false
    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()

    fun start() {
        if (running) return
        running = true
        thread(name = "StopHazardProxy") {
            try {
                ServerSocket(PORT, 64, java.net.InetAddress.getByName("127.0.0.1")).use {
                    server = it
                    while (running) {
                        val client = it.accept()
                        pool.execute { handle(client) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
        pool.shutdownNow()
    }

    private fun handle(client: Socket) {
        client.use { c ->
            c.soTimeout = 8000
            val input = BufferedInputStream(c.getInputStream())
            val output = BufferedOutputStream(c.getOutputStream())
            val requestLine = readLine(input) ?: return
            val headers = StringBuilder()
            while (true) {
                val line = readLine(input) ?: return
                if (line.isEmpty()) break
                headers.append(line).append("\r\n")
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val target = parts[1]

            val hostPort = if (method.equals("CONNECT", true)) {
                target
            } else {
                val uri = java.net.URI(target)
                val host = uri.host ?: headerHost(headers.toString()) ?: return
                val port = if (uri.port > 0) uri.port else 80
                "${host}:${port}"
            }

            val hp = hostPort.substringBeforeLast(":")
            val port = hostPort.substringAfterLast(":").toIntOrNull()
                ?: if (method.equals("CONNECT", true)) 443 else 80

            if (BlockedDomains.isBlocked(hp)) {
                val body = "STOP HAZARD\nTa strona hazardowa jest zablokowana."
                val bytes = body.toByteArray()
                output.write(("HTTP/1.1 403 Forbidden\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n$body").toByteArray())
                output.flush()
                return
            }

            val upstream = Socket()
            vpn.protect(upstream)
            upstream.connect(InetSocketAddress(hp, port), 6000)
            upstream.soTimeout = 15000
            upstream.use { u ->
                if (method.equals("CONNECT", true)) {
                    output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                    output.flush()
                    relay(c, u)
                } else {
                    val first = "$requestLine\r\n$headers\r\n".toByteArray()
                    u.getOutputStream().write(first)
                    u.getOutputStream().flush()
                    relay(c, u)
                }
            }
        }
    }

    private fun relay(a: Socket, b: Socket) {
        val ab = thread { copy(a.getInputStream(), b.getOutputStream()) }
        val ba = thread { copy(b.getInputStream(), a.getOutputStream()) }
        ab.join(20000)
        ba.join(20000)
    }

    private fun copy(input: InputStream, output: OutputStream) {
        try {
            val buf = ByteArray(16384)
            while (running) {
                val n = input.read(buf)
                if (n <= 0) break
                output.write(buf, 0, n)
                output.flush()
            }
        } catch (_: Exception) {}
    }

    private fun readLine(input: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (b.size() < 8192) {
            val ch = input.read()
            if (ch < 0) return null
            if (ch == 10) {
                val bytes = b.toByteArray()
                if (bytes.isNotEmpty() && bytes.last() == 13.toByte())
                    return String(bytes, 0, bytes.size - 1, Charsets.ISO_8859_1)
                return String(bytes, Charsets.ISO_8859_1)
            }
            b.write(ch)
        }
        return null
    }

    private fun headerHost(headers: String): String? =
        headers.lineSequence().firstOrNull { it.startsWith("Host:", true) }
            ?.substringAfter(":")
            ?.trim()
            ?.substringBefore(":")
            ?.trim()

    companion object { const val PORT = 18080 }
}