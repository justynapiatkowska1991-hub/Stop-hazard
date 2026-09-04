package pl.stophazard.app

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Safe, transport-agnostic IPv4 UDP/DNS router.
 *
 * It only handles IPv4 UDP packets whose destination port is 53.
 * Unsupported protocols and malformed packets are ignored rather than
 * partially rewritten.
 */
class VpnPacketRouter(
    private val dnsProcessor: DnsQueryProcessor
) {
    data class RouteResult(
        val responsePacket: ByteArray? = null,
        val blocked: Boolean = false,
        val hostname: String? = null
    )

    fun route(packet: ByteArray, length: Int = packet.size): RouteResult {
        if (length < 20 || length > packet.size) return RouteResult()

        val version = (packet[0].toInt() ushr 4) and 0x0f
        val ihlWords = packet[0].toInt() and 0x0f
        val headerLength = ihlWords * 4
        if (version != 4 || ihlWords < 5 || headerLength > length) return RouteResult()

        val totalLength = u16(packet, 2)
        if (totalLength < headerLength || totalLength > length) return RouteResult()

        val protocol = packet[9].toInt() and 0xff
        if (protocol != 17) return RouteResult() // UDP only

        val udpOffset = headerLength
        if (udpOffset + 8 > totalLength) return RouteResult()

        val destinationPort = u16(packet, udpOffset + 2)
        if (destinationPort != 53) return RouteResult()

        val udpLength = u16(packet, udpOffset + 4)
        if (udpLength < 8 || udpOffset + udpLength > totalLength) return RouteResult()

        val dnsOffset = udpOffset + 8
        val dnsLength = udpLength - 8
        val dnsRequest = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)

        val processed = dnsProcessor.process(dnsRequest)
        val response = processed.response ?: return RouteResult(
            blocked = processed.blocked,
            hostname = processed.hostname
        )

        return RouteResult(
            responsePacket = buildUdpIpv4Response(packet, totalLength, response),
            blocked = processed.blocked,
            hostname = processed.hostname
        )
    }

    private fun buildUdpIpv4Response(
        request: ByteArray,
        totalLength: Int,
        dnsResponse: ByteArray
    ): ByteArray {
        val ipHeaderLength = (request[0].toInt() and 0x0f) * 4
        val udpOffset = ipHeaderLength
        val resultLength = ipHeaderLength + 8 + dnsResponse.size
        val result = ByteArray(resultLength)

        // Copy IPv4 header, then swap source/destination addresses.
        request.copyInto(result, 0, 0, ipHeaderLength)
        for (i in 0 until 4) {
            result[12 + i] = request[16 + i]
            result[16 + i] = request[12 + i]
        }

        result[8] = 64
        result[9] = 17

        writeU16(result, 2, resultLength)
        result[10] = 0
        result[11] = 0
        writeU16(result, 10, ipv4Checksum(result, 0, ipHeaderLength))

        // Swap UDP ports.
        result[udpOffset] = request[udpOffset + 2]
        result[udpOffset + 1] = request[udpOffset + 3]
        result[udpOffset + 2] = request[udpOffset]
        result[udpOffset + 3] = request[udpOffset + 1]

        val udpLength = 8 + dnsResponse.size
        writeU16(result, udpOffset + 4, udpLength)
        result[udpOffset + 6] = 0
        result[udpOffset + 7] = 0
        dnsResponse.copyInto(result, udpOffset + 8)

        writeU16(
            result,
            udpOffset + 6,
            udpChecksum(
                result,
                udpOffset,
                udpLength
            )
        )

        return result
    }

    private fun ipv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length

        while (i + 1 < end) {
            sum += u16(data, i)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xff) shl 8

        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xffff) + (sum ushr 16)
        }
        return sum.inv().toInt() and 0xffff
    }

    private fun udpChecksum(data: ByteArray, udpOffset: Int, udpLength: Int): Int {
        var sum = 0L

        // IPv4 pseudo-header.
        for (i in 12 until 20 step 2) sum += u16(data, i)
        sum += 17
        sum += udpLength

        var i = udpOffset
        val end = udpOffset + udpLength
        while (i + 1 < end) {
            sum += u16(data, i)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xff) shl 8

        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xffff) + (sum ushr 16)
        }

        val checksum = sum.inv().toInt() and 0xffff
        return if (checksum == 0) 0xffff else checksum
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 8) or
            (data[offset + 1].toInt() and 0xff)

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte()
        data[offset + 1] = value.toByte()
    }
}
