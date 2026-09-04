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

    fun start() {
        val intent = Intent(context, BlockingVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop() {
        context.stopService(Intent(context, BlockingVpnService::class.java))
    }
}
