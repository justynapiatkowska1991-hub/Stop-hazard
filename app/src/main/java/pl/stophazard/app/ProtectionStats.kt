package pl.stophazard.app

data class ProtectionStats(
    val total: Int,
    val blocked: Int,
    val allowed: Int,
    val blockedRequests: Long = blocked.toLong(),
    val lastBlockedHost: String? = null
)

object ProtectionStatsStore {
    private const val PREF = "stop_hazard_stats"
    private const val KEY_TOTAL = "total"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_ALLOWED = "allowed"
    private const val KEY_LAST_HOST = "last_blocked_host"

    fun read(context: android.content.Context): ProtectionStats {
        val prefs = context.applicationContext.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val blocked = prefs.getInt(KEY_BLOCKED, 0)
        return ProtectionStats(
            total = prefs.getInt(KEY_TOTAL, 0),
            blocked = blocked,
            allowed = prefs.getInt(KEY_ALLOWED, 0),
            blockedRequests = blocked.toLong(),
            lastBlockedHost = prefs.getString(KEY_LAST_HOST, null)
        )
    }

    fun record(context: android.content.Context, blocked: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val total = prefs.getInt(KEY_TOTAL, 0) + 1
        val blockedCount = prefs.getInt(KEY_BLOCKED, 0) + if (blocked) 1 else 0
        val allowedCount = prefs.getInt(KEY_ALLOWED, 0) + if (blocked) 0 else 1
        prefs.edit()
            .putInt(KEY_TOTAL, total)
            .putInt(KEY_BLOCKED, blockedCount)
            .putInt(KEY_ALLOWED, allowedCount)
            .apply()
    }

    fun recordBlocked(context: android.content.Context, host: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val total = prefs.getInt(KEY_TOTAL, 0) + 1
        val blockedCount = prefs.getInt(KEY_BLOCKED, 0) + 1
        prefs.edit()
            .putInt(KEY_TOTAL, total)
            .putInt(KEY_BLOCKED, blockedCount)
            .putString(KEY_LAST_HOST, BlockedDomains.normalize(host))
            .apply()
    }
}
