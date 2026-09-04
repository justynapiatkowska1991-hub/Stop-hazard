package pl.stophazard.app

import android.content.Context

class ProtectionEventRecorder(context: Context) {
    private val store = ProtectionEventStore(context.applicationContext)

    fun record(host: String, blocked: Boolean) {
        val normalized = BlockedDomains.normalize(host)
        if (normalized.isBlank()) return
        store.add(ProtectionEvent(normalized, blocked))
    }

    fun recent(limit: Int = 50): List<ProtectionEvent> =
        store.read().take(limit.coerceIn(1, 250))

    fun clear() = store.clear()
}
