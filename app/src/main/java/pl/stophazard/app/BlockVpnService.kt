package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class BlockVpnService : VpnService() {

    companion object {
        const val ACTION_START = "pl.stophazard.app.action.START"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var packetThread: Thread? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (running.get()) {
            return START_STICKY
        }

        startForegroundNotification()
        BlockedDomains.refreshFromOfficialRegistry()

        try {
            startVpn()
        } catch (e: Exception) {
            stopVpn()
        }

        return START_STICKY
    }

    private fun startVpn() {

        val builder = Builder()
            .setSession("STOP HAZARD")
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .setBlocking(false)
            // Kierujemy przez VPN tylko ruch DNS. Nie dodajemy trasy 0.0.0.0/0,
            // bo bez pełnego forwardera pakiety WWW zostałyby odcięte od Internetu.
            // Przechwytuj popularne serwery DNS, aby ich zapytania trafiały do naszego filtra.
            .addRoute("1.1.1.1", 32)
            .addRoute("1.0.0.1", 32)
            .addRoute("8.8.8.8", 32)
            .addRoute("8.8.4.4", 32)
            .addRoute("9.9.9.9", 32)
            .addRoute("149.112.112.112", 32)

        // Nie kierujemy całego Internetu do TUN.
        // Pełny routing bez forwardera pakietów odcina połączenie.
        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            throw IllegalStateException("Nie udało się utworzyć interfejsu VPN")
        }

        running.set(true)

        packetThread = thread(name = "StopHazard-TUN") {
            readPackets()
        }
    }

    private fun readPackets() {
        val input = FileInputStream(vpnInterface!!.fileDescriptor)
        val output = FileOutputStream(vpnInterface!!.fileDescriptor)
        val packet = ByteArray(32767)
        while (running.get()) {
            try {
                val length = input.read(packet)
                if (length > 0) handlePacket(packet, length, output)
            } catch (_: Exception) {
                if (!running.get()) break
            }
        }
    }

    private fun handlePacket(packet: ByteArray, length: Int, output: FileOutputStream) {
        if (length < 28) return
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if ((packet[0].toInt() ushr 4) != 4 || packet[9].toInt() and 0xFF != 17 || length < ihl + 8) return
        val dstPort = u16(packet, ihl + 2)
        if (dstPort != 53) return
        val dns = packet.copyOfRange(ihl + 8, length)
        val domain = readDnsName(dns) ?: return
        val blocked = BlockedDomains.isBlocked(domain)
        val response = if (blocked) blockedResponse(dns) else forwardDns(dns)
        if (response != null) writeResponse(packet, ihl, response, output)
    }

    private fun readDnsName(dns: ByteArray): String? {
        if (dns.size < 13) return null
        var p = 12
        val labels = mutableListOf<String>()
        while (p < dns.size) {
            val n = dns[p].toInt() and 0xFF
            p++
            if (n == 0) break
            if ((n and 0xC0) != 0 || p + n > dns.size) return null
            labels += String(dns, p, n, Charsets.US_ASCII).lowercase()
            p += n
        }
        return labels.joinToString(".")
    }

    private fun blockedResponse(query: ByteArray): ByteArray {
        val r = query.copyOf()
        r[2] = (r[2].toInt() or 0x80).toByte()
        r[3] = ((r[3].toInt() and 0xF0) or 0x03).toByte()
        r[6] = 0; r[7] = 0; r[8] = 0; r[9] = 0
        return r
    }

    private fun forwardDns(query: ByteArray): ByteArray? = try {
        DatagramSocket().use { socket ->
            protect(socket)
            socket.soTimeout = 2000
            socket.send(DatagramPacket(query, query.size, InetSocketAddress("1.1.1.1", 53)))
            val b = ByteArray(4096)
            val p = DatagramPacket(b, b.size)
            socket.receive(p)
            b.copyOf(p.length)
        }
    } catch (_: Exception) { null }

    private fun writeResponse(original: ByteArray, ihl: Int, dns: ByteArray, output: FileOutputStream) {
        val total = ihl + 8 + dns.size
        val out = ByteArray(total)
        System.arraycopy(original, 0, out, 0, ihl)
        for (i in 0..3) {
            out[12 + i] = original[16 + i]
            out[16 + i] = original[12 + i]
        }
        out[2] = (total ushr 8).toByte(); out[3] = total.toByte()
        out[10] = 0; out[11] = 0
        val sum = checksum(out, 0, ihl)
        out[10] = (sum ushr 8).toByte(); out[11] = sum.toByte()
        val srcPort = u16(original, ihl)
        val dstPort = u16(original, ihl + 2)
        out[ihl] = (dstPort ushr 8).toByte(); out[ihl + 1] = dstPort.toByte()
        out[ihl + 2] = (srcPort ushr 8).toByte(); out[ihl + 3] = srcPort.toByte()
        val udpLen = 8 + dns.size
        out[ihl + 4] = (udpLen ushr 8).toByte(); out[ihl + 5] = udpLen.toByte()
        out[ihl + 6] = 0; out[ihl + 7] = 0
        System.arraycopy(dns, 0, out, ihl + 8, dns.size)
        output.write(out)
    }

    private fun checksum(b: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var p = off
        while (p + 1 < off + len) { sum += u16(b, p); p += 2 }
        if (p < off + len) sum += (b[p].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv().toInt() and 0xFFFF
    }

    private fun u16(b: ByteArray, p: Int): Int =
        ((b[p].toInt() and 0xFF) shl 8) or (b[p + 1].toInt() and 0xFF)

    private fun stopVpn() {

        running.set(false)

        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }

        vpnInterface = null

        packetThread?.interrupt()
        packetThread = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {

        stopVpn()

        super.onDestroy()
    }

    private fun startForegroundNotification() {

        val channelId = "stop_hazard_vpn"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "STOP HAZARD",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification.Builder(this, channelId)
                    .setContentTitle("STOP HAZARD")
                    .setContentText("Ochrona jest aktywna")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true)
                    .build()

            } else {

                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("STOP HAZARD")
                    .setContentText("Ochrona jest aktywna")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true)
                    .build()
            }

        startForeground(1001, notification)
    }
}