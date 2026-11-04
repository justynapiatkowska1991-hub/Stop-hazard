package pl.stophazard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRequestInspectorTest {
    @Test
    fun extractsHostHeaderCaseInsensitivelyAndRemovesPort() {
        assertEquals("sts.pl", parse("GET / HTTP/1.1\r\nhOsT: STS.PL:8080\r\n\r\n"))
    }

    @Test
    fun extractsConnectAuthority() {
        assertEquals("fortuna.pl", parse("CONNECT fortuna.pl:443 HTTP/1.1\r\n\r\n"))
    }

    @Test
    fun extractsAbsoluteFormUrlHost() {
        assertEquals("casino.example", parse("GET https://casino.example/path?q=1 HTTP/1.1\r\n\r\n"))
    }

    @Test
    fun handlesBracketedIpv6Authority() {
        assertEquals("2001:db8::1", parse("CONNECT [2001:db8::1]:443 HTTP/1.1\r\n\r\n"))
    }

    @Test
    fun returnsNullWhenHostIsMissingOrInputLengthIsInvalid() {
        val bytes = "GET / HTTP/1.1\r\n\r\n".toByteArray()
        assertNull(HttpRequestInspector.parseHost(bytes, bytes.size))
        assertNull(HttpRequestInspector.parseHost(bytes, bytes.size + 1))
    }

    @Test
    fun detectsFragmentedMethodPrefixesAndHeaderTerminator() {
        assertTrue(HttpRequestInspector.looksLikeRequest("CON".toByteArray(), 3))
        assertFalse(HttpRequestInspector.looksLikeRequest("SSH-2.0".toByteArray(), 7))
        assertFalse(HttpRequestInspector.looksLikeRequest("GET".toByteArray(), 4))
        assertFalse(HttpRequestInspector.hasHeaderTerminator("GET /".toByteArray(), 5))
        assertFalse(HttpRequestInspector.hasHeaderTerminator("GET".toByteArray(), 4))
        val complete = "GET / HTTP/1.1\r\n\r\n".toByteArray()
        assertTrue(HttpRequestInspector.hasHeaderTerminator(complete, complete.size))
    }

    private fun parse(request: String): String? {
        val bytes = request.toByteArray(Charsets.ISO_8859_1)
        return HttpRequestInspector.parseHost(bytes, bytes.size)
    }
}
