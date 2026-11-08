package mv.legacy.link

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends one notification event to Legacy, and — importantly — flushes any
 * events that piled up while the PC was asleep or off-network. All network
 * work runs off the main thread (callers use a background executor).
 *
 * The transport is plain HTTP: safe because it only ever travels inside the
 * Tailscale WireGuard tunnel. The shared secret says WHO is posting.
 */
object Sender {

    private const val MAX_FIELD_LENGTH = 8192

    /** Build the JSON body for one notification. */
    fun buildEvent(pkg: String, appLabel: String, title: String, text: String): String =
        JSONObject().apply {
            put("app", pkg.take(MAX_FIELD_LENGTH))
            put("appLabel", appLabel.take(MAX_FIELD_LENGTH))
            put("title", title.take(MAX_FIELD_LENGTH))
            put("text", text.take(MAX_FIELD_LENGTH))
            put("at", System.currentTimeMillis())
        }.toString()

    /** POST one JSON body. Returns true on a 2xx from Legacy. */
    private fun postOne(host: String, secret: String, body: String): Boolean {
        if (Pairing.normalizeHost(host, BuildConfig.ALLOW_CLEARTEXT) == null) return false
        return try {
            val conn = (URL("$host/phone-notify").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Legacy-Secret", secret)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try to send `event`. On success, also drain the offline queue (oldest
     * first). On failure, add `event` to the queue for a later attempt.
     * Synchronized so a burst of notifications can't interleave the queue.
     */
    @Synchronized
    fun sendOrQueue(c: Context, event: String) {
        if (!Prefs.isConfigured(c) || !Prefs.enabled(c) || !Prefs.consent(c)) return
        val host = Prefs.host(c)
        val secret = Prefs.secret(c)

        if (postOne(host, secret, event)) {
            Prefs.recordDelivery(c)
            val q = Prefs.queue(c)
            if (q.isNotEmpty()) {
                val remaining = ArrayList<String>()
                var stillUp = true
                for (item in q) {
                    if (stillUp && Prefs.enabled(c) && Prefs.consent(c) && postOne(host, secret, item)) { Prefs.recordDelivery(c); continue }
                    stillUp = false
                    remaining.add(item)
                }
                Prefs.setQueue(c, remaining)
            }
        } else {
            val q = Prefs.queue(c)
            q.add(event)
            Prefs.setQueue(c, q)
        }
    }

    /** Try the queue alone (called on reconnect / boot / app open). */
    @Synchronized
    fun flush(c: Context) {
        if (!Prefs.isConfigured(c) || !Prefs.enabled(c) || !Prefs.consent(c)) return
        val q = Prefs.queue(c)
        if (q.isEmpty()) return
        val host = Prefs.host(c)
        val secret = Prefs.secret(c)
        val remaining = ArrayList<String>()
        var stillUp = true
        for (item in q) {
            if (stillUp && Prefs.enabled(c) && Prefs.consent(c) && postOne(host, secret, item)) { Prefs.recordDelivery(c); continue }
            stillUp = false
            remaining.add(item)
        }
        Prefs.setQueue(c, remaining)
    }

    /** A pairing test that says WHAT went wrong, not just pass/fail.
     *  Returns one of: "ok" | "unreachable" | "auth" | "http:<code>". */
    fun test(host: String, secret: String): String {
        if (Pairing.normalizeHost(host, BuildConfig.ALLOW_CLEARTEXT) == null) return "https_required"
        val h = host.trimEnd('/')
        // 1) Can we reach Legacy at all? (no secret needed for the ping)
        val reachable = try {
            val c = (URL("$h/phone-ping").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
            }
            val code = c.responseCode; c.disconnect(); code in 200..299
        } catch (e: Exception) { false }
        if (!reachable) return "unreachable"

        // 2) Reachable — now check the secret by posting a real test event.
        val body = buildEvent("mv.legacy.link", "Legacy Link", "Pairing test", "Legacy Link is connected.")
        return try {
            val conn = (URL("$h/phone-notify").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 6000; readTimeout = 6000; doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Legacy-Secret", secret)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode; conn.disconnect()
            when { code in 200..299 -> "ok"; code == 401 -> "auth"; else -> "http:$code" }
        } catch (e: Exception) { "unreachable" }
    }

    @Synchronized
    fun clearLocalData(c: Context) = Prefs.clearLocalData(c)
}
