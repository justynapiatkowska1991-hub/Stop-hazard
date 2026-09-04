package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEventTest {

    @Test
    fun blockedDomainEventKeepsHostname() {
        val event = ProtectionEvent.DomainBlocked("bet365.com")
        assertTrue(event is ProtectionEvent.DomainBlocked)
        assertEquals("bet365.com", event.hostname)
    }

    @Test
    fun authorizationEventIsDistinct() {
        val event = ProtectionEvent.VpnAuthorizationRequired
        assertTrue(event is ProtectionEvent.VpnAuthorizationRequired)
    }
}
