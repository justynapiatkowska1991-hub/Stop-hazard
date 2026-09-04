package pl.stophazard.app

import java.net.InetAddress

/**
 * Bridges DNS packets between the TUN representation and upstream DNS.
 * The current implementation is intentionally conservative: it only accepts
 * IPv4 UDP DNS queries and reconstructs the IPv4/UDP response packet before
 * writing it back to TUN.
 */
object DnsTunBridge {
    private fun u8(v: Byte) = v.toInt() and 0xff

    fun isIpv4UdpDns(packet: ByteArray): Boolean =
        DnsPacket.isDnsQuery(packet)

    fun buildResponse(query: ByteArray, dnsPayload: ByteArray): ByteArray? {
        if (!isIpv4UdpDns(query) || dnsPayload.size < 12) return null

        val ihl = (u8(query[0]) and 0x0f) * 4
        if (query.size < ihl + 8) return null

        val udpOffset = ihl
        val sourceIp = query.copyOfRange(12, 16)
        val destinationIp = query.copyOfRange(16, 20)
        val sourcePort = query.copyOfRange(udpOffset, udpOffset + 2)
        val destinationPort = query.copyOfRange(udpOffset + 2, udpOffset + 4)

        val out = ByteArray(ihl + 8 + dnsPayload.size)
        query.copyInto(out, 0, 0, ihl)

        destinationIp.copyInto(out, 12)
        sourceIp.copyInto(out, 16)

        out[udpOffset] = destinationPort[0]
        out[udpOffset + 1] = destinationPort[1]
        out[udpOffset + 2] = sourcePort[0]
        out[udpOffset + 3] = sourcePort[1]

        val udpLength = 8 + dnsPayload.size
        out[udpOffset + 4] = (udpLength ushr 8).toByte()
        out[udpOffset + 5] = udpLength.toByte()
        out[udpOffset + 6] = 0
        out[udpOffset + 7] = 0

        dnsPayload.copyInto(out, udpOffset + 8)
        updateIpv4TotalLength(out, ihl, out.size)
        out[10] = 0
        out[11] = 0
        val ipChecksum = checksum(out, 0, ihl)
        out[10] = (ipChecksum ushr 8).toByte()
        out[11] = ipChecksum.toByte()
        val udpChecksum = udpChecksum(out, ihl, out.size)
        out[udpOffset + 6] = (udpChecksum ushr 8).toByte()
        out[udpOffset + 7] = udpChecksum.toByte()
        return out
    }

    private fun updateIpv4TotalLength(p: ByteArray, ihl: Int, length: Int) {
        p[2] = (length ushr 8).toByte()
        p[3] = length.toByte()
    }

    private fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += (u8(data[i]) shl 8) or u8(data[i + 1])
            i += 2
        }
        if (i < end) sum += u8(data[i]) shl 8
        while ((sum ushr 16) != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun udpChecksum(p: ByteArray, udpOffset: Int, end: Int): Int {
        val pseudo = ByteArray(12 + end - udpOffset)
        p.copyInto(pseudo, 0, 12, 20)
        pseudo[8] = 0
        pseudo[9] = 17
        val udpLen = end - udpOffset
        pseudo[10] = (udpLen ushr 8).toByte()
        pseudo[11] = udpLen.toByte()
        p.copyInto(pseudo, 12, udpOffset, end)
        pseudo[18] = 0
        pseudo[19] = 0
        var c = checksum(pseudo, 0, pseudo.size)
        if (c == 0) c = 0xffff
        return c
    }
}