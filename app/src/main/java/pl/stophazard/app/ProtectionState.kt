package pl.stophazard.app

import android.content.Context

object ProtectionState {
    private const val PREF="stop_hazard_settings"
    private const val ENABLED="enabled"
    fun isEnabled(c:Context)=c.getSharedPreferences(PREF,0).getBoolean(ENABLED,true)
    fun setEnabled(c:Context,v:Boolean)=c.getSharedPreferences(PREF,0).edit().putBoolean(ENABLED,v).apply()
}
