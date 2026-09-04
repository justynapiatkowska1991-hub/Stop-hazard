package pl.stophazard.app

import android.content.Context
import org.json.JSONObject

data class ProtectionStats(val total:Int,val blocked:Int,val allowed:Int)

object ProtectionStatsStore {
    private const val PREF="stop_hazard_stats"
    private const val KEY="stats"
    fun read(context:Context):ProtectionStats {
        val o=try{JSONObject(context.getSharedPreferences(PREF,0).getString(KEY,"{}"))}catch(_:Exception){JSONObject()}
        return ProtectionStats(o.optInt("total",0),o.optInt("blocked",0),o.optInt("allowed",0))
    }
    fun record(context:Context,blocked:Boolean) {
        val s=read(context)
        val o=JSONObject().put("total",s.total+1)
            .put("blocked",s.blocked+if(blocked)1 else 0)
            .put("allowed",s.allowed+if(blocked)0 else 1)
        context.getSharedPreferences(PREF,0).edit().putString(KEY,o.toString()).apply()
    }
}
