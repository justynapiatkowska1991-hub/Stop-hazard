package pl.stophazard.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsTcpMessageCodecTest {
    @Test
    fun parsesLengthPrefixedBlockedQuery() {
        val frame = frame(query("fortuna.pl"))
        val parsed = DnsTcpMessageCodec.parseQuery(frame)
        assertNotNull(parsed)
        assertEquals("fortuna.pl", parsed!!.domain)
    }

    @Test
    fun createsLengthPrefixedNxdomainForBlockedQuery() {
        val query = query("fortuna.pl")
        val response = DnsTcpMessageCodec.blockedResponse(frame(query))
        assertNotNull(response)
        response!!
        val length = ((response[0].toInt() and 0xff) shl 8) or (response[1].toInt() and 0xff)
        assertEquals(response.size - 2, length)
        val dns = response.copyOfRange(2, response.size)
        assertEquals(3, dns[3].toInt() and 0x0f)
        assertArrayEquals(query.copyOfRange(12, query.size), dns.copyOfRange(12, dns.size))
    }

    @Test
    fun allowedDomainDoesNotProduceBlockedResponse() {
        assertNull(DnsTcpMessageCodec.blockedResponse(frame(query("google.com"))))
    }

    @Test
    fun rejectsIncompleteAndMismatchedFrames() {
        val valid = frame(query("fortuna.pl"))
        assertNull(DnsTcpMessageCodec.parseQuery(valid.copyOf(valid.size - 1)))
        val badLength = valid.copyOf().also { it[1] = (it[1] - 1).toByte() }
        assertNull(DnsTcpMessageCodec.parseQuery(badLength))
    }

    @Test
    fun rejectsNonDnsPayload() {
        assertNull(DnsTcpMessageCodec.parseQuery(byteArrayOf(0, 3, 1, 2, 3)))
    }

    private fun frame(message: ByteArray): ByteArray =
        byteArrayOf((message.size ushr 8).toByte(), message.size.toByte()) + message

    private fun query(domain: String): ByteArray {
        val output = ArrayList<Byte>()
        fun add(value: Int) { output += value.toByte() }
        add(0x12); add(0x34)
        add(0x01); add(0x00)
        add(0x00); add(0x01)
        repeat(6) { add(0) }
        domain.split('.').forEach { label ->
            add(label.length)
            label.toByteArray(Charsets.US_ASCII).forEach { output += it }
        }
        add(0)
        add(0); add(1)
        add(0); add(1)
        return output.toByteArray()
    }
}
