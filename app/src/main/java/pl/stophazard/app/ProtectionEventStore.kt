package pl.stophazard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProtectionEventStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("stop_hazard_events", Context.MODE_PRIVATE)

    @Synchronized
    fun add(event: ProtectionEvent) {
        val list = read().toMutableList()
        list.add(0, event)
        val trimmed = list.take(MAX_EVENTS)
        val json = JSONArray()
        trimmed.forEach {
            json.put(JSONObject().apply {
                put("host", it.host)
                put("blocked", it.blocked)
                put("timestamp", it.timestampMillis)
            })
        }
        prefs.edit().putString(KEY_EVENTS, json.toString()).apply()
    }

    fun read(): List<ProtectionEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    add(
                        ProtectionEvent(
                            host = item.getString("host"),
                            blocked = item.getBoolean("blocked"),
                            timestampMillis = item.getLong("timestamp")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 250
    }
}
