package pl.stophazard.app

import android.content.Context
import android.content.Intent
import android.net.VpnService

object ProtectionController {
    private const val PREF="stop_hazard_settings"
    private const val ENABLED="enabled"

    fun isEnabled(context:Context)=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        .getBoolean(ENABLED,true)

    fun setEnabled(context:Context,enabled:Boolean) {
        context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED,enabled).apply()
        if(enabled) start(context) else stop(context)
    }

    fun start(context:Context) {
        val permission=VpnService.prepare(context)
        if(permission!=null) {
            if(context is android.app.Activity)
                context.startActivityForResult(permission,7001)
            return
        }
        val intent=Intent(context,HazardVpnService::class.java)
        context.startService(intent)
    }

    fun stop(context:Context) {
        context.stopService(Intent(context,HazardVpnService::class.java))
    }
}
