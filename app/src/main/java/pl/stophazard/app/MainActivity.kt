package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestVpnPermission()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, 7001)
        else startService(Intent(this, HazardVpnService::class.java))
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==7001 && resultCode==RESULT_OK)
            startService(Intent(this,HazardVpnService::class.java))
    }
}
