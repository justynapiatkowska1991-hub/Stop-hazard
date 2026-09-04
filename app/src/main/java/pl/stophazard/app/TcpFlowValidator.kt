package pl.stophazard.app

import java.util.concurrent.ConcurrentHashMap

/**
 * Connects TCP parsing with the TCP state machine.
 * Invalid control progression is rejected before payload forwarding.
 */
class TcpFlowValidator(
    private val maxStates: Int = 2048
) {
    private val states = ConcurrentHashMap<TcpFlowStateTable.Key, TcpProxyStateMachine.State>()
    private val machine = TcpProxyStateMachine()

    fun validate(
        key: TcpFlowStateTable.Key,
        segment: TcpSegmentParser.Segment
    ): Boolean {
        val state = states.computeIfAbsent(key) {
            if (states.size >= maxStates) return false
            TcpProxyStateMachine.State()
        }

        val accepted = machine.accept(
            state,
            TcpProxyStateMachine.Segment(
                sequence = segment.sequence,
                acknowledgement = segment.acknowledgement,
                syn = segment.syn,
                ack = segment.ack,
                fin = segment.fin,
                rst = segment.rst,
                payloadLength = segment.payloadLength
            )
        )

        if (!accepted || state.phase == TcpProxyStateMachine.Phase.CLOSED) {
            states.remove(key)
        }
        return accepted
    }

    fun remove(key: TcpFlowStateTable.Key) {
        states.remove(key)
    }

    fun clear() = states.clear()

    fun size(): Int = states.size
}