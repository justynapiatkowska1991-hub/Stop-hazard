package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps the transport objects associated with admitted flows.
 * Centralizes ownership so sockets can be closed deterministically when a flow
 * expires or the VPN service stops.
 */
class FlowTransportRegistry(
    private val transport: UpstreamTransport
) {
    private val active = ConcurrentHashMap<FlowTable.Key, Long>()

    fun register(key: FlowTable.Key): Boolean {
        active[key] = System.currentTimeMillis()
        return true
    }

    fun touch(key: FlowTable.Key) {
        if (active.containsKey(key)) {
            active[key] = System.currentTimeMillis()
        }
    }

    fun remove(key: FlowTable.Key) {
        active.remove(key)
        transport.close(key)
    }

    fun cleanup(idleTimeoutMs: Long = 120_000L) {
        val now = System.currentTimeMillis()
        active.entries.removeIf { (key, lastSeen) ->
            if (now - lastSeen > idleTimeoutMs) {
                transport.close(key)
                true
            } else {
                false
            }
        }
    }

    fun size(): Int = active.size

    fun clear() {
        active.keys.forEach { transport.close(it) }
        active.clear()
    }
}