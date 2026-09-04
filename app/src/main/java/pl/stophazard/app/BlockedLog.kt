package pl.stophazard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BlockedLog {
    private const val PREF="stop_hazard_log"
    private const val KEY="events"
    fun record(context:Context,host:String) {
        val prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        val old=try{JSONArray(prefs.getString(KEY,"[]"))}catch(_:Exception){JSONArray()}
        val item=JSONObject().apply {
            put("host",host)
            put("blocked",true)
            put("timestamp",System.currentTimeMillis())
        }
        val next=JSONArray()
        next.put(item)
        for(i in 0 until minOf(old.length(),199)) next.put(old.getJSONObject(i))
        prefs.edit().putString(KEY,next.toString()).apply()
    }
    fun all(context:Context):JSONArray {
        val prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        return try{JSONArray(prefs.getString(KEY,"[]"))}catch(_:Exception){JSONArray()}
    }
    fun clear(context:Context){context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(KEY).apply()}
}
