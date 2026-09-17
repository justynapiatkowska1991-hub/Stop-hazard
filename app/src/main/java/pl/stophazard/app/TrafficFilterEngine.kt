package pl.stophazard.app

/**
 * Wspólny kontrakt dla rzeczywistego silnika filtrowania ruchu.
 *
 * Implementacja produkcyjna może zostać podłączona dopiero po testach
 * przekazywania zwykłego ruchu internetowego. Brak silnika nie może
 * powodować uruchomienia pustego VPN odcinającego internet.
 */
interface TrafficFilterEngine {
    fun start(): Boolean
    fun stop()
    fun isRunning(): Boolean
}

/**
 * Bezpieczna implementacja tymczasowa.
 * Nigdy nie przejmuje ruchu i zawsze pozostaje wyłączona.
 */
class DisabledTrafficFilterEngine : TrafficFilterEngine {
    override fun start(): Boolean = false

    override fun stop() = Unit

    override fun isRunning(): Boolean = false
}
