package pl.stophazard.app

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DnsPacket {
    fun isDnsQuery(p:ByteArray):Boolean {
        if(p.size<28) return false
        val ihl=(p[0].toInt() and 15)*4
        if(p.size<ihl+12) return false
        val protocol=p[9].toInt() and 255
        if(protocol!=17) return false
        val dst=((p[16].toInt() and 255) shl 8) or (p[17].toInt() and 255)
        return dst==53
    }

    fun readQuestionHost(p:ByteArray):String? {
        if(!isDnsQuery(p)) return null
        val ihl=(p[0].toInt() and 15)*4
        var i=ihl+8+12
        if(i>=p.size) return null
        val labels=mutableListOf<String>()
        while(i<p.size) {
            val n=p[i].toInt() and 255
            if(n==0) break
            if((n and 0xC0)!=0 || n>63 || i+1+n>p.size) return null
            labels.add(String(p,i+1,n,Charsets.US_ASCII))
            i+=n+1
        }
        return labels.joinToString(".").lowercase().takeIf{it.isNotBlank()}
    }

    fun blockedResponse(query:ByteArray):ByteArray {
        val ihl=(query[0].toInt() and 15)*4
        val dnsOffset=ihl+8
        val out=query.copyOf()
        // DNS header: response + authoritative answer, zero answers is
        // deliberately avoided; return NXDOMAIN to fail closed for blocked host.
        val flags=(out[dnsOffset+2].toInt() and 255 shl 8) or (out[dnsOffset+3].toInt() and 255)
        val newFlags=(flags or 0x8000 or 0x0003) and 0xFFFF
        out[dnsOffset+2]=(newFlags ushr 8).toByte()
        out[dnsOffset+3]=newFlags.toByte()
        out[dnsOffset+6]=0;out[dnsOffset+7]=0
        out[dnsOffset+8]=0;out[dnsOffset+9]=0
        return out
    }
}
