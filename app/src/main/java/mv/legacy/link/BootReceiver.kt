package mv.legacy.link

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a reboot, flush anything the queue is still holding. The listener
 *  itself is re-bound by Android automatically once access is granted. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            Thread {
                try {
                    Sender.flush(context.applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
}
