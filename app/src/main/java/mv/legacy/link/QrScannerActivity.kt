package mv.legacy.link

import android.content.Context
import org.json.JSONObject

/**
 * Pairing by QR. Legacy's Settings shows a QR encoding:
 *   {"host":"http://legacy-pc:3000","secret":"..."}
 * We decode it straight into Prefs. Uses ZXing's embedded scanner so there's
 * no camera plumbing to maintain.
 */
object QrScannerActivity {
    /** Parse a scanned QR string into (host, secret), or null if it isn't ours. */
    fun parse(raw: String?): Pair<String, String>? {
        raw ?: return null
        return try {
            val o = JSONObject(raw)
            val host = o.optString("host").trim()
            val secret = o.optString("secret").trim()
            if (host.isBlank() || secret.isBlank() || secret.any { it == '\r' || it == '\n' }) {
                null
            } else {
                host to secret
            }
        } catch (e: Exception) {
            null
        }
    }

    fun save(c: Context, host: String, secret: String) =
        Prefs.setPairing(
            c,
            if (host.startsWith("http://", true) || host.startsWith("https://", true)) host else "http://$host",
            secret
        )
}
