package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainFilterTest {
    @Test
    fun blocksExactHazardDomain() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.check("sts.pl"))
    }

    @Test
    fun blocksSubdomain() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.check("login.fortuna.pl"))
    }

    @Test
    fun allowsUnrelatedDomain() {
        assertEquals(BlockDecision.ALLOW, DomainFilter.check("example.com"))
    }

    @Test
    fun normalizesUppercaseAndTrailingDot() {
        assertEquals(BlockDecision.BLOCK, DomainFilter.check("WWW.STS.PL."))
    }
}
