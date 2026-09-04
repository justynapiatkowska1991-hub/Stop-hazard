package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle

class MainActivity:Activity(){
    private val requestCode=7001
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        if(ProtectionState.isEnabled(this)) requestVpn()
    }
    private fun requestVpn(){
        val permission=VpnService.prepare(this)
        if(permission!=null) startActivityForResult(permission,requestCode)
        else startProtection()
    }
    private fun startProtection(){
        startService(Intent(this,HazardVpnService::class.java))
    }
    override fun onActivityResult(req:Int,result:Int,data:Intent?){
        super.onActivityResult(req,result,data)
        if(req==requestCode && result==RESULT_OK) startProtection()
    }
}
