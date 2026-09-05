package pl.stophazard.app

import android.accessibilityservice.AccessibilityService
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
        if (now - lastBlockedAt < 1500) return
        lastBlockedAt = now

        Toast.makeText(
            this,
            "STOP HAZARD — zablokowano: $host",
            Toast.LENGTH_LONG
        ).show()

        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun collectNodeText(
        node: AccessibilityNodeInfo?,
        out: StringBuilder,
        depth: Int
    ) {
        if (node == null || depth > 12) return

        node.text?.let { out.append(' ').append(it) }
        node.contentDescription?.let { out.append(' ').append(it) }

        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), out, depth + 1)
        }
    }

    private fun findBlockedHost(raw: String): String? {
        val normalized = raw.lowercase()
        return BlockedDomains.domains().firstOrNull { normalized.contains(it) }
    }

    override fun onInterrupt() = Unit

    companion object {
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.sec.android.app.sbrowser"
        )
    }
}
