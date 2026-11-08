package mv.legacy.link

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import mv.legacy.link.Design.add
import mv.legacy.link.Design.weight
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : LegacyActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private var tab = "Overview"
    private var hostField: EditText? = null
    private var secretField: EditText? = null
    private var draftHost = ""
    private var draftSecret = ""
    private var testing = false
    private var renderedTab = ""
    private var resultView: TextView? = null
    private val changes = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (tab == "Overview" && key in setOf("queue", "last_delivery")) {
            runOnUiThread { if (!isDestroyed && !isFinishing && tab == "Overview") render() }
        }
    }
    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        val raw = result?.contents ?: return@registerForActivityResult
        val pair = QrScannerActivity.parse(raw)
        if (pair == null) {
            toast("This isn't a valid Legacy pairing code. Open Phone link in your assistant's settings.")
        } else {
            draftHost = pair.first
            draftSecret = pair.second
            hostField?.setText(draftHost)
            secretField?.setText(draftSecret)
            saveAndTest()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getString("tab") ?: intent.getStringExtra("tab") ?: "Overview"
        draftHost = savedInstanceState?.getString("host") ?: Prefs.host(this)
        // Secrets stay in memory or private preferences, never saved-instance bundles.
        draftSecret = Prefs.secret(this)
    }

    override fun onResume() {
        super.onResume()
        render()
        io.execute { Sender.flush(applicationContext) }
    }

    override fun onStart() { super.onStart(); Prefs.observe(this, changes) }
    override fun onStop() { Prefs.unobserve(this, changes); super.onStop() }

    override fun onSaveInstanceState(outState: Bundle) {
        captureDraft()
        outState.putString("tab", tab)
        outState.putString("host", draftHost)
        super.onSaveInstanceState(outState)
    }

    private fun captureDraft() {
        hostField?.let { draftHost = it.text.toString() }
        secretField?.let { draftSecret = it.text.toString() }
    }

    private fun navigate(next: String) {
        captureDraft()
        if (next == "Apps") startActivity(Intent(this, AppPickerActivity::class.java))
        else { tab = next; render() }
    }

    private fun render() {
        val scrollY = if (renderedTab == tab) pageScroll?.scrollY ?: 0 else 0
        captureDraft()
        hostField = null
        secretField = null
        resultView = null
        val content = shell(tab, ::navigate)
        header(content)
        if (tab == "Settings") settings(content) else overview(content)
        renderedTab = tab
        pageScroll?.post { pageScroll?.scrollTo(0, scrollY) }
    }

    private fun header(root: LinearLayout) {
        val header = Design.row(this)
        val mark = Design.iconTile(this, "brand", Design.ink, 42)
        mark.background = Design.shape(this, Design.raised, 13, Design.line)
        header.addView(mark)
        header.weight(Design.column(this).apply {
            add(Design.text(context, "Legacy Link", 18f, bold = true))
            add(Design.text(context, "BY LEGACY STUDIOS", 8f, Design.muted, true).apply { letterSpacing = .19f }, 5)
        })
        val privacy = FrameLayout(this).apply {
            addView(Design.icon(context, "shield", Design.muted), FrameLayout.LayoutParams(Design.dp(context, 23), Design.dp(context, 23), Gravity.CENTER))
            contentDescription = "Privacy and data sharing"
            Design.click(this) { showPrivacy() }
        }
        header.addView(privacy, LinearLayout.LayoutParams(Design.dp(this, 48), Design.dp(this, 48)))
        root.add(header)
    }

    private fun overview(root: LinearLayout) {
        root.add(Design.text(this, "Your phone.\nConnected to Legacy.", 29f, bold = true).apply { setLineSpacing(0f, 1.1f) }, 24)
        root.add(Design.text(this, "Banking alerts. Selected apps. One direct link.", 12f, Design.muted), 10)
        val paired = Prefs.isConfigured(this)
        val access = isAccessGranted()
        val apps = Prefs.mirroredApps(this).size
        val enabled = Prefs.enabled(this)
        val checkFailed = Prefs.connectionCheck(this).let { it.isNotBlank() && it != "ok" }
        val ready = paired && access && Prefs.consent(this) && apps > 0 && enabled
        val connection = Design.card(this)
        val connectionHeader = Design.row(this)
        connectionHeader.weight(Design.eyebrow(this, "ASSISTANT CONNECTION"))
        val state = when { !enabled -> "PAUSED"; !paired -> "NOT PAIRED"; checkFailed -> "CHECK LINK"; ready -> "READY"; else -> "FINISH SETUP" }
        connectionHeader.addView(Design.text(this, "●  $state", 8f, if (ready && !checkFailed) Design.green else Design.amber, true).apply { letterSpacing = .06f })
        connection.add(connectionHeader)
        val bridge = Design.row(this).apply { gravity = Gravity.CENTER; setPadding(0, Design.dp(context, 20), 0, Design.dp(context, 16)) }
        fun endpoint(glyph: String, label: String) = Design.column(this).apply {
            gravity = Gravity.CENTER
            val tile = FrameLayout(context).apply {
                background = Design.shape(context, Design.raised, 20, Design.line)
                addView(Design.icon(context, glyph, Design.ink, 30), FrameLayout.LayoutParams(Design.dp(context, 30), Design.dp(context, 30), Gravity.CENTER))
            }
            addView(tile, LinearLayout.LayoutParams(Design.dp(context, 56), Design.dp(context, 56)))
            addView(Design.text(context, label, 10f, Design.muted), LinearLayout.LayoutParams(-2, -2).apply { topMargin = Design.dp(context, 10) })
        }
        bridge.addView(endpoint("phone", "This phone"))
        val wire = Design.row(this).apply {
            gravity = Gravity.CENTER
            setPadding(Design.dp(context, 12), 0, Design.dp(context, 12), Design.dp(context, 23))
            addView(View(context).apply { setBackgroundColor(Design.line) }, LinearLayout.LayoutParams(0, Design.dp(context, 1), 1f))
            addView(Design.icon(context, "link", if (paired) Design.blue else Design.muted, 22), LinearLayout.LayoutParams(Design.dp(context, 36), Design.dp(context, 22)))
            addView(View(context).apply { setBackgroundColor(Design.line) }, LinearLayout.LayoutParams(0, Design.dp(context, 1), 1f))
        }
        bridge.addView(wire, LinearLayout.LayoutParams(0, -2, 1f))
        bridge.addView(endpoint("brand", "Legacy AI"))
        connection.add(bridge)
        connection.add(Design.text(this, when { !enabled -> "Your link is paused"; !paired -> "Connect your Legacy assistant"; checkFailed -> "Check your assistant connection"; ready -> "Ready for your notifications"; else -> "Complete your link" }, 18f, bold = true))
        connection.add(Design.text(this, when {
            !enabled -> "Forwarding and queued delivery are paused."
            !paired -> "Pair once. Send the notifications you choose directly to your assistant."
            checkFailed -> "The last connection test failed. Open settings to check your pairing."
            !access -> "Allow notification access to start your link."
            !Prefs.consent(this) -> "Review data sharing before forwarding notifications."
            apps == 0 -> "Choose your banking apps and other sources."
            else -> "Selected apps can forward to your saved assistant."
        }, 12f, Design.muted), 8)
        connection.add(Design.button(this, if (paired) "Connection settings" else "Pair your assistant", if (paired) "settings" else "qr") { navigate("Settings") }, 16)
        if (paired) connection.add(Design.text(this, checkSummary(), 10f, Design.muted), 12)
        root.add(connection, 20)

        root.add(Design.eyebrow(this, "YOUR LINK"), 22)
        val forwarding = Design.card(this, 14)
        val forwardRow = Design.row(this)
        forwardRow.addView(Design.iconTile(this, if (enabled) "link" else "pause"))
        forwardRow.weight(Design.column(this).apply {
            add(Design.text(context, "Notification forwarding", 13f, bold = true))
            add(Design.text(context, if (enabled) { if (ready) "Ready" else "On · waiting for setup" } else "Paused by you", 11f, Design.muted), 5)
        })
        forwardRow.addView(Design.toggle(this, enabled).apply {
            contentDescription = "Notification forwarding"
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on); render(); if (on) io.execute { Sender.flush(applicationContext) } }
        })
        forwarding.add(forwardRow)
        root.add(forwarding, 12)
        val metrics = Design.row(this)
        fun metric(number: Int, title: String, glyph: String, action: () -> Unit) = Design.card(this, 16).apply {
            val top = Design.row(context)
            top.weight(Design.text(context, number.toString().padStart(2, '0'), 29f, bold = true))
            top.addView(Design.icon(context, glyph, Design.muted, 20))
            add(top)
            add(Design.text(context, title, 11f, Design.muted), 9)
            Design.click(this, action)
        }
        metrics.addView(metric(apps, "Selected apps", "apps") { navigate("Apps") }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = Design.dp(this@MainActivity, 6) })
        metrics.addView(metric(Prefs.queue(this).size, "Waiting to send", "clock") { showQueue() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Design.dp(this@MainActivity, 6) })
        root.add(metrics, 12)
        val permission = Design.card(this, 16)
        val permissionRow = Design.row(this)
        val approved = access && Prefs.consent(this)
        permissionRow.addView(Design.iconTile(this, if (approved) "check" else "bell", if (approved) Design.green else Design.amber))
        permissionRow.weight(Design.column(this).apply {
            add(Design.text(context, if (approved) "Notification access allowed" else if (access) "Review data sharing" else "Allow notification access", 12f, bold = true))
            add(Design.text(context, if (approved) "You control which apps are shared." else "Review what reaches your assistant.", 10f, Design.muted), 6)
        })
        permissionRow.addView(Design.icon(this, "chevron", Design.muted, 17))
        permission.add(permissionRow)
        Design.click(permission) { grantAccess() }
        root.add(permission, 12)
        root.add(Design.eyebrow(this, "LATEST DELIVERY"), 26)
        val delivery = Design.row(this)
        delivery.addView(Design.iconTile(this, "activity", Design.muted, 40))
        delivery.weight(Design.column(this).apply {
            val at = Prefs.lastDelivery(context)
            add(Design.text(context, if (at == 0L) "Your next notification starts here" else "Notification delivered to Legacy", 12f, bold = true))
            add(Design.text(context, if (at == 0L) "No notifications forwarded yet." else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at)), 10f, Design.muted), 6)
        })
        root.add(delivery, 14)
        root.add(Design.text(this, "LEGACY AI ECOSYSTEM", 8f, Design.muted, true).apply { gravity = Gravity.CENTER; letterSpacing = .23f }, 32)
    }

    private fun settings(root: LinearLayout) {
        root.add(Design.text(this, "Connection settings", 27f, bold = true), 30)
        root.add(Design.text(this, "A direct line to your Legacy assistant.", 13f, Design.muted), 10)
        val scanner = Design.card(this)
        scanner.add(Design.eyebrow(this, "QUICK PAIR"))
        scanner.add(Design.text(this, "One scan. You're linked.", 20f, bold = true), 14)
        scanner.add(Design.text(this, "On your computer, open Legacy → Settings → Phone link, then scan the pairing QR code.", 12f, Design.muted), 9)
        scanner.add(Design.button(this, "Scan pairing QR", "qr") { launchQrScanner() }, 20)
        root.add(scanner, 24)
        root.add(Design.eyebrow(this, "OR CONNECT MANUALLY"), 28)
        val form = Design.card(this)
        form.add(Design.text(this, "Assistant address", 12f, bold = true))
        hostField = Design.field(this, if (BuildConfig.ALLOW_CLEARTEXT) "http://100.x.x.x:3000" else "https://your-assistant.example").apply { setText(draftHost); contentDescription = "Assistant address"; isEnabled = !testing }
        form.add(hostField!!, 10)
        form.add(Design.text(this, if (BuildConfig.ALLOW_CLEARTEXT) "Use the address shown in Legacy's Phone link settings." else "Use your assistant's HTTPS address with a valid certificate.", 10f, Design.muted), 8)
        val secretLabel = Design.row(this)
        secretLabel.weight(Design.text(this, "Pairing secret", 12f, bold = true))
        val showSecret = Design.text(this, "Show", 11f, Design.blue, true).apply {
            gravity = Gravity.CENTER
            minimumWidth = Design.dp(context, 48)
            minimumHeight = Design.dp(context, 48)
            contentDescription = "Show pairing secret"
        }
        Design.click(showSecret) {
            secretField?.let { field ->
                val reveal = field.transformationMethod != null
                field.transformationMethod = if (reveal) null else PasswordTransformationMethod.getInstance()
                field.setSelection(field.text.length)
                showSecret.text = if (reveal) "Hide" else "Show"
                showSecret.contentDescription = "${if (reveal) "Hide" else "Show"} pairing secret"
            }
        }
        secretLabel.addView(showSecret)
        form.add(secretLabel, 10)
        secretField = Design.field(this, "Paste your pairing secret", true).apply { setText(draftSecret); contentDescription = "Pairing secret"; isEnabled = !testing }
        form.add(secretField!!)
        form.add(Design.button(this, if (testing) "Testing connection…" else "Save & test connection", "link", false) { saveAndTest() }.apply { isEnabled = !testing; alpha = if (testing) .5f else 1f }, 20)
        resultView = Design.text(this, if (testing) "Checking your assistant…" else checkDetails(), 11f, Design.muted).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        form.add(resultView!!, 14)
        root.add(form, 12)
        val access = Design.card(this, 16)
        access.add(Design.text(this, "Notification access", 13f, bold = true))
        access.add(Design.text(this, if (isAccessGranted()) "Allowed. Only selected apps are forwarded." else "Allow Legacy Link to read notifications. Then choose the apps you want to share.", 11f, Design.muted), 8)
        access.add(Design.button(this, "Open Android permissions", "bell", false) { grantAccess() }, 16)
        access.add(Design.button(this, "Notification access blocked?", primary = false) { showAccessHelp() }, 12)
        root.add(access, 20)
        root.add(Design.button(this, "Privacy & data sharing", "shield", false) { showPrivacy() }, 20)
        root.add(Design.button(this, "Privacy policy", "shield", false) { openPolicy() }, 12)
        root.add(Design.button(this, "Clear local data", primary = false) { confirmClearData() }, 12)
        root.add(Design.text(this, "Legacy Link ${BuildConfig.VERSION_NAME} · ${if (BuildConfig.ALLOW_CLEARTEXT) "Personal" else "Google Play"}\nMade by Legacy Studios", 10f, Design.muted).apply { gravity = Gravity.CENTER }, 28)
    }

    private fun saveAndTest() {
        if (testing) return
        captureDraft()
        val host = Pairing.normalizeHost(draftHost, BuildConfig.ALLOW_CLEARTEXT)
        if (host == null) { hostField?.error = if (BuildConfig.ALLOW_CLEARTEXT) "Enter a valid http:// or https:// address." else "The Google Play build requires an HTTPS address."; toast("Enter a valid ${if (BuildConfig.ALLOW_CLEARTEXT) "assistant" else "HTTPS assistant"} address."); return }
        val secret = draftSecret.trim()
        if (!Pairing.validSecret(secret)) { secretField?.error = "Paste the secret from Phone link settings."; toast("Enter a valid pairing secret."); return }
        draftHost = host
        draftSecret = secret
        // Update the fields so render's draft capture preserves the normalized values.
        hostField?.setText(host)
        secretField?.setText(secret)
        Prefs.setPairing(this, host, secret)
        testing = true
        tab = "Settings"
        render()
        io.execute {
            val result = Sender.test(host, secret)
            Prefs.setConnectionCheck(applicationContext, result)
            if (result == "ok") Sender.flush(applicationContext)
            runOnUiThread { testing = false; if (!isFinishing && !isDestroyed) render() }
        }
    }

    private fun checkSummary(): String = when (Prefs.connectionCheck(this)) {
        "ok" -> "Last test passed · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(Prefs.lastCheck(this)))}"
        "auth" -> "Pairing secret needs attention"
        "unreachable" -> "Last test couldn't reach your assistant"
        "" -> "Saved pairing · connection not tested yet"
        else -> "Last connection test failed"
    }
    private fun checkDetails(): String = when (Prefs.connectionCheck(this)) {
        "ok" -> "Connection verified. Legacy received your test. ${if (Prefs.mirroredApps(this).isEmpty()) "Choose apps in the Apps tab to start forwarding." else "Your selected apps are ready."}"
        "auth" -> "Legacy was reached, but the secret was rejected. Copy a fresh secret from Settings → Phone link."
        "unreachable" -> if (BuildConfig.ALLOW_CLEARTEXT) "Couldn't reach Legacy. Check the address and port, keep Legacy running, and connect both devices to the same private network." else "Couldn't reach Legacy. Check the HTTPS address, certificate and network, and keep your assistant receiver running."
        "https_required" -> "The Google Play build requires HTTPS. Enter your assistant's HTTPS address."
        "" -> "Saving sends one test notification to your assistant."
        else -> "Your assistant returned ${Prefs.connectionCheck(this)}. Check the receiver address and port."
    }
    private fun launchQrScanner() {
        qrScanner.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan the QR in Legacy → Settings → Phone link")
            setBeepEnabled(false)
            setOrientationLocked(false)
        })
    }
    private fun isAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, LegacyNotificationListener::class.java)
        return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
    }
    private fun grantAccess() {
        if (Prefs.consent(this)) {
            openNotificationSettings()
            return
        }
        AlertDialog.Builder(this).setTitle("Share selected notifications with Legacy")
            .setMessage("Legacy Link reads notifications from apps you choose and sends their app name, title, text and time to the assistant address you configure, including while this app is closed or not in use.\n\nNotifications can include financial balances, transactions, private messages and one-time codes. Your assistant may retain or process them using its own services.\n\nUp to 200 undelivered notifications are saved on this phone until sent, replaced by newer entries, or cleared. Installed app names are used locally to help you choose sources.\n\nYou can pause forwarding, deselect apps, revoke Android notification access or clear local data in Settings. No source is selected by default.")
            .setPositiveButton("I agree") { _, _ ->
                Prefs.setConsent(this, true)
                openNotificationSettings()
            }
            .setNegativeButton("Not now", null)
            .setNeutralButton("Privacy policy") { _, _ -> openPolicy() }.show()
    }
    private fun openNotificationSettings() {
        val intent = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, LegacyNotificationListener::class.java).flattenToString()
            )
        } else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
    private fun showAccessHelp() {
        AlertDialog.Builder(this).setTitle("Notification access blocked?")
            .setMessage("Android may restrict notification access for an APK installed from a browser, file manager or messaging app. This is controlled by Android.\n\nIf you trust this Legacy Link build:\n1. Open App info below.\n2. Tap the three-dot menu and Allow restricted settings, if shown.\n3. Confirm with your device lock if Android asks.\n4. Return here, open Android permissions and enable Legacy Link.\n\nMenu names vary by phone. If the option is unavailable or your phone is managed, check with your device administrator. Legacy Link cannot grant this access itself.")
            .setPositiveButton("Open App info") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Close", null).show()
    }
    private fun showQueue() {
        AlertDialog.Builder(this).setTitle("Waiting to send")
            .setMessage("${Prefs.queue(this).size} notifications are saved on this phone. Legacy Link retries when the app opens, the listener reconnects, or another notification arrives. Up to 200 can be kept. Pausing forwarding also pauses queued delivery.")
            .setPositiveButton("Got it", null).show()
    }
    private fun showPrivacy() {
        AlertDialog.Builder(this).setTitle("Your link. Your choice.")
            .setMessage("Only notifications from apps you select are forwarded after you agree to data sharing and grant Android notification access. They may include balances, transactions, messages or one-time codes.\n\n${if (BuildConfig.ALLOW_CLEARTEXT) "Use HTTPS or an encrypted private Tailscale network. HTTP alone is not encrypted." else "The Google Play build sends to HTTPS addresses only."}\n\nUp to 200 undelivered notifications stay on this phone. You can erase the queue, pairing and settings with Clear local data. Copies already sent are controlled by your assistant.\n\nNo ads or analytics. Android backup and device transfer of app data are disabled.\n\nPrivacy contact: legacystudiosofficial@gmail.com")
            .setPositiveButton("Close", null).setNeutralButton("Privacy policy") { _, _ -> openPolicy() }.show()
    }
    private fun openPolicy() {
        val url = "https://shahydlegacyofficial-creator.github.io/legacy-Link/privacy.html"
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast("Privacy policy: $url\nContact legacystudiosofficial@gmail.com") }
    }
    private fun confirmClearData() {
        AlertDialog.Builder(this).setTitle("Clear local data?")
            .setMessage("This removes your pairing secret, selected apps, waiting notifications and local delivery status. Forwarding will stop. Copies already sent to your assistant are not removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear local data") { _, _ ->
                Prefs.setConsent(this, false)
                Prefs.setEnabled(this, false)
                io.execute {
                    Sender.clearLocalData(applicationContext)
                    runOnUiThread {
                        if (!isDestroyed) {
                            hostField = null; secretField = null
                            draftHost = ""; draftSecret = ""
                            render(); toast("Local data cleared. Forwarding is paused.")
                        }
                    }
                }
            }.show()
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    override fun onDestroy() { io.shutdown(); super.onDestroy() }
}
