package pl.stophazard.app

/**
 * Small, Android-independent HTTP request inspector used before TCP relay.
 * It only extracts a hostname; it does not claim to parse every HTTP variant.
 */
internal object HttpRequestInspector {
    private val knownMethods = listOf(
        "GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS", "PATCH", "CONNECT", "TRACE",
    )

    fun looksLikeRequest(data: ByteArray, length: Int): Boolean {
        if (length <= 0) return false
        val prefix = data.copyOf(minOf(length, 16)).toString(Charsets.ISO_8859_1)
        val method = prefix.takeWhile { !it.isWhitespace() }
        if (method.isEmpty() || method.length > 10) return false
        return knownMethods.any { it.startsWith(method) || method.startsWith(it) }
    }

    fun hasHeaderTerminator(data: ByteArray, length: Int): Boolean {
        if (length < 4) return false
        for (i in 0..length - 4) {
            if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() &&
                data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()
            ) return true
        }
        return false
    }

    fun parseHost(data: ByteArray, length: Int): String? {
        if (length <= 0 || length > data.size) return null
        val text = data.copyOf(length).toString(Charsets.ISO_8859_1)
        val requestLine = text.lineSequence().firstOrNull()?.trim().orEmpty()

        val connectTarget = Regex("(?i)^CONNECT\\s+([^\\s]+)")
            .find(requestLine)?.groupValues?.get(1)
        val absoluteTarget = Regex("(?i)^[A-Z]+\\s+https?://([^/:?#\\s]+)")
            .find(requestLine)?.groupValues?.get(1)
        val headerHost = Regex("(?im)^Host\\s*:\\s*([^\\s]+)")
            .find(text)?.groupValues?.get(1)
        val rawHost = connectTarget ?: absoluteTarget ?: headerHost ?: return null

        val host = if (rawHost.startsWith("[")) {
            rawHost.substringAfter('[').substringBefore(']')
        } else {
            rawHost.substringBefore(':')
        }
        return host.trimEnd('.').lowercase().takeIf { it.isNotBlank() }
    }
}
