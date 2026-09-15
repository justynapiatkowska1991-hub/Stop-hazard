package pl.stophazard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bezpieczny tryb awaryjny.
 *
 * Poprzednia implementacja przejmowała cały ruch IPv4 przez tun2socks,
 * ale nie obsługiwała poprawnie całego ruchu, przez co internet znikał.
 * Dopóki filtr DNS/TUN nie zostanie poprawnie zaimplementowany i przetestowany,
 * usługa NIE uruchamia VPN. Dzięki temu aplikacja nie może odciąć internetu.
 *
 * Uwaga: w tym trybie blokowanie stron jest tymczasowo wyłączone.
 */
class BlockVpnService : VpnService() {

    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Celowo nie wywołujemy Builder().establish() ani tun2socks.
        // Bezpieczeństwo połączenia internetowego ma pierwszeństwo.
        running.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

    private fun stopVpn() {
        running.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("unused")
    private fun startForegroundNotification() {
        val channelId = "stop_hazard_vpn"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "STOP HAZARD",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
                    .setContentTitle("STOP HAZARD")
                    .setContentText("Ochrona jest chwilowo niedostępna")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("STOP HAZARD")
                    .setContentText("Ochrona jest chwilowo niedostępna")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(false)
                    .build()
            }

        startForeground(1001, notification)
    }
}
