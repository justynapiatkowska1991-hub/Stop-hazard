package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainFilterTest {
    @Test
    fun blocksExactHazardDomain() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.decision("sts.pl"))
    }

    @Test
    fun blocksSubdomain() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.decision("login.fortuna.pl"))
    }

    @Test
    fun allowsUnrelatedDomain() {
        assertEquals(BlockDecision.ALLOW, DomainFilter.decision("example.com"))
    }

    @Test
    fun normalizesUppercaseAndTrailingDot() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.decision("WWW.STS.PL."))
    }
}
