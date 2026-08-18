package mv.legacy.link

import android.content.Context

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

    fun host(c: Context): String = p(c).getString(K_HOST, "") ?: ""
    fun secret(c: Context): String = p(c).getString(K_SECRET, "") ?: ""
    fun enabled(c: Context): Boolean = p(c).getBoolean(K_ENABLED, true)

    fun setPairing(c: Context, host: String, secret: String) =
        p(c).edit().putString(K_HOST, host.trimEnd('/')).putString(K_SECRET, secret).apply()

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
