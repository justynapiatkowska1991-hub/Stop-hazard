package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks logical IPv4 TCP/UDP flows seen by the VPN.
 *
 * This is state management only. It does not claim to implement NAT,
 * TCP reassembly, or Internet forwarding.
 */
class FlowTable(
    private val maxFlows: Int = 4096,
    private val idleTimeoutMs: Long = 120_000L
) {
    enum class State { NEW, ACTIVE, CLOSED }

    data class Key(
        val protocol: Int,
        val sourceIp: String,
        val sourcePort: Int,
        val destinationIp: String,
        val destinationPort: Int
    )

    data class Flow(
        val key: Key,
        @Volatile var state: State = State.NEW,
        @Volatile var packets: Long = 0,
        @Volatile var bytes: Long = 0,
        @Volatile var lastSeenMs: Long = System.currentTimeMillis()
    )

    private val flows = ConcurrentHashMap<Key, Flow>()

    fun observe(key: Key, bytes: Int): Flow? {
        cleanup()
        if (!flows.containsKey(key) && flows.size >= maxFlows) return null

        val flow = flows.computeIfAbsent(key) { Flow(it) }
        flow.state = State.ACTIVE
        flow.packets++
        flow.bytes += bytes.coerceAtLeast(0)
        flow.lastSeenMs = System.currentTimeMillis()
        return flow
    }

    fun close(key: Key) {
        flows[key]?.state = State.CLOSED
        flows.remove(key)
    }

    fun find(key: Key): Flow? = flows[key]

    fun size(): Int = flows.size

    fun snapshot(): List<Flow> = flows.values.toList()

    fun cleanup(nowMs: Long = System.currentTimeMillis()) {
        flows.entries.removeIf { (_, flow) ->
            nowMs - flow.lastSeenMs > idleTimeoutMs
        }
    }

    fun clear() = flows.clear()
}