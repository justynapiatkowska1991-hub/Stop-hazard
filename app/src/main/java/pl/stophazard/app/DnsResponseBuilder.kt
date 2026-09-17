package pl.stophazard.app

/**
 * Buduje minimalną odpowiedź DNS NXDOMAIN wyłącznie dla domen zablokowanych.
 *
 * Klasa nie otwiera socketów, nie tworzy VPN i nie przekazuje ruchu.
 * Odpowiedź zawiera nagłówek oraz oryginalną sekcję pytania.
 */
object DnsResponseBuilder {
    /**
     * Zwraca NXDOMAIN tylko wtedy, gdy DnsFilterPolicy blokuje domenę.
     * Dla domeny dozwolonej lub niepoprawnego pakietu zwraca null.
     */
    fun responseFor(packet: ByteArray): ByteArray? {
        val query = DnsPacketFilter.parseQuery(packet) ?: return null
        if (DnsFilterPolicy.decide(query.domain) != DnsFilterPolicy.Decision.BLOCK) {
            return null
        }
        return nxdomainResponse(packet)
    }

    fun nxdomainResponse(queryPacket: ByteArray): ByteArray? {
        val query = DnsPacketFilter.parseQuery(queryPacket) ?: return null
        val questionEnd = questionEnd(queryPacket) ?: return null
        val response = ByteArray(12 + (questionEnd - 12))

        response[0] = queryPacket[0]
        response[1] = queryPacket[1]
        // QR=1, RD copied from query, RCODE=3 (NXDOMAIN).
        val requestFlags = ((queryPacket[2].toInt() and 0xff) shl 8) or
            (queryPacket[3].toInt() and 0xff)
        val responseFlags = 0x8000 or (requestFlags and 0x0100) or 0x0003
        response[2] = (responseFlags ushr 8).toByte()
        response[3] = responseFlags.toByte()
        response[4] = 0
        response[5] = 1 // one question
        // ANCOUNT, NSCOUNT, ARCOUNT remain zero.
        queryPacket.copyInto(response, destinationOffset = 12, startIndex = 12, endIndex = questionEnd)
        return response
    }

    private fun questionEnd(packet: ByteArray): Int? {
        if (packet.size < 12) return null
        var offset = 12
        var labels = 0
        while (true) {
            if (offset >= packet.size) return null
            val length = packet[offset].toInt() and 0xff
            offset++
            if (length == 0) break
            if ((length and 0xc0) != 0 || length > 63) return null
            if (offset + length > packet.size) return null
            offset += length
            labels++
            if (labels > 127) return null
        }
        if (offset + 4 > packet.size) return null
        return offset + 4
    }
}
