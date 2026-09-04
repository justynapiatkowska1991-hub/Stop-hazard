package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProtectionEventStoreTest {
    private lateinit var store: ProtectionEventStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = ProtectionEventStore(context)
        store.clear()
    }

    @Test
    fun storesAndReadsEvents() {
        store.add(ProtectionEvent("blocked.example", true, 123L))
        val result = store.read()
        assertEquals(1, result.size)
        assertEquals("blocked.example", result.first().host)
        assertTrue(result.first().blocked)
        assertEquals(123L, result.first().timestampMillis)
    }

    @Test
    fun clearRemovesEvents() {
        store.add(ProtectionEvent("blocked.example", true))
        store.clear()
        assertEquals(0, store.read().size)
    }
}
