package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks TCP sequence/acknowledgement state per logical flow.
 * This is state tracking only; it does not synthesize arbitrary TCP handshakes.
 */
class TcpFlowStateTable(
    private val maxFlows: Int = 2048,
    private val idleTimeoutMs: Long = 120_000L
) {
    enum class Phase { SYN_SENT, ESTABLISHED, FIN_WAIT, CLOSED }

    data class Key(
        val sourceIp: String,
        val sourcePort: Int,
        val destinationIp: String,
        val destinationPort: Int
    )

    data class State(
        val key: Key,
        @Volatile var phase: Phase = Phase.SYN_SENT,
        @Volatile var nextSequence: Long = 0,
        @Volatile var nextAcknowledgement: Long = 0,
        @Volatile var lastSeen: Long = System.currentTimeMillis()
    )

    private val table = ConcurrentHashMap<Key, State>()

    fun open(key: Key, initialSequence: Long): State? {
        cleanup()
        if (!table.containsKey(key) && table.size >= maxFlows) return null
        return table.computeIfAbsent(key) {
            State(it, Phase.SYN_SENT, initialSequence + 1, 0)
        }.also { it.lastSeen = System.currentTimeMillis() }
    }

    fun establish(key: Key, acknowledgement: Long) {
        table[key]?.apply {
            phase = Phase.ESTABLISHED
            nextAcknowledgement = acknowledgement
            lastSeen = System.currentTimeMillis()
        }
    }

    fun update(key: Key, nextSequence: Long, acknowledgement: Long) {
        table[key]?.apply {
            this.nextSequence = nextSequence
            this.nextAcknowledgement = acknowledgement
            this.lastSeen = System.currentTimeMillis()
        }
    }

    fun closing(key: Key) {
        table[key]?.apply {
            phase = Phase.FIN_WAIT
            lastSeen = System.currentTimeMillis()
        }
    }

    fun close(key: Key) {
        table.remove(key)?.phase = Phase.CLOSED
    }

    fun find(key: Key): State? = table[key]

    fun snapshot(): List<State> = table.values.toList()

    fun cleanup(now: Long = System.currentTimeMillis()) {
        table.entries.removeIf { (_, state) ->
            now - state.lastSeen > idleTimeoutMs
        }
    }

    fun clear() = table.clear()
}