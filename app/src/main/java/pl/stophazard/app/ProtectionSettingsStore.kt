package pl.stophazard.app

import android.content.Context

class ProtectionSettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("stop_hazard_settings", Context.MODE_PRIVATE)

    fun get(): ProtectionSettings = ProtectionSettings(
        enabled = prefs.getBoolean("enabled", true),
        mode = if (prefs.getString("mode", ProtectionMode.STANDARD.name) == ProtectionMode.STRICT.name)
            ProtectionMode.STRICT else ProtectionMode.STANDARD,
        blockGambling = prefs.getBoolean("block_gambling", true),
        blockBetting = prefs.getBoolean("block_betting", true),
        blockPoker = prefs.getBoolean("block_poker", true),
        blockLotteries = prefs.getBoolean("block_lotteries", true),
        blockPromotions = prefs.getBoolean("block_promotions", true),
        safeSearch = prefs.getBoolean("safe_search", true),
        notifications = prefs.getBoolean("notifications", true)
    )

    fun setEnabled(value: Boolean) = prefs.edit().putBoolean("enabled", value).apply()

    fun setMode(value: ProtectionMode) =
        prefs.edit().putString("mode", value.name).apply()

    fun setCategories(
        gambling: Boolean,
        betting: Boolean,
        poker: Boolean,
        lotteries: Boolean,
        promotions: Boolean
    ) = prefs.edit()
        .putBoolean("block_gambling", gambling)
        .putBoolean("block_betting", betting)
        .putBoolean("block_poker", poker)
        .putBoolean("block_lotteries", lotteries)
        .putBoolean("block_promotions", promotions)
        .apply()

    fun setSafeSearch(value: Boolean) =
        prefs.edit().putBoolean("safe_search", value).apply()

    fun setNotifications(value: Boolean) =
        prefs.edit().putBoolean("notifications", value).apply()

    fun reset() {
        prefs.edit().clear().apply()
    }
}
