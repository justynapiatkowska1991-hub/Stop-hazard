package pl.stophazard.app

class ProtectionEngine(
    private val settingsStore: ProtectionSettingsStore,
    private val rules: DomainRuleRepository,
    private val events: ProtectionEventRecorder
) {
    fun evaluate(host:String):Decision{
        val normalized=BlockedDomains.normalize(host)
        if(normalized.isBlank()) return Decision.Allow
        val settings=settingsStore.get()
        if(!settings.enabled) return Decision.Allow
        if(rules.isBlocked(normalized)){
            events.record(normalized,true)
            return Decision.Block("Domena znajduje się na liście blokad")
        }
        return Decision.Allow.also{events.record(normalized,false)}
    }

    sealed class Decision{
        data class Block(val reason:String):Decision()
        data object Allow:Decision()
    }
}
