package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionDashboardStateTest {
    @Test
    fun protectionIsReadyOnlyWhenEnabledAndAuthorized() {
        assertFalse(
            ProtectionDashboardState(enabled = true, vpnAuthorized = false).protectionReady
        )
        assertFalse(
            ProtectionDashboardState(enabled = false, vpnAuthorized = true).protectionReady
        )
        assertTrue(
            ProtectionDashboardState(enabled = true, vpnAuthorized = true).protectionReady
        )
    }
}
