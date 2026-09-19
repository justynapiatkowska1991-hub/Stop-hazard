package pl.stophazard.app

/**
 * Jedno miejsce wyboru silnika filtrowania.
 *
 * Zwykły APK pozostaje bezpiecznie wyłączony. Produkcyjny adapter NetValve jest
 * dostępny tylko w specjalnym buildzie z -Pstophazard.netstack=true i nadal
 * wymaga osobnego testu na urządzeniu przed włączeniem przycisku OCHRONA.
 */
object TrafficFilterEngineFactory {
    fun create(): TrafficFilterEngine = DisabledTrafficFilterEngine()

    fun create(service: BlockVpnService): TrafficFilterEngine {
        if (!BuildConfig.USE_NETSTACK) return DisabledTrafficFilterEngine()

        // Reflection keeps the safe APK free of the native NetValve AAR.
        return runCatching {
            Class.forName("pl.stophazard.app.NetValveTrafficFilterEngine")
                .getDeclaredConstructor(android.net.VpnService::class.java)
                .newInstance(service) as TrafficFilterEngine
        }.getOrElse {
            DisabledTrafficFilterEngine()
        }
    }
}
