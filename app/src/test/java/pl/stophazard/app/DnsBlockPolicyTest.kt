package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsBlockPolicyTest {

    @Test
    fun enabledPolicyBlocksGamblingDomain() {
        val policy = DnsBlockPolicy(enabled = true)
        assertTrue(policy.shouldBlock("www.bet365.com"))
    }

    @Test
    fun enabledPolicyAllowsUnlistedDomain() {
        val policy = DnsBlockPolicy(enabled = true)
        assertFalse(policy.shouldBlock("example.com"))
    }

    @Test
    fun disabledPolicyAllowsEverything() {
        val policy = DnsBlockPolicy(enabled = false)
        assertFalse(policy.shouldBlock("bet365.com"))
    }
}
