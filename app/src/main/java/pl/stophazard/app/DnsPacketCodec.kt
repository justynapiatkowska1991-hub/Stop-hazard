package pl.stophazard.app

import java.io.ByteArrayOutputStream

/**
 * Minimal DNS wire-format codec used by STOP HAZARD.
 *
 * It deliberately supports the common UDP DNS query shape used by Android:
 * one question, QNAME, QTYPE and QCLASS. Malformed or unsupported packets
 * are rejected instead of being guessed.
 */
object DnsPacketCodec {

    data class Query(
        val id: Int,
        val flags: Int,
        val hostname: String,
        val questionEndOffset: Int,
        val raw: ByteArray
    )

    data class Question(
        val hostname: String,
        val type: Int,
        val clazz: Int,
        val endOffset: Int
    )

    fun parseQuery(packet: ByteArray, length: Int = packet.size): Query? {
        if (length < 12 || length > packet.size) return null

        val id = u16(packet, 0)
        val flags = u16(packet, 2)
        val qdCount = u16(packet, 4)
        if (qdCount != 1) return null

        // QR=1 means this is already a response, not a query.
        if ((flags and 0x8000) != 0) return null

        val question = parseQuestion(packet, 12, length) ?: return null
        return Query(
            id = id,
            flags = flags,
            hostname = question.hostname,
            questionEndOffset = question.endOffset,
            raw = packet.copyOf(length)
        )
    }

    fun parseQuestion(packet: ByteArray, start: Int, length: Int = packet.size): Question? {
        if (start < 12 || start >= length) return null

        val labels = ArrayList<String>()
        var offset = start
        var labelCount = 0

        while (true) {
            if (offset >= length) return null
            val size = packet[offset].toInt() and 0xff
            offset++

            if (size == 0) break
            // Compression pointers are not valid in the QNAME of a normal
            // Android-originated query and are intentionally rejected here.
            if ((size and 0xc0) != 0 || size > 63) return null
            if (offset + size > length) return null

            val label = packet.copyOfRange(offset, offset + size)
                .toString(Charsets.US_ASCII)
            if (label.isEmpty()) return null
            labels += label
            labelCount++
            if (labelCount > 127) return null
            offset += size
        }

        if (labels.isEmpty()) return null
        if (offset + 4 > length) return null

        val type = u16(packet, offset)
        val clazz = u16(packet, offset + 2)
        offset += 4

        return Question(
            hostname = labels.joinToString("."),
            type = type,
            clazz = clazz,
            endOffset = offset
        )
    }

    /**
     * Builds an NXDOMAIN response while preserving the original question.
     * The returned bytes are a DNS payload, not an IP/UDP packet.
     */
    fun buildNxDomainResponse(query: Query): ByteArray {
        val source = query.raw
        val questionEnd = query.questionEndOffset
        if (questionEnd < 16 || questionEnd > source.size) {
            throw IllegalArgumentException("Invalid DNS question offset")
        }

        val out = ByteArrayOutputStream(questionEnd + 16)

        writeU16(out, query.id)

        // QR=1, authoritative answer, recursion desired/available preserved,
        // RCODE=NXDOMAIN. Question count remains one.
        val responseFlags =
            0x8000 or
            0x0400 or
            (query.flags and 0x0100) or
            (query.flags and 0x0080) or
            0x0003

        writeU16(out, responseFlags)
        writeU16(out, 1) // QDCOUNT
        writeU16(out, 0) // ANCOUNT
        writeU16(out, 0) // NSCOUNT
        writeU16(out, 0) // ARCOUNT

        out.write(source, 12, questionEnd - 12)
        return out.toByteArray()
    }

    fun isDnsQueryPacket(packet: ByteArray, length: Int = packet.size): Boolean =
        parseQuery(packet, length) != null

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or
            (bytes[offset + 1].toInt() and 0xff)

    private fun writeU16(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 8) and 0xff)
        out.write(value and 0xff)
    }
}
