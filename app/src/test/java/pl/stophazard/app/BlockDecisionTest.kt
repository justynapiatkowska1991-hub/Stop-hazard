package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDecisionTest {

    @Test
    fun blockedHostProducesBlockDecision() {
        val policy = DomainPolicy()
        assertEquals(BlockDecision.BLOCK, decideForHost("bet365.com", policy))
    }

    @Test
    fun safeHostProducesAllowDecision() {
        val policy = DomainPolicy()
        assertEquals(BlockDecision.ALLOW, decideForHost("example.com", policy))
    }

    @Test
    fun customBlockedHostProducesBlockDecision() {
        val policy = DomainPolicy(setOf("blocked.example"))
        assertEquals(BlockDecision.BLOCK, decideForHost("shop.blocked.example", policy))
    }
}
