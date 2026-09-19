package pl.stophazard.app

/**
 * Jedno miejsce wyboru silnika filtrowania.
 *
 * Zwykły APK pozostaje bezpiecznie wyłączony. Nawet build z NetValve ma osobną
 * bramkę runtime, dzięki czemu sam test kompilacji nie może przejąć Internetu.
 */
object TrafficFilterEngineFactory {
    fun create(): TrafficFilterEngine = DisabledTrafficFilterEngine()

    fun create(service: BlockVpnService): TrafficFilterEngine {
        if (!BuildConfig.USE_NETSTACK || !BuildConfig.ENABLE_NETSTACK_RUNTIME) {
            return DisabledTrafficFilterEngine()
        }

        return runCatching {
            Class.forName("pl.stophazard.app.NetValveTrafficFilterEngine")
                .getDeclaredConstructor(android.net.VpnService::class.java)
                .newInstance(service) as TrafficFilterEngine
        }.getOrElse {
            DisabledTrafficFilterEngine()
        }
    }
}
