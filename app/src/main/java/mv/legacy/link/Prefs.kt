package mv.legacy.link

import android.content.Context
import android.content.SharedPreferences

/**
 * Everything the app remembers: where Legacy lives, the shared secret, which
 * apps to mirror, and the offline queue. Plain SharedPreferences — no
 * database, nothing to go wrong.
 */
object Prefs {
    private const val FILE = "legacy_link"
    private const val K_HOST = "host"        // Legacy PC address, e.g. http://legacy-pc:3000
    private const val K_SECRET = "secret"    // X-Legacy-Secret
    private const val K_APPS = "apps"        // packages to mirror, newline-joined
    private const val K_QUEUE = "queue"      // offline events, one JSON per line
    private const val K_ENABLED = "enabled"

    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun observe(c: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).registerOnSharedPreferenceChangeListener(listener)
    fun unobserve(c: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).unregisterOnSharedPreferenceChangeListener(listener)

    fun host(c: Context): String = p(c).getString(K_HOST, "") ?: ""
    fun secret(c: Context): String = p(c).getString(K_SECRET, "") ?: ""
    fun enabled(c: Context): Boolean = p(c).getBoolean(K_ENABLED, true)
    fun consent(c: Context): Boolean = p(c).getBoolean("privacy_consent_v1", false)
    fun setConsent(c: Context, accepted: Boolean) = p(c).edit().putBoolean("privacy_consent_v1", accepted).apply()
    fun clearLocalData(c: Context) = p(c).edit().clear().putBoolean(K_ENABLED, false).apply()

    fun setPairing(c: Context, host: String, secret: String) =
        p(c).edit().putString(K_HOST, host.trimEnd('/')).putString(K_SECRET, secret)
            .remove("connection_check").remove("last_check").apply()

    fun connectionCheck(c: Context): String = p(c).getString("connection_check", "") ?: ""
    fun lastCheck(c: Context) = p(c).getLong("last_check", 0L)
    fun setConnectionCheck(c: Context, result: String) = p(c).edit()
        .putString("connection_check", result).putLong("last_check", System.currentTimeMillis()).apply()
    fun lastDelivery(c: Context) = p(c).getLong("last_delivery", 0L)
    fun recordDelivery(c: Context) = p(c).edit().putLong("last_delivery", System.currentTimeMillis()).apply()

    fun setEnabled(c: Context, on: Boolean) = p(c).edit().putBoolean(K_ENABLED, on).apply()

    fun isConfigured(c: Context) = host(c).isNotBlank() && secret(c).isNotBlank()

    /** Apps the user explicitly selected. The privacy-safe default is none. */
    fun mirroredApps(c: Context): Set<String> {
        val raw = p(c).getString(K_APPS, null) ?: return emptySet()
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }
    fun setMirroredApps(c: Context, apps: Set<String>) =
        p(c).edit().putString(K_APPS, apps.joinToString("\n")).apply()

    // Offline queue — events captured while the PC was unreachable.
    fun queue(c: Context): MutableList<String> =
        (p(c).getString(K_QUEUE, "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
    fun setQueue(c: Context, items: List<String>) =
        p(c).edit().putString(K_QUEUE, items.takeLast(200).joinToString("\n")).apply()
}
