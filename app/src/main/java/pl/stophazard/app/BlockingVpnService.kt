package pl.stophazard.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class BlockingVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        establishLocalInterface()
        return START_STICKY
    }

    private fun establishLocalInterface() {
        if (vpnInterface != null) return

        vpnInterface = Builder()
            .setSession("STOP HAZARD")
            .addAddress("10.10.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)
            .establish()
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
