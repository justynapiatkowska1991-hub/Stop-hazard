package pl.stophazard.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class DomainRuleRepositoryTest {
    private lateinit var repo:DomainRuleRepository

    @Before fun setUp(){
        val c=ApplicationProvider.getApplicationContext<Context>()
        repo=DomainRuleRepository(c)
        repo.remove("example.com")
    }

    @Test fun parentDomainMatchesSubdomain(){
        repo.upsert(DomainRule("example.com",DomainRuleType.CUSTOM,true))
        assertTrue(repo.isBlocked("example.com"))
        assertTrue(repo.isBlocked("www.example.com"))
        assertTrue(repo.isBlocked("shop.example.com"))
        assertFalse(repo.isBlocked("example.net"))
    }

    @Test fun disabledRuleDoesNotBlock(){
        repo.upsert(DomainRule("example.com",DomainRuleType.CUSTOM,false))
        assertFalse(repo.isBlocked("example.com"))
    }
}
