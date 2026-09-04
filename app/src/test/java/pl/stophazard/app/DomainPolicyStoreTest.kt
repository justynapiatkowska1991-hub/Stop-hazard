package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DomainPolicyStoreTest {

    private lateinit var store: DomainPolicyStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("stop_hazard_domain_policy", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = DomainPolicyStore(context)
    }

    @Test
    fun storesValidCustomDomain() {
        assertTrue(store.addCustomDomain("Example-Gambling.test"))
        assertTrue(store.getCustomBlockedDomains().contains("example-gambling.test"))
    }

    @Test
    fun rejectsInvalidHostname() {
        assertFalse(store.addCustomDomain("not a domain"))
        assertFalse(store.addCustomDomain("-invalid.example"))
    }

    @Test
    fun removesCustomDomain() {
        store.addCustomDomain("example-gambling.test")
        store.removeCustomDomain("example-gambling.test")
        assertFalse(store.getCustomBlockedDomains().contains("example-gambling.test"))
    }
}
