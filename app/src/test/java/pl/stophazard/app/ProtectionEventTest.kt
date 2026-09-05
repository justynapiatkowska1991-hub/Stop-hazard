package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEventTest {
    @Test
    fun blockedDomainEventKeepsHostname() {
        val event = ProtectionEvent("bet365.com", true)
        assertTrue(event.blocked)
        assertEquals("bet365.com", event.host)
    }

    @Test
    fun protectionStateRepresentsEnabledState() {
        val state = ProtectionState(enabled = true, tier = SubscriptionTier.BASIC)
        assertTrue(state.enabled)
        assertEquals(SubscriptionTier.BASIC, state.tier)
    }
}
