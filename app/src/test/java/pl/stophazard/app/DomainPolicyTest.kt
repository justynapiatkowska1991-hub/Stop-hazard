package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPolicyTest {

    @Test
    fun blocksBuiltInDomain() {
        val policy = DomainPolicy()
        assertTrue(policy.shouldBlock("bet365.com"))
    }

    @Test
    fun blocksCustomDomainAndSubdomain() {
        val policy = DomainPolicy(setOf("example-gambling.test"))
        assertTrue(policy.shouldBlock("example-gambling.test"))
        assertTrue(policy.shouldBlock("www.example-gambling.test"))
    }

    @Test
    fun doesNotBlockLookalikeCustomDomain() {
        val policy = DomainPolicy(setOf("example-gambling.test"))
        assertFalse(policy.shouldBlock("notexample-gambling.test"))
    }
}
