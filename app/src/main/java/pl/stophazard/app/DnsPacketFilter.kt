package pl.stophazard.app

/**
 * Odczytuje nazwę domeny z pojedynczego DNS query i stosuje DnsFilterPolicy.
 *
 * Ta klasa nie otwiera socketów, nie tworzy VPN i nie przekazuje pakietów.
 * Jest bezpiecznym, testowalnym krokiem przed podłączeniem transportu DNS.
 */
object DnsPacketFilter {
    data class Query(val id: Int, val domain: String, val questionType: Int, val questionClass: Int)

    sealed interface Result {
        data class Allow(val query: Query) : Result
        data class Block(val query: Query) : Result
        data object NotDnsQuery : Result
    }

    fun inspect(packet: ByteArray): Result {
        val query = parseQuery(packet) ?: return Result.NotDnsQuery
        return when (DnsFilterPolicy.decide(query.domain)) {
            DnsFilterPolicy.Decision.BLOCK -> Result.Block(query)
            DnsFilterPolicy.Decision.ALLOW -> Result.Allow(query)
        }
    }

    fun parseQuery(packet: ByteArray): Query? {
        if (packet.size < 12) return null
        val flags = u16(packet, 2)
        val questionCount = u16(packet, 4)
        if ((flags and 0x8000) != 0 || questionCount != 1) return null

        var offset = 12
        val labels = ArrayList<String>()
        while (true) {
            if (offset >= packet.size) return null
            val length = packet[offset].toInt() and 0xff
            offset++
            if (length == 0) break
            if ((length and 0xc0) != 0 || length > 63) return null
            if (offset + length > packet.size) return null
            labels += packet.copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)
            offset += length
            if (labels.size > 127) return null
        }
        if (labels.isEmpty() || offset + 4 > packet.size) return null
        val domain = DnsFilterPolicy.normalize(labels.joinToString(".")) ?: return null
        return Query(
            id = u16(packet, 0),
            domain = domain,
            questionType = u16(packet, offset),
            questionClass = u16(packet, offset + 2)
        )
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}
