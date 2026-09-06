package pl.stophazard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class GamblingAccessibilityService : AccessibilityService() {

    private var lastBlockedAt = 0L
    private var lastBlockedHost = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg !in BROWSER_PACKAGES) return

        val text = StringBuilder()

        // Najpierw odczytujemy tylko pasek adresu. Dzięki temu zwykła
        // treść stron nie jest traktowana jako adres i nie jest blokowana.
        findAddressBar(rootInActiveWindow)?.let {
            text.append(' ').append(it)
        }

        // Fallback dla przeglądarek, które nie udostępniają identyfikatora
        // paska adresu.
        event.text.forEach { text.append(' ').append(it) }
        event.contentDescription?.let { text.append(' ').append(it) }

        val host = findBlockedHost(text.toString()) ?: return
        val now = System.currentTimeMillis()

        if (host == lastBlockedHost && now - lastBlockedAt < 4000) return

        lastBlockedHost = host
        lastBlockedAt = now

        Toast.makeText(
            this,
            "STOP HAZARD — zablokowano: $host",
            Toast.LENGTH_LONG
        ).show()

        performGlobalAction(GLOBAL_ACTION_BACK)

        startActivity(
            Intent(this, BlockedActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                .putExtra(BlockedActivity.EXTRA_HOST, host)
        )
    }

    private fun findAddressBar(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null

        val id = root.viewIdResourceName ?: ""
        val className = root.className?.toString() ?: ""
        val hint = root.hintText?.toString() ?: ""
        val text = root.text?.toString() ?: ""
        val description = root.contentDescription?.toString() ?: ""

        val looksLikeUrlBar =
            id.contains("url_bar", ignoreCase = true) ||
                id.contains("location_bar", ignoreCase = true) ||
                id.contains("address", ignoreCase = true) ||
                hint.contains("address", ignoreCase = true) ||
                hint.contains("adres", ignoreCase = true) ||
                (className.contains("EditText") &&
                    (text.contains("http", ignoreCase = true) ||
                        text.contains(".com", ignoreCase = true) ||
                        text.contains(".pl", ignoreCase = true)))

        if (looksLikeUrlBar) {
            return listOf(text, description).joinToString(" ")
        }

        for (i in 0 until root.childCount) {
            val found = findAddressBar(root.getChild(i))
            if (!found.isNullOrBlank()) return found
        }

        return null
    }

    private fun findBlockedHost(raw: String): String? {
        val urls = URL_REGEX.findAll(raw)

        for (match in urls) {
            val host = match.groupValues[1]
            if (BlockedDomains.isBlocked(host)) {
                return BlockedDomains.normalize(host)
            }
        }

        return null
    }

    override fun onInterrupt() = Unit

    companion object {
        private val URL_REGEX = Regex(
            """(?i)(?:https?://)?(?:www\.)?([a-z0-9][a-z0-9.-]*\.[a-z]{2,})(?:[/?:#\s]|$)"""
        )

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "com.android.browser",
            "com.mi.globalbrowser"
        )
    }
}
