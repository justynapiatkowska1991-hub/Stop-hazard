package pl.stophazard.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsTcpFrameDecoderTest {
    @Test
    fun decodesPrefixAndPayloadAcrossSeparateReads() {
        val input = frame(query("fortuna.pl"))
        val decoder = DnsTcpFrameDecoder()
        assertTrue(decoder.feed(input.copyOfRange(0, 1)).isEmpty())
        assertTrue(decoder.feed(input.copyOfRange(1, 7)).isEmpty())
        val result = decoder.feed(input.copyOfRange(7, input.size))
        assertEquals(1, result.size)
        assertArrayEquals(input, result.single())
    }

    @Test
    fun decodesSeveralFramesFromOneRead() {
        val first = frame(query("fortuna.pl"))
        val second = frame(query("google.com"))
        val result = DnsTcpFrameDecoder().feed(first + second)
        assertEquals(2, result.size)
        assertArrayEquals(first, result[0])
        assertArrayEquals(second, result[1])
    }

    @Test
    fun preservesPartialSecondFrameAfterCompletingFirst() {
        val first = frame(query("fortuna.pl"))
        val second = frame(query("google.com"))
        val decoder = DnsTcpFrameDecoder()
        val result = decoder.feed(first + second.copyOfRange(0, 4))
        assertEquals(1, result.size)
        assertArrayEquals(first, result.single())
        val remainder = decoder.feed(second.copyOfRange(4, second.size))
        assertEquals(1, remainder.size)
        assertArrayEquals(second, remainder.single())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDnsTcpLengthShorterThanDnsHeader() {
        DnsTcpFrameDecoder().feed(byteArrayOf(0, 11))
    }

    @Test
    fun resetDiscardsIncompleteFrame() {
        val input = frame(query("fortuna.pl"))
        val decoder = DnsTcpFrameDecoder()
        decoder.feed(input.copyOfRange(0, 5))
        decoder.reset()
        val decoded = decoder.feed(input)
        assertEquals(1, decoded.size)
        assertArrayEquals(input, decoded.single())
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
