package pl.stophazard.app

import android.content.Context

data class ProtectionStats(
    val total: Int,
    val blocked: Int,
    val allowed: Int,
    val blockedRequests: Long = blocked.toLong(),
    val lastBlockedHost: String? = null
)

class ProtectionStatsStore(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun read(): ProtectionStats = read(context)

    fun recordBlocked(host: String) {
        val total = prefs.getInt(KEY_TOTAL, 0) + 1
        val blocked = prefs.getInt(KEY_BLOCKED, 0) + 1
        prefs.edit().putInt(KEY_TOTAL, total).putInt(KEY_BLOCKED, blocked)
            .putString(KEY_LAST_HOST, BlockedDomains.normalize(host)).apply()
    }

    fun record(blocked: Boolean) {
        val total = prefs.getInt(KEY_TOTAL, 0) + 1
        val blockedCount = prefs.getInt(KEY_BLOCKED, 0) + if (blocked) 1 else 0
        val allowed = prefs.getInt(KEY_ALLOWED, 0) + if (blocked) 0 else 1
        prefs.edit().putInt(KEY_TOTAL, total).putInt(KEY_BLOCKED, blockedCount)
            .putInt(KEY_ALLOWED, allowed).apply()
    }

    companion object {
        private const val PREF = "stop_hazard_stats"
        private const val KEY_TOTAL = "total"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_ALLOWED = "allowed"
        private const val KEY_LAST_HOST = "last_blocked_host"

        fun read(context: Context): ProtectionStats {
            val p = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val blocked = p.getInt(KEY_BLOCKED, 0)
            return ProtectionStats(
                total = p.getInt(KEY_TOTAL, 0),
                blocked = blocked,
                allowed = p.getInt(KEY_ALLOWED, 0),
                blockedRequests = blocked.toLong(),
                lastBlockedHost = p.getString(KEY_LAST_HOST, null)
            )
        }

        fun record(context: Context, blocked: Boolean) {
            ProtectionStatsStore(context).record(blocked)
        }
    }
}
