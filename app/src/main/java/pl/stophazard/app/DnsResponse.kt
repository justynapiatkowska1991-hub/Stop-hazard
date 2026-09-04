package pl.stophazard.app

object DnsResponse {
    private fun u8(v:Byte)=v.toInt() and 255

    fun nxdomain(query:ByteArray):ByteArray?{
        if(query.size<28 || !DnsPacket.isDnsQuery(query))return null
        val ihl=(u8(query[0]) and 15)*4
        val dns=ihl+8
        if(dns+12>query.size)return null

        val out=query.copyOf()
        // DNS flags: QR=1 and RCODE=3 (NXDOMAIN).
        val flags=((u8(out[dns+2]) shl 8) or u8(out[dns+3]))
        val newFlags=(flags or 0x8000 or 0x0003) and 0xFFFF
        out[dns+2]=(newFlags ushr 8).toByte()
        out[dns+3]=newFlags.toByte()

        // Keep the question but advertise no answer/authority/additional records.
        for(i in 6 until 12) { out[dns+i]=0 }
        return out
    }
}
