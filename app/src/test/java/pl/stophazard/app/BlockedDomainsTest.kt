package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedDomainsTest {
    @Test
    fun exactKnownDomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("sts.pl"))

    @Test
    fun wwwKnownDomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("www.fortuna.pl"))

    @Test
    fun subdomainOfKnownDomainIsBlocked() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("login.bet365.com"))

    @Test
    fun hostNameIsCaseInsensitive() =
        assertEquals(BlockDecision.BLOCK, BlockedDomains.decision("WWW.STS.PL."))

    @Test
    fun unrelatedDomainIsAllowed() =
        assertEquals(BlockDecision.ALLOW, BlockedDomains.decision("example.com"))

    @Test
    fun similarButDifferentDomainIsAllowed() =
        assertEquals(BlockDecision.ALLOW, BlockedDomains.decision("notsts.pl"))
}
