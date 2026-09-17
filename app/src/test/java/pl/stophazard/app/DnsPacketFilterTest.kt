package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsPacketFilterTest {
    @Test
    fun blockedDomainIsClassifiedAsBlock() {
        val result = DnsPacketFilter.inspect(query("fortuna.pl"))
        assertTrue(result is DnsPacketFilter.Result.Block)
        assertEquals("fortuna.pl", (result as DnsPacketFilter.Result.Block).query.domain)
    }

    @Test
    fun ordinaryDomainIsClassifiedAsAllow() {
        val result = DnsPacketFilter.inspect(query("google.com"))
        assertTrue(result is DnsPacketFilter.Result.Allow)
    }

    @Test
    fun malformedPacketIsRejected() {
        assertEquals(DnsPacketFilter.Result.NotDnsQuery, DnsPacketFilter.inspect(byteArrayOf(1, 2, 3)))
    }

    private fun query(domain: String): ByteArray {
        val output = ArrayList<Byte>()
        fun add(value: Int) { output += value.toByte() }
        add(0x12); add(0x34) // transaction ID
        add(0x01); add(0x00) // standard query, recursion desired
        add(0x00); add(0x01) // one question
        repeat(3) { add(0) } // answer, authority, additional counts
        domain.split('.').forEach { label ->
            add(label.length)
            label.toByteArray(Charsets.US_ASCII).forEach { output += it }
        }
        add(0)
        add(0); add(1) // A
        add(0); add(1) // IN
        return output.toByteArray()
    }
}
