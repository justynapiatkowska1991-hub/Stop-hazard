package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bezpieczny tryb awaryjny.
 *
 * Usługa nie uruchamia pustego VPN. Korzysta z jawnego kontraktu silnika,
 * którego tymczasowa implementacja jest wyłączona. Dzięki temu przypadkowe
 * uruchomienie usługi nie odcina całego internetu.
 */
class BlockVpnService : VpnService() {

    private val running = AtomicBoolean(false)
    private val engine: TrafficFilterEngine = DisabledTrafficFilterEngine()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Nie wywołujemy Builder().establish() ani tun2socks.
        // Silnik zostanie podłączony dopiero po testach pełnego routingu.
        val started = engine.start()
        running.set(started)

        if (!started) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        engine.stop()
        running.set(false)
        stopSelf()
    }
}
