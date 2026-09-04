package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class HazardVpnService:VpnService(){
    private var vpn:ParcelFileDescriptor?=null
    @Volatile private var running=false

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(!running) startVpn()
        return START_STICKY
    }

    private fun startVpn(){
        vpn=try{
            Builder()
                .setSession("STOP HAZARD")
                .setMtu(1500)
                .addAddress("10.8.0.2",32)
                .addRoute("0.0.0.0",0)
                .addDnsServer("10.8.0.1")
                .establish()
        }catch(_:Exception){null}

        if(vpn==null)return
        running=true
        ProtectionServiceState.setRunning(this,true)

        Thread{
            val input=FileInputStream(vpn!!.fileDescriptor)
            val output=FileOutputStream(vpn!!.fileDescriptor)
            val buffer=ByteArray(32767)
            val engine=ProtectionEngine(this)

            try{
                while(running){
                    val n=input.read(buffer)
                    if(n<=0)break
                    val packet=buffer.copyOf(n)
                    val host=DnsPacket.readQuestionHost(packet)
                    if(host!=null && engine.inspect(host)){
                        val response=DnsResponse.nxdomain(packet)
                        if(response!=null)output.write(response)
                        continue
                    }
                    // No fake forwarding: non-DNS transport is left for the
                    // dedicated upstream implementation.
                }
            }catch(_:Exception){}finally{
                ProtectionServiceState.setRunning(this,false)
            }
        }.start()
    }

    override fun onDestroy(){
        running=false
        try{vpn?.close()}catch(_:Exception){}
        vpn=null
        ProtectionServiceState.setRunning(this,false)
        super.onDestroy()
    }
}
