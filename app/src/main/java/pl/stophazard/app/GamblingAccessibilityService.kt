package pl.stophazard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class GamblingAccessibilityService : AccessibilityService() {

    private var lastBlockedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg !in BROWSER_PACKAGES) return

        val text = StringBuilder()
        event.text.forEach { text.append(' ').append(it) }
        event.contentDescription?.let { text.append(' ').append(it) }
        collectNodeText(rootInActiveWindow, text, 0)

        val host = findBlockedHost(text.toString()) ?: return
        val now = System.currentTimeMillis()
        if (now - lastBlockedAt < 2500) return
        lastBlockedAt = now

        Toast.makeText(
            this,
            "STOP HAZARD — zablokowano: $host",
            Toast.LENGTH_LONG
        ).show()

        // Zamykamy bieżącą stronę i pokazujemy ekran blokady zamiast
        // próbować filtrować cały ruch internetowy przez niepełny VPN.
        performGlobalAction(GLOBAL_ACTION_BACK)
        startActivity(
            Intent(this, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockedActivity.EXTRA_HOST, host)
        )
    }

    private fun collectNodeText(
        node: AccessibilityNodeInfo?,
        out: StringBuilder,
        depth: Int
    ) {
        if (node == null || depth > 15) return

        node.text?.let { out.append(' ').append(it) }
        node.contentDescription?.let { out.append(' ').append(it) }

        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), out, depth + 1)
        }
    }

    private fun findBlockedHost(raw: String): String? {
        // Blokujemy wyłącznie rzeczywisty adres domeny, a nie samo słowo
        // występujące w treści zwykłej strony.
        val urls = Regex(
            """(?i)(?:https?://)?(?:www\.)?([a-z0-9][a-z0-9.-]*\.[a-z]{2,})(?:[/?:#\s]|$)"""
        ).findAll(raw)

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
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "com.android.browser"
        )
    }
}
