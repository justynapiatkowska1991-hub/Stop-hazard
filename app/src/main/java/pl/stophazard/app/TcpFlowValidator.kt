package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap

class TcpFlowValidator(private val maxStates: Int = 2048) {
    private val states = ConcurrentHashMap<TcpFlowStateTable.Key, TcpProxyStateMachine.State>()
    private val machine = TcpProxyStateMachine()

    fun validate(key: TcpFlowStateTable.Key, segment: TcpSegmentParser.Segment): Boolean {
        var state = states[key]
        if (state == null) {
            if (states.size >= maxStates) return false
            val candidate = TcpProxyStateMachine.State()
            state = states.putIfAbsent(key, candidate) ?: candidate
        }
        val accepted = machine.accept(
            state,
            TcpProxyStateMachine.Segment(
                segment.sequence, segment.acknowledgement, segment.syn,
                segment.ack, segment.fin, segment.rst, segment.payloadLength
            )
        )
        if (!accepted || state.phase == TcpProxyStateMachine.Phase.CLOSED) states.remove(key)
        return accepted
    }

    fun remove(key: TcpFlowStateTable.Key) { states.remove(key) }
    fun clear() = states.clear()
    fun size(): Int = states.size
}