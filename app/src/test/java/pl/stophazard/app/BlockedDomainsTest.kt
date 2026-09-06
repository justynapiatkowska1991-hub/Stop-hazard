package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedDomainsTest {
    @Test fun exactDomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("casino1.com"))

    @Test fun wwwDomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("www.casino1.com"))

    @Test fun subdomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("login.casino1.com"))

    @Test fun unrelatedDomainIsAllowed() =
        assertEquals(BlockDecision.ALLOW, BlockedDomains.decision("example.com"))
}
