package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Usługa ochrony ruchu.
 *
 * Domyślny build nadal wybiera DisabledTrafficFilterEngine. Dopiero specjalny
 * build integracyjny może utworzyć adapter NetValve, a jego uruchomienie jest
 * nadal chronione przez wynik start().
 */
class BlockVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private var engine: TrafficFilterEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopExistingEngine()

        val selectedEngine = TrafficFilterEngineFactory.create(this)
        engine = selectedEngine

        val started = try {
            selectedEngine.start()
        } catch (_: Throwable) {
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
            // Cleanup failure must not keep the service alive.
        }
        engine = null
        running.set(false)
    }

    private fun stopVpn() {
        stopExistingEngine()
        stopSelf()
    }
}
