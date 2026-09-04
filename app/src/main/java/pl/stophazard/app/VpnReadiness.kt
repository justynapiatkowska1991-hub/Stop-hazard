package pl.stophazard.app

import android.content.Context
import android.net.VpnService

/**
 * Small, deterministic VPN readiness check.
 * Keeps permission checks separate from packet routing.
 */
object VpnReadiness {
    data class Result(
        val prepared: Boolean,
        val serviceDeclared: Boolean,
        val reason: String? = null
    )

    fun check(context: Context): Result {
        val prepared = VpnService.prepare(context) == null
        return if (prepared) {
            Result(true, true)
        } else {
            Result(false, true, "vpn-consent-required")
        }
    }
}