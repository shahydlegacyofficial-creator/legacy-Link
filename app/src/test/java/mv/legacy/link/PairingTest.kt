package mv.legacy.link

import org.junit.Assert.*
import org.junit.Test

class PairingTest {
    @Test fun acceptsAssistantAddressesAndRejectsUnsafeInput() {
        assertEquals("http://100.80.10.2:3000", Pairing.normalizeHost(" 100.80.10.2:3000/ "))
        assertEquals("https://legacy.example/bridge", Pairing.normalizeHost("https://legacy.example/bridge/"))
        assertEquals("http://[::1]:3000", Pairing.normalizeHost("http://[::1]:3000"))
        assertEquals("https://legacy.example", Pairing.normalizeHost("legacy.example", false))
        assertEquals("https://legacy.example", Pairing.normalizeHost("https://legacy.example/", false))
        assertNull(Pairing.normalizeHost("http://100.80.10.2:3000", false))
        for (input in listOf("", "ftp://legacy", "https://", "http://user:secret@legacy", "http://legacy?secret=x", "http://legacy#x", "http://legacy:0", "http://legacy:70000", "http://legacy\nInjected:yes", "http://bad host")) {
            assertNull(input, Pairing.normalizeHost(input))
        }
        assertTrue(Pairing.validSecret("test-secret_123"))
        for (secret in listOf("", " ", "x\r\nInjected: true", "x\u0000", "☃")) assertFalse(Pairing.validSecret(secret))
    }
}
