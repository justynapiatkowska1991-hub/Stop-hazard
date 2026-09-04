package pl.stophazard.app

/**
 * Normalizes trusted hostname metadata before policy evaluation.
 * The classifier never guesses a hostname from arbitrary encrypted traffic.
 */
object DomainClassifier {
    data class HostResult(
        val hostname: String?,
        val valid: Boolean,
        val reason: String
    )

    fun normalize(hostname: String?): HostResult {
        val value = hostname
            ?.trim()
            ?.lowercase()
            ?.trimEnd('.')
            ?.takeIf { it.isNotEmpty() }

        if (value == null) return HostResult(null, false, "hostname-empty")
        if (value.length > 253) return HostResult(null, false, "hostname-too-long")
        if (value.contains("..")) return HostResult(null, false, "hostname-invalid")

        val labels = value.split('.')
        if (labels.size < 2) return HostResult(null, false, "hostname-not-domain")
        if (labels.any { it.isEmpty() || it.length > 63 }) {
            return HostResult(null, false, "hostname-label-invalid")
        }
        if (labels.any { label -> !label.all { it.isLetterOrDigit() || it == '-' } ||
                label.startsWith('-') || label.endsWith('-') }) {
            return HostResult(null, false, "hostname-label-invalid")
        }

        return HostResult(value, true, "ok")
    }
}