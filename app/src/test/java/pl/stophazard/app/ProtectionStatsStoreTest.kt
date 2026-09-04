package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProtectionStatsStoreTest {
    private lateinit var store: ProtectionStatsStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = ProtectionStatsStore(context)
        store.reset()
    }

    @Test
    fun recordsBlockedHost() {
        store.recordBlocked("WWW.Example.com.")
        val stats = store.read()
        assertEquals(1L, stats.blockedRequests)
        assertEquals("example.com", stats.lastBlockedHost)
    }
}
