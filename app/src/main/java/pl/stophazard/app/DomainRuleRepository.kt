package pl.stophazard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DomainRuleRepository(context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences("stop_hazard_rules",Context.MODE_PRIVATE)
    private val key="rules"

    fun getAll():List<DomainRule>{
        val raw=prefs.getString(key,null)?:return emptyList()
        return try{
            val a=JSONArray(raw)
            buildList{
                for(i in 0 until a.length()){
                    val o=a.getJSONObject(i)
                    add(DomainRule(
                        host=o.getString("host"),
                        type=runCatching{DomainRuleType.valueOf(o.getString("type"))}.getOrDefault(DomainRuleType.CUSTOM),
                        enabled=o.optBoolean("enabled",true),
                        note=o.optString("note","")
                    ))
                }
            }
        }catch(_:Exception){emptyList()}
    }

    @Synchronized fun upsert(rule:DomainRule){
        val list=getAll().filterNot{it.host.equals(rule.host,true)}.toMutableList()
        list.add(rule.copy(host=normalize(rule.host)))
        write(list)
    }

    @Synchronized fun remove(host:String)=write(getAll().filterNot{it.host.equals(normalize(host),true)})

    fun isBlocked(host:String):Boolean{
        val normalized=normalize(host)
        return getAll().any{it.enabled && matches(normalized,it.host)}
    }

    private fun matches(host:String,rule:String)=host==rule || host.endsWith("."+rule)

    private fun write(list:List<DomainRule>){
        val a=JSONArray()
        list.take(1000).forEach{r->a.put(JSONObject().apply{
            put("host",normalize(r.host));put("type",r.type.name);put("enabled",r.enabled);put("note",r.note)
        })}
        prefs.edit().putString(key,a.toString()).apply()
    }

    private fun normalize(host:String):String=host.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
}
