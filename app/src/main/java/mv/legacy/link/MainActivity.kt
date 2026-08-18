package mv.legacy.link

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.net.URL
import java.util.concurrent.Executors

/**
 * The whole UI, in one screen and pure code (no XML layout needed):
 *   1. Grant notification access  (a button that deep-links to the toggle)
 *   2. Pair with Legacy           (paste address + secret, or scan the QR)
 *   3. Choose which apps to mirror
 *   4. See status + send a test
 *
 * Deliberately plain — this app has one job and should be obvious.
 */
class MainActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var hostField: EditText
    private lateinit var secretField: EditText
    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        val raw = result?.contents ?: return@registerForActivityResult
        val pair = QrScannerActivity.parse(raw)
        if (pair == null) {
            toast("That QR isn't a Legacy pairing code.")
        } else {
            QrScannerActivity.save(this, pair.first, pair.second)
            toast("Paired with ${pair.first}")
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        fun heading(t: String) = TextView(this).apply {
            text = t; textSize = 14f; setPadding(0, 40, 0, 12); alpha = 0.75f
        }.also { root.addView(it) }

        title = "Legacy Link"

        // ---- 1. Notification access ----
        heading("STEP 1 · NOTIFICATION ACCESS")
        val accessBtn = Button(this).apply {
            text = "Grant notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        root.addView(accessBtn)
        root.addView(TextView(this).apply {
            text = "Find “Legacy Link”, turn it on, confirm. Android needs this to see your notifications."
            textSize = 14f; alpha = 0.7f; setPadding(0, 6, 0, 0)
        })

        // ---- 2. Pairing ----
        heading("STEP 2 · PAIR WITH LEGACY")
        hostField = EditText(this).apply {
            hint = "http://legacy-pc:3000  (or the PC's 100.x Tailscale address)"
            setText(Prefs.host(this@MainActivity))
        }
        secretField = EditText(this).apply {
            hint = "secret from Legacy → Settings → Phone link"
            setText(Prefs.secret(this@MainActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        root.addView(hostField)
        root.addView(secretField)

        val scanBtn = Button(this).apply {
            text = "Scan pairing QR"
            setOnClickListener { launchQrScanner() }
        }
        val saveBtn = Button(this).apply {
            text = "Save & test connection"
            setOnClickListener { savePairing() }
        }
        root.addView(scanBtn)
        root.addView(saveBtn)

        // ---- 3. App picker ----
        heading("STEP 3 · WHICH APPS TO MIRROR")
        val pickBtn = Button(this).apply {
            text = "Choose apps"
            setOnClickListener { startActivity(Intent(this@MainActivity, AppPickerActivity::class.java)) }
        }
        root.addView(pickBtn)

        // ---- 4. Status + master switch ----
        heading("STATUS")
        val enabledSwitch = Switch(this).apply {
            text = "Mirroring on"
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on); refresh() }
        }
        root.addView(enabledSwitch)
        status = TextView(this).apply { textSize = 15f; setPadding(0, 16, 0, 0) }
        root.addView(status)

        refresh()
    }

    override fun onResume() {
        super.onResume()
        hostField.setText(Prefs.host(this))
        secretField.setText(Prefs.secret(this))
        refresh()
        io.execute { Sender.flush(applicationContext) }
    }

    private fun savePairing() {
        val host = hostField.text.toString().trim()
        val secret = secretField.text.toString().trim()
        if (host.isBlank() || secret.isBlank()) {
            toast("Fill in both the address and the secret."); return
        }
        val fixedHost = normalizeHost(host)
        if (fixedHost == null) {
            toast("Enter a valid http:// or https:// address.")
            return
        }
        Prefs.setPairing(this, fixedHost, secret)
        status.text = "Testing…"
        io.execute {
            val result = Sender.test(fixedHost, secret)
            runOnUiThread {
                status.text = when (result) {
                    "ok" -> "✓ Connected. Legacy heard the test."
                    "auth" -> "✗ Reached Legacy, but the secret is wrong. Re-scan the QR, or copy the secret from Legacy → Settings → Phone link."
                    "unreachable" -> "✗ Can't reach Legacy at $fixedHost.\n• Both devices on the SAME Tailscale account?\n• Use the http://100.x address Legacy prints (NOT the https one).\n• Is Legacy running, and is Node allowed through the PC firewall?"
                    else -> "✗ Legacy answered with an error ($result). Check the address and port (should be :3000)."
                }
            }
        }
    }

    private fun refresh() {
        val access = isAccessGranted()
        val paired = Prefs.isConfigured(this)
        val apps = Prefs.mirroredApps(this).size
        val queued = Prefs.queue(this).size
        status.text = buildString {
            append(if (access) "✓ Notification access granted\n" else "✗ Notification access NOT granted\n")
            append(if (paired) "✓ Paired with Legacy\n" else "✗ Not paired yet\n")
            append("• Mirroring $apps app(s)\n")
            if (queued > 0) append("• $queued event(s) queued (PC offline)\n")
            append(if (Prefs.enabled(this@MainActivity)) "• Mirroring is ON" else "• Mirroring is OFF")
        }
    }

    private fun isAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, LegacyNotificationListener::class.java).flattenToString()
        return flat.split(":").any { it.equals(me, ignoreCase = true) }
    }

    private fun launchQrScanner() {
        qrScanner.launch(ScanOptions().apply {
            setPrompt("Point at the pairing QR in Legacy's settings")
            setBeepEnabled(false)
            setOrientationLocked(true)
        })
    }

    private fun normalizeHost(input: String): String? {
        val withScheme = if (input.startsWith("http://", true) || input.startsWith("https://", true)) {
            input
        } else {
            "http://$input"
        }
        return runCatching {
            val url = URL(withScheme)
            if (url.protocol.lowercase() !in setOf("http", "https") || url.host.isBlank()) null
            else withScheme.trimEnd('/')
        }.getOrNull()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
