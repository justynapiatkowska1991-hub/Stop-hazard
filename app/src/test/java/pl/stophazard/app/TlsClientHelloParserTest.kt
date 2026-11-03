package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TlsClientHelloParserTest {
    @Test
    fun returnsNeedMoreForTcpFragmentAndFindsSniAfterAppend() {
        val hello = clientHello("STS.PL")
        assertEquals(TlsClientHelloParser.Result.NeedMore, TlsClientHelloParser.parse(hello, 11))
        assertEquals(TlsClientHelloParser.Result.Host("sts.pl"), TlsClientHelloParser.parse(hello))
    }

    @Test
    fun reassemblesClientHelloSplitAcrossTlsRecords() {
        val hello = clientHello("fortuna.pl")
        val recordPayloadStart = 5
        val split = 20
        val handshake = hello.copyOfRange(recordPayloadStart, hello.size)
        val first = tlsRecord(handshake.copyOfRange(0, split))
        val second = tlsRecord(handshake.copyOfRange(split, handshake.size))
        val combined = first + second
        assertEquals(TlsClientHelloParser.Result.Host("fortuna.pl"), TlsClientHelloParser.parse(combined))
    }

    @Test
    fun rejectsMalformedRecordLength() {
        val malformed = byteArrayOf(22, 3, 3, 0x7f, 0xff.toByte())
        assertEquals(TlsClientHelloParser.Result.Invalid, TlsClientHelloParser.parse(malformed))
    }

    private fun clientHello(host: String): ByteArray {
        val name = host.toByteArray(Charsets.US_ASCII)
        val sniEntry = byteArrayOf(0) + u16(name.size) + name
        val sniList = u16(sniEntry.size) + sniEntry
        val extension = u16(0) + u16(sniList.size) + sniList
        val extensions = u16(extension.size) + extension
        val body = byteArrayOf(3, 3) + ByteArray(32) +
            byteArrayOf(0) + u16(2) + byteArrayOf(0x13, 0x01) +
            byteArrayOf(1, 0) + extensions
        val handshake = byteArrayOf(1, (body.size shr 16).toByte(), (body.size shr 8).toByte(), body.size.toByte()) + body
        return tlsRecord(handshake)
    }

    private fun tlsRecord(payload: ByteArray): ByteArray =
        byteArrayOf(22, 3, 3) + u16(payload.size) + payload

    private fun u16(value: Int): ByteArray =
        byteArrayOf((value shr 8).toByte(), value.toByte())
}
