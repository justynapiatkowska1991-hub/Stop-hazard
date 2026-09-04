package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.IOException

class HazardVpnService : VpnService() {
    private var vpn: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (vpn == null) startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        vpn = try {
            Builder()
                .setSession("STOP HAZARD")
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .establish()
        } catch (_:Exception) { null }
    }

    override fun onDestroy() {
        try { vpn?.close() } catch (_:IOException) {}
        vpn=null
        super.onDestroy()
    }
}
