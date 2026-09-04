package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedDomainsTest {

    @Test
    fun blocksExactDomain() {
        assertTrue(BlockedDomains.isBlocked("bet365.com"))
    }

    @Test
    fun blocksSubdomain() {
        assertTrue(BlockedDomains.isBlocked("www.bet365.com"))
        assertTrue(BlockedDomains.isBlocked("m.bet365.com"))
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(BlockedDomains.isBlocked("WWW.BET365.COM"))
    }

    @Test
    fun doesNotBlockLookalikeDomain() {
        assertFalse(BlockedDomains.isBlocked("bet365.com.example.org"))
        assertFalse(BlockedDomains.isBlocked("notbet365.com"))
    }

    @Test
    fun ignoresTrailingDot() {
        assertTrue(BlockedDomains.isBlocked("bet365.com."))
    }
}
