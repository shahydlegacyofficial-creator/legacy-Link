package mv.legacy.link

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors

/**
 * The one component that matters. Once the user grants Notification access,
 * Android calls onNotificationPosted for EVERY notification on the phone.
 * We keep only the apps the user chose, extract the human-readable title and
 * text, and hand it to Sender on a background thread.
 *
 * No polling, no wakelocks, no battery drain: this only runs when Android
 * has something to give us.
 */
class LegacyNotificationListener : NotificationListenerService() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onListenerConnected() {
        // Reconnected (app update, reboot, re-grant) — good moment to flush.
        io.execute { Sender.flush(applicationContext) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // never mirror our own

        val mirrored = Prefs.mirroredApps(applicationContext)
        if (pkg !in mirrored) return

        val extras = sbn.notification?.extras ?: return
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString().trim()
        // Prefer the big text when present (fuller message), fall back to the line.
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = (big ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()

        if (title.isBlank() && text.isBlank()) return
        // Skip ongoing/silent chrome (music controls, "running" services).
        if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val appLabel = appLabelFor(pkg)
        val event = Sender.buildEvent(pkg, appLabel, title, text)
        io.execute { Sender.sendOrQueue(applicationContext, event) }
    }

    private fun appLabelFor(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg.substringAfterLast('.')
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
