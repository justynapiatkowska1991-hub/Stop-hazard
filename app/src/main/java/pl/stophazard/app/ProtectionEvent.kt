package pl.stophazard.app

data class ProtectionEvent(
    val host: String,
    val blocked: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)
