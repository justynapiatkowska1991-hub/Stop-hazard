package pl.stophazard.app

import android.content.Context

object ProtectionServiceState {
    private const val PREF="stop_hazard_runtime"
    private const val RUNNING="running"

    fun setRunning(context:Context,value:Boolean)=context
        .getSharedPreferences(PREF,0).edit().putBoolean(RUNNING,value).apply()

    fun isRunning(context:Context)=context
        .getSharedPreferences(PREF,0).getBoolean(RUNNING,false)
}
