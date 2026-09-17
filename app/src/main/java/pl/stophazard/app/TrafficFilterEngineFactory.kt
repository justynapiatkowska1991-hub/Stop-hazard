package pl.stophazard.app

/**
 * Jedno miejsce wyboru silnika filtrowania.
 *
 * Dopóki rzeczywisty silnik nie przejdzie testów routingu TCP/UDP/DNS,
 * aplikacja zawsze wybiera implementację wyłączoną. Nie wolno tu podłączać
 * pustego VPN ani nieprzetestowanego tun2socks.
 */
object TrafficFilterEngineFactory {
    fun create(): TrafficFilterEngine = DisabledTrafficFilterEngine()
}
