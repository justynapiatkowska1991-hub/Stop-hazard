package pl.stophazard.app

/**
 * Validates TCP control progression for flows observed at the TUN boundary.
 *
 * This state machine does not fabricate packets or bypass the kernel TCP stack.
 * It is used to decide whether an observed segment may advance the logical
 * flow state before payload forwarding.
 */
class TcpProxyStateMachine {

    enum class Phase {
        NEW,
        SYN_SEEN,
        ESTABLISHED,
        FIN_SEEN,
        CLOSED
    }

    data class Segment(
        val sequence: Long,
        val acknowledgement: Long,
        val syn: Boolean,
        val ack: Boolean,
        val fin: Boolean,
        val rst: Boolean,
        val payloadLength: Int
    )

    data class State(
        var phase: Phase = Phase.NEW,
        var nextSequence: Long = 0,
        var nextAcknowledgement: Long = 0
    )

    fun accept(state: State, segment: Segment): Boolean {
        if (segment.rst) {
            state.phase = Phase.CLOSED
            return true
        }

        if (segment.payloadLength < 0) return false

        return when (state.phase) {
            Phase.NEW -> {
                if (!segment.syn || segment.ack) return false
                state.nextSequence = segment.sequence + 1
                state.phase = Phase.SYN_SEEN
                true
            }

            Phase.SYN_SEEN -> {
                if (!segment.ack) return false
                state.nextAcknowledgement = segment.acknowledgement
                state.phase = Phase.ESTABLISHED
                true
            }

            Phase.ESTABLISHED -> {
                val end = segment.sequence + segment.payloadLength +
                    if (segment.fin) 1 else 0
                if (end < segment.sequence) return false
                state.nextSequence = maxOf(state.nextSequence, end)
                state.nextAcknowledgement =
                    maxOf(state.nextAcknowledgement, segment.acknowledgement)
                if (segment.fin) state.phase = Phase.FIN_SEEN
                true
            }

            Phase.FIN_SEEN -> {
                if (segment.rst || segment.ack) {
                    state.nextAcknowledgement =
                        maxOf(state.nextAcknowledgement, segment.acknowledgement)
                    if (segment.fin) state.phase = Phase.CLOSED
                    true
                } else {
                    false
                }
            }

            Phase.CLOSED -> false
        }
    }
}