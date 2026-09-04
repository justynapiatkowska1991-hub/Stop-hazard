package pl.stophazard.app

class ProtectionController(
    private val settingsStore: ProtectionSettingsStore
) {
    fun isEnabled(): Boolean = settingsStore.get().enabled

    fun enable() = settingsStore.setEnabled(true)

    fun disable() = settingsStore.setEnabled(false)

    fun toggle(): Boolean {
        val next = !isEnabled()
        settingsStore.setEnabled(next)
        return next
    }

    fun current(): ProtectionSettings = settingsStore.get()
}
