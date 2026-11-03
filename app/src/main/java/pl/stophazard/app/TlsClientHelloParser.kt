package pl.stophazard.app

/** Incremental TLS ClientHello parser; supports TCP splits and multiple TLS records. */
internal object TlsClientHelloParser {
    sealed class Result {
        object NotTls : Result()
        object NeedMore : Result()
        object NoSni : Result()
        object Invalid : Result()
        data class Host(val value: String) : Result()
    }

    fun parse(data: ByteArray, length: Int = data.size): Result {
        if (length < 1 || length > data.size) return Result.Invalid
        if (u8(data, 0) != 22) return Result.NotTls
        val handshake = ArrayList<Byte>(minOf(length, 16 * 1024))
        var offset = 0
        while (offset < length) {
            if (length - offset < 5) return Result.NeedMore
            if (u8(data, offset) != 22 || u8(data, offset + 1) != 3) return Result.Invalid
            val recordLength = u16(data, offset + 3)
            if (recordLength > 18_432) return Result.Invalid
            if (length - offset - 5 < recordLength) return Result.NeedMore
            for (i in 0 until recordLength) handshake.add(data[offset + 5 + i])
            offset += 5 + recordLength
            if (handshake.size >= 4) {
                val hs = handshake.toByteArray()
                if (u8(hs, 0) != 1) return Result.Invalid
                val messageLength = (u8(hs, 1) shl 16) or (u8(hs, 2) shl 8) or u8(hs, 3)
                if (messageLength > 65_535) return Result.Invalid
                if (hs.size >= messageLength + 4) return parseClientHello(hs, messageLength + 4)
            }
        }
        return Result.NeedMore
    }

    private fun parseClientHello(data: ByteArray, total: Int): Result {
        var p = 4
        fun has(n: Int) = n >= 0 && p <= total - n
        if (!has(35)) return Result.Invalid
        p += 34
        val sessionLength = u8(data, p++)
        if (!has(sessionLength + 2)) return Result.Invalid
        p += sessionLength
        val cipherLength = u16(data, p)
        p += 2
        if (cipherLength < 2 || cipherLength % 2 != 0 || !has(cipherLength + 1)) return Result.Invalid
        p += cipherLength
        val compressionLength = u8(data, p++)
        if (!has(compressionLength)) return Result.Invalid
        p += compressionLength
        if (p == total) return Result.NoSni
        if (!has(2)) return Result.Invalid
        val extensionsLength = u16(data, p)
        p += 2
        if (!has(extensionsLength) || p + extensionsLength != total) return Result.Invalid
        val extensionsEnd = p + extensionsLength
        while (p < extensionsEnd) {
            if (!has(4)) return Result.Invalid
            val type = u16(data, p)
            val size = u16(data, p + 2)
            p += 4
            if (!has(size) || p + size > extensionsEnd) return Result.Invalid
            if (type == 0) {
                if (size < 5 || u16(data, p) != size - 2) return Result.Invalid
                var q = p + 2
                val end = p + size
                while (q < end) {
                    if (q + 3 > end) return Result.Invalid
                    val nameType = u8(data, q++)
                    val nameLength = u16(data, q)
                    q += 2
                    if (nameLength == 0 || q + nameLength > end) return Result.Invalid
                    if (nameType == 0) {
                        val host = data.copyOfRange(q, q + nameLength)
                            .toString(Charsets.US_ASCII).trimEnd('.').lowercase()
                        if (host.isBlank() || host.any { it <= ' ' || it == '/' || it == ':' }) return Result.Invalid
                        return Result.Host(host)
                    }
                    q += nameLength
                }
                return Result.NoSni
            }
            p += size
        }
        return Result.NoSni
    }

    private fun u8(data: ByteArray, i: Int) = data[i].toInt() and 0xff
    private fun u16(data: ByteArray, i: Int) = (u8(data, i) shl 8) or u8(data, i + 1)
}
