package pl.stophazard.app

import android.net.VpnService

/** Central VPN interface configuration. */
object VpnConfiguration {
    const val ADDRESS = "10.8.0.2"
    const val ROUTE = "0.0.0.0"
    const val PREFIX = 0
    const val SESSION = "STOP HAZARD"

    fun build(builder: VpnService.Builder): VpnService.Builder =
        builder
            .setSession(SESSION)
            .addAddress(ADDRESS, 32)
            .addRoute(ROUTE, PREFIX)
            .setBlocking(false)
}