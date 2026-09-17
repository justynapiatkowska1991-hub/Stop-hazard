package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Usługa ochrony ruchu.
 *
 * Silnik jest tworzony wyłącznie przez TrafficFilterEngineFactory.
 * Dopóki prawdziwy silnik nie przejdzie testów routingu TCP/UDP/DNS,
 * fabryka zwraca bezpieczną implementację wyłączoną.
 */
class BlockVpnService : VpnService() {

    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Nie uruchamiamy pustego VPN ani tun2socks.
        // Wszystkie implementacje muszą przejść przez fabrykę.
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create()
        engine = selectedEngine

        val started = try {
            selectedEngine.start()
        } catch (_: Throwable) {
            // Awaria silnika nie może przejąć ruchu ani odciąć internetu.
            false
        }

        running.set(started)

        if (!started) {
            engine = null
            stopSelf(startId)
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopExistingEngine() {
        val previousEngine = engine ?: return
        try {
            previousEngine.stop()
        } catch (_: Throwable) {
            // Nie pozwalamy, aby awaria sprzątania przerwała bezpieczny start.
        }
        engine = null
        running.set(false)
    }

    private fun stopVpn() {
        stopExistingEngine()
        stopSelf()
    }
}
