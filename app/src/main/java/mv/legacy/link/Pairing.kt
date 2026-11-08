package mv.legacy.link

import java.net.URI

/** Shared validation for typed addresses and QR codes. */
object Pairing {
    fun normalizeHost(input: String, allowCleartext: Boolean = true): String? {
        val raw = input.trim()
        if (raw.isBlank() || raw.any { it.isWhitespace() || it.isISOControl() }) return null
        val candidate = if (raw.contains("://")) raw else "${if (allowCleartext) "http" else "https"}://$raw"
        return runCatching {
            val uri = URI(candidate)
            if ((!allowCleartext && !uri.scheme.equals("https", true)) || uri.scheme.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
                uri.port !in -1..65535 || uri.port == 0) null
            else candidate.trimEnd('/')
        }.getOrNull()
    }
    fun validSecret(value: String) = value.isNotBlank() && value.all { it.code in 32..126 }
}
