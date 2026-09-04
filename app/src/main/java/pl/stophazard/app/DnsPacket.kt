package pl.stophazard.app

object DnsPacket {
    private fun u8(v:Byte)=v.toInt() and 255
    fun isDnsQuery(p:ByteArray):Boolean{
        if(p.size<28)return false
        val ihl=(u8(p[0]) and 15)*4
        if(p.size<ihl+8+12)return false
        if(u8(p[9])!=17)return false
        val dst=(u8(p[ihl+2]) shl 8) or u8(p[ihl+3])
        return dst==53
    }
    fun readQuestionHost(p:ByteArray):String?{
        if(!isDnsQuery(p))return null
        val ihl=(u8(p[0]) and 15)*4
        var i=ihl+8+12
        val labels=mutableListOf<String>()
        while(i<p.size){
            val n=u8(p[i])
            if(n==0)break
            if(n>63 || i+1+n>p.size)return null
            labels.add(String(p,i+1,n,Charsets.US_ASCII));i+=n+1
        }
        return labels.joinToString(".").lowercase().takeIf{it.isNotBlank()}
    }
}
