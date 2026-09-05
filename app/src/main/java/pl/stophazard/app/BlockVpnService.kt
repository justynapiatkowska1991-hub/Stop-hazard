package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class BlockVpnService : VpnService() {
    @Volatile private var running = false
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopProtection() else if (intent?.action == ACTION_START) startProtection()
        return START_NOT_STICKY
    }

    private fun startProtection() {
        if (running) return
        createChannel()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NOTIFICATION_ID, notification())
        }

        vpnInterface = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
            .establish()

        if (vpnInterface == null) { stopSelf(); return }
        running = true
        thread(name = "StopHazardDns") { loop() }
    }

    private fun loop() {
        val descriptor = vpnInterface ?: return
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32767)
        try {
            while (running) {
                val length = input.read(buffer)
                if (length > 0) handlePacket(buffer, length, output)
            }
        } catch (_: Exception) {
        } finally {
            try { input.close() } catch (_: Exception) {}
            try { output.close() } catch (_: Exception) {}
        }
    }

    private fun handlePacket(p: ByteArray, length: Int, output: FileOutputStream) {
        if (length < 28) return
        val version = (p[0].toInt() ushr 4) and 15
        val ihl = (p[0].toInt() and 15) * 4
        if (version != 4 || ihl < 20 || length < ihl + 8 || (p[9].toInt() and 255) != 17) return
        if (readIp(p, 16) != "10.0.0.1") return
        val destinationPort = u16(p, ihl + 2)
        if (destinationPort != 53) return
        val sourceIp = readIp(p, 12)
        val sourcePort = u16(p, ihl)
        val udpLength = u16(p, ihl + 4)
        val dnsOffset = ihl + 8
        val dnsLength = minOf(udpLength - 8, length - dnsOffset)
        if (dnsLength < 12) return
        val dns = p.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        val host = parseQuestion(dns) ?: return

        if (BlockedDomains.isBlocked(host)) {
            writeResponse(output, p, ihl, sourcePort, blockedResponse(dns))
        } else {
            forward(output, p, ihl, sourcePort, dns)
        }
        sourceIp.length // keeps packet source explicitly parsed for clarity
    }

    private fun forward(output: FileOutputStream, original: ByteArray, ihl: Int, port: Int, dns: ByteArray) {
        try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 2500
                socket.send(DatagramPacket(dns, dns.size, InetSocketAddress("1.1.1.1", 53)))
                val buf = ByteArray(4096)
                val reply = DatagramPacket(buf, buf.size)
                socket.receive(reply)
                writeResponse(output, original, ihl, port, reply.data.copyOf(reply.length))
            }
        } catch (_: Exception) {
            writeResponse(output, original, ihl, port, servfailResponse(dns))
        }
    }

    private fun writeResponse(output: FileOutputStream, original: ByteArray, ihl: Int, clientPort: Int, dns: ByteArray) {
        val total = ihl + 8 + dns.size
        val out = ByteArray(total)
        System.arraycopy(original, 0, out, 0, ihl)
        out[2] = (total ushr 8).toByte(); out[3] = total.toByte()
        System.arraycopy(original, 16, out, 12, 4)
        System.arraycopy(original, 12, out, 16, 4)
        out[10] = 0; out[11] = 0
        val ipSum = checksum(out, 0, ihl)
        out[10] = (ipSum ushr 8).toByte(); out[11] = ipSum.toByte()
        val u = ihl
        out[u] = 0; out[u + 1] = 53
        out[u + 2] = (clientPort ushr 8).toByte(); out[u + 3] = clientPort.toByte()
        val ul = 8 + dns.size
        out[u + 4] = (ul ushr 8).toByte(); out[u + 5] = ul.toByte()
        out[u + 6] = 0; out[u + 7] = 0
        System.arraycopy(dns, 0, out, u + 8, dns.size)
        val us = udpChecksum(out, u, ul)
        out[u + 6] = (us ushr 8).toByte(); out[u + 7] = us.toByte()
        output.write(out); output.flush()
    }

    private fun parseQuestion(dns: ByteArray): String? {
        var pos = 12
        val labels = mutableListOf<String>()
        while (pos < dns.size) {
            val n = dns[pos].toInt() and 255
            pos++
            if (n == 0) break
            if (n > 63 || pos + n > dns.size) return null
            labels += String(dns, pos, n, Charsets.US_ASCII); pos += n
        }
        return labels.joinToString(".").takeIf { it.isNotBlank() }
    }

    private fun blockedResponse(q: ByteArray) = dnsErrorResponse(q, 3)
    private fun servfailResponse(q: ByteArray) = dnsErrorResponse(q, 2)

    private fun dnsErrorResponse(q: ByteArray, code: Int): ByteArray {
        val r = q.copyOf()
        val flags = u16(r, 2) or 0x8000 or 0x0080 or code
        r[2] = (flags ushr 8).toByte(); r[3] = flags.toByte()
        for (i in 6..11) r[i] = 0
        return r
    }

    private fun readIp(d: ByteArray, o: Int) = (0..3).joinToString(".") { (d[o + it].toInt() and 255).toString() }
    private fun u16(d: ByteArray, o: Int) = ((d[o].toInt() and 255) shl 8) or (d[o + 1].toInt() and 255)

    private fun checksum(d: ByteArray, o: Int, len: Int): Int {
        var sum = 0L; var i = o; val end = o + len
        while (i + 1 < end) { sum += u16(d, i); i += 2 }
        if (i < end) sum += (d[i].toInt() and 255) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 65535) + (sum ushr 16)
        return sum.inv().toInt() and 65535
    }

    private fun udpChecksum(d: ByteArray, o: Int, len: Int): Int {
        var sum = 0L
        sum += u16(d, 12) + u16(d, 14) + u16(d, 16) + u16(d, 18) + 17 + len
        var i = o; val end = o + len
        while (i + 1 < end) { sum += u16(d, i); i += 2 }
        if (i < end) sum += (d[i].toInt() and 255) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 65535) + (sum ushr 16)
        val result = sum.inv().toInt() and 65535
        return if (result == 0) 65535 else result
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "STOP HAZARD", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("STOP HAZARD")
            .setContentText("Ochrona przed stronami hazardowymi jest aktywna")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun stopProtection() {
        running = false
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    companion object {
        const val ACTION_START = "pl.stophazard.app.START"
        const val ACTION_STOP = "pl.stophazard.app.STOP"
        private const val CHANNEL_ID = "stop_hazard_protection"
        private const val NOTIFICATION_ID = 1001
    }
}