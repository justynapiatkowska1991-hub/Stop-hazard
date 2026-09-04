package pl.stophazard.app

import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Native lifecycle facade used by the application layer. */
class ProtectionControllerFacade(private val context: Context) {

    fun prepareVpn(): Intent? = VpnService.prepare(context)

    fun isAuthorized(): Boolean = VpnService.prepare(context) == null

    fun start(): Boolean = try {
        ProtectionController.start(context)
        true
    } catch (_: Exception) {
        false
    }

    fun stop() {
        ProtectionController.stop(context)
    }

    fun syncWithState(enabled: Boolean): Boolean = try {
        if (enabled) start() else { stop(); true }
    } catch (_: Exception) {
        false
    }
}