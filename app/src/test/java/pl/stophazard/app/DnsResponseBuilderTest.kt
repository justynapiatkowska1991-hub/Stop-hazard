package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsResponseBuilderTest {
    @Test
    fun buildsNxDomainWithMatchingTransactionIdAndQuestion() {
        val query = query("fortuna.pl")
        val response = DnsResponseBuilder.nxdomainResponse(query)

        assertNotNull(response)
        response!!
        assertEquals(query[0], response[0])
        assertEquals(query[1], response[1])
        val flags = ((response[2].toInt() and 0xff) shl 8) or (response[3].toInt() and 0xff)
        assertTrue(flags and 0x8000 != 0) // response
        assertEquals(3, flags and 0x000f) // NXDOMAIN
        assertEquals(1, response[5].toInt()) // one question
        assertEquals(query.size, response.size)
    }

    @Test
    fun malformedQueryDoesNotProduceResponse() {
        assertNull(DnsResponseBuilder.nxdomainResponse(byteArrayOf(1, 2, 3)))
    }

    private fun query(domain: String): ByteArray {
        val output = ArrayList<Byte>()
        fun add(value: Int) { output += value.toByte() }
        add(0x12); add(0x34)
        add(0x01); add(0x00)
        add(0x00); add(0x01)
        repeat(3) { add(0) }
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
