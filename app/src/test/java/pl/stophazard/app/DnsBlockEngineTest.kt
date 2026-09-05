package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class DnsBlockEngineTest {
    private lateinit var stats: ProtectionStatsStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        stats = ProtectionStatsStore(context)
        stats.reset()
    }

    @Test
    fun blockedDomainProducesResponseAndStats() {
        val engine = DnsBlockEngine(DomainPolicy(), stats)
        val packet = query("bet365.com")
        val result = engine.inspect(packet)

        assertTrue(result.blocked)
        assertNotNull(result.response)
        assertTrue(stats.read().blockedRequests == 1L)
    }

    @Test
    fun safeDomainIsNotBlocked() {
        val engine = DnsBlockEngine(DomainPolicy(), stats)
        val result = engine.inspect(query("example.com"))

        assertFalse(result.blocked)
        assertTrue(result.hostname == "example.com")
    }

    private fun query(host: String): ByteArray {
        val out = ArrayList<Byte>()
        fun u16(v: Int) {
            out += (v ushr 8).toByte()
            out += v.toByte()
        }
        u16(0x4321)
        u16(0x0100)
        u16(1); u16(0); u16(0); u16(0)
        host.split(".").forEach {
            out += it.length.toByte()
            it.toByteArray(Charsets.US_ASCII).forEach(out::add)
        }
        out += 0
        u16(1); u16(1)
        return out.toByteArray()
    }
}
