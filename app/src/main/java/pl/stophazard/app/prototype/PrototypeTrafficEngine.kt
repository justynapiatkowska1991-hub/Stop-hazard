package pl.stophazard.app.prototype

/**
 * Bezpieczny szkielet przyszłego silnika ruchu.
 *
 * Ten prototyp NIE uruchamia VpnService, nie tworzy tunelu i nie przejmuje
 * ruchu urządzenia. Start jest domyślnie zablokowany, aby przypadkowo nie
 * odciąć internetu użytkownika.
 */
class PrototypeTrafficEngine {
    enum class State {
        STOPPED,
        STARTING,
        RUNNING,
        FAILED
    }

    var state: State = State.STOPPED
        private set

    /**
     * Uruchomienie jest możliwe wyłącznie w jawnie oznaczonym środowisku
     * testowym. Samo true nie oznacza jeszcze filtrowania ruchu.
     */
    fun start(allowStartForDeviceTest: Boolean = false): Boolean {
        if (!allowStartForDeviceTest) {
            state = State.STOPPED
            return false
        }

        // Celowo nie uruchamiamy tu VPN ani transportu ruchu.
        state = State.FAILED
        return false
    }

    fun stop() {
        state = State.STOPPED
    }
}
