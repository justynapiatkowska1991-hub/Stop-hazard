package pl.stophazard.app

import android.content.Context

class ProtectionStatsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): ProtectionStats = ProtectionStats(
        blockedRequests = prefs.getLong(KEY_BLOCKED, 0L),
        lastBlockedHost = prefs.getString(KEY_LAST_HOST, null)
    )

    @Synchronized
    fun recordBlocked(host: String) {
        val normalized = BlockedDomains.normalize(host)
        prefs.edit()
            .putLong(KEY_BLOCKED, read().blockedRequests + 1L)
            .putString(KEY_LAST_HOST, normalized)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "stop_hazard_stats"
        private const val KEY_BLOCKED = "blocked_requests"
        private const val KEY_LAST_HOST = "last_blocked_host"
    }
}
