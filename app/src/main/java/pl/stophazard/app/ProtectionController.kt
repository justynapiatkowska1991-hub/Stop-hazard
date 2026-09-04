package pl.stophazard.app

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build

/**
 * Coordinates user intent and the VPN service.
 * Billing remains separate so protection logic is not coupled to payments.
 */
class ProtectionController(private val context: Context) {

    fun prepareVpn(): Intent? = VpnService.prepare(context)

    fun isAuthorized(): Boolean = VpnService.prepare(context) == null

    fun start(): Boolean {
        if (VpnService.prepare(context) != null) return false
        val intent = Intent(context, BlockingVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        return true
    }

    fun stop() {
        context.stopService(Intent(context, BlockingVpnService::class.java))
    }

    fun syncWithState(enabled: Boolean): Boolean {
        if (!enabled) {
            stop()
            return true
        }
        if (!isAuthorized()) return false
        return start()
    }
}
