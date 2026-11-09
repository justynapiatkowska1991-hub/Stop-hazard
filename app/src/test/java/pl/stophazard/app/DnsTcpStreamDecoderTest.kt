package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsTcpStreamDecoderTest {
    @Test
    fun decodesPrefixSplitAcrossReads() {
        val frame = frame(query("fortuna.pl"))
        val decoder = DnsTcpStreamDecoder()

        assertTrue(decoder.feed(frame.copyOfRange(0, 1)).isEmpty())
        assertEquals(1, decoder.bufferedByteCount())
        val decoded = decoder.feed(frame.copyOfRange(1, frame.size))

        assertEquals(1, decoded.size)
        assertEquals(frame.toList(), decoded.single().toList())
        assertEquals(0, decoder.bufferedByteCount())
    }

    @Test
    fun decodesPayloadSplitAcrossReads() {
        val frame = frame(query("fortuna.pl"))
        val decoder = DnsTcpStreamDecoder()
        val split = 9

        assertTrue(decoder.feed(frame.copyOfRange(0, split)).isEmpty())
        val decoded = decoder.feed(frame.copyOfRange(split, frame.size))

        assertEquals(listOf(frame.toList()), decoded.map { it.toList() })
    }

    @Test
    fun decodesMultipleMessagesFromOneReadAndRetainsTrailingPartialFrame() {
        val first = frame(query("fortuna.pl"))
        val second = frame(query("google.com"))
        val decoder = DnsTcpStreamDecoder()
        val combined = first + second.copyOfRange(0, 5)

        val decodedFirst = decoder.feed(combined)
        assertEquals(listOf(first.toList()), decodedFirst.map { it.toList() })
        assertEquals(5, decoder.bufferedByteCount())

        val decodedSecond = decoder.feed(second.copyOfRange(5, second.size))
        assertEquals(listOf(second.toList()), decodedSecond.map { it.toList() })
        assertEquals(0, decoder.bufferedByteCount())
    }

    @Test
    fun emptyChunkDoesNotChangeBufferedState() {
        val frame = frame(query("fortuna.pl"))
        val decoder = DnsTcpStreamDecoder()
        decoder.feed(frame.copyOfRange(0, 3))
        assertTrue(decoder.feed(byteArrayOf()).isEmpty())
        assertEquals(3, decoder.bufferedByteCount())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidShortDnsLengthResetsAndFailsClosed() {
        val decoder = DnsTcpStreamDecoder()
        decoder.feed(byteArrayOf(0, 11))
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
