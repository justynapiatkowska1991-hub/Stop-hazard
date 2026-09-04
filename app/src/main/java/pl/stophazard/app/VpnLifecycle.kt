package pl.stophazard.app

import android.content.Context
import android.content.Intent

/**
 * Central lifecycle adapter for the native VPN service.
 * It intentionally does not pretend that routing is active until the
 * service itself reports readiness.
 */
object VpnLifecycle {
    fun start(context: Context): Boolean = try {
        ProtectionController.start(context.applicationContext)
        true
    } catch (_: Throwable) {
        false
    }

    fun stop(context: Context) {
        try { ProtectionController.stop(context.applicationContext) }
        catch (_: Throwable) {}
    }

    fun consentIntent(context: Context): Intent? =
        android.net.VpnService.prepare(context.applicationContext)
}