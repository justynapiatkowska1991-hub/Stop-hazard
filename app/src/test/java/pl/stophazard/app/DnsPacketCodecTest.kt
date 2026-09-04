package pl.stophazard.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsPacketCodecTest {

    @Test
    fun parsesSimpleAQuery() {
        val packet = query(0x1234, "bet365.com", 1, 1)
        val parsed = DnsPacketCodec.parseQuery(packet)
        requireNotNull(parsed)
        assertEquals(0x1234, parsed.id)
        assertEquals("bet365.com", parsed.hostname)
    }

    @Test
    fun rejectsResponsePacket() {
        val packet = query(0x1111, "example.com", 1, 1)
        packet[2] = (packet[2].toInt() or 0x80).toByte()
        assertNull(DnsPacketCodec.parseQuery(packet))
    }

    @Test
    fun buildsNxdomainAndPreservesQuestion() {
        val packet = query(0x2222, "blocked.example", 1, 1)
        val parsed = requireNotNull(DnsPacketCodec.parseQuery(packet))
        val response = DnsPacketCodec.buildNxDomainResponse(parsed)

        assertEquals(0x22, response[0].toInt() and 0xff)
        assertEquals(0x22, response[1].toInt() and 0xff)
        assertEquals(0x83, response[2].toInt() and 0xff)
        assertEquals(0x03, response[3].toInt() and 0xff)
        assertArrayEquals(packet.copyOfRange(12, packet.size), response.copyOfRange(12, response.size))
    }

    private fun query(id: Int, host: String, type: Int, clazz: Int): ByteArray {
        val out = ArrayList<Byte>()
        fun u16(v: Int) {
            out += (v ushr 8).toByte()
            out += v.toByte()
        }
        u16(id)
        u16(0x0100)
        u16(1)
        u16(0)
        u16(0)
        u16(0)
        host.split(".").forEach {
            out += it.length.toByte()
            it.toByteArray(Charsets.US_ASCII).forEach(out::add)
        }
        out += 0
        u16(type)
        u16(clazz)
        return out.toByteArray()
    }
}
