package pl.stophazard.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsTcpFrameFilterTest {
    @Test
    fun replacesBlockedQueryWithNxdomain() {
        val query = frame(query("fortuna.pl"))
        val decision = DnsTcpFrameFilter.inspect(query)

        assertTrue(decision.blocked)
        assertTrue(decision.frame !== query)
        val dns = decision.frame.copyOfRange(2, decision.frame.size)
        assertTrue(dns[2].toInt() and 0x80 != 0)
        assertTrue(dns[3].toInt() and 0x0f == 3)
    }

    @Test
    fun forwardsAllowedQueryUnchanged() {
        val query = frame(query("google.com"))
        val decision = DnsTcpFrameFilter.inspect(query)

        assertFalse(decision.blocked)
        assertArrayEquals(query, decision.frame)
    }

    @Test
    fun forwardsNonQueryPayloadUnchanged() {
        val response = frame(query("fortuna.pl").also { it[2] = 0x81.toByte() })
        val decision = DnsTcpFrameFilter.inspect(response)

        assertFalse(decision.blocked)
        assertArrayEquals(response, decision.frame)
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
