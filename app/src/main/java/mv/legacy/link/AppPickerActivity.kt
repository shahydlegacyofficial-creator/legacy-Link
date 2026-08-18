package mv.legacy.link

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

/**
 * Choose which apps to mirror — from the phone's OWN installed apps.
 *
 * We list every launchable (user-facing) app the phone actually has, so the
 * real BML, WhatsApp, etc. appear with their true package names. The list is
 * built on a background thread (querying packages can be slow) and rendered
 * only when ready, so the screen never freezes.
 *
 * Package visibility is limited to launchable apps. Nothing is uploaded — the
 * list stays on the phone, and only apps the user ticks are forwarded.
 */
class AppPickerActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Apps to mirror"

        val chosen = Prefs.mirroredApps(this).toMutableSet()

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 48)
        }
        setContentView(ScrollView(this).apply { addView(list) })

        list.addView(TextView(this).apply {
            text = "Tick the apps whose notifications Legacy should receive. Everything else stays on your phone."
            textSize = 15f; alpha = 0.75f; setPadding(0, 0, 0, 20)
        })
        val loading = TextView(this).apply {
            text = "Loading your apps…"
            textSize = 15f; alpha = 0.75f; setPadding(0, 12, 0, 12)
        }
        list.addView(loading)

        // ---- build the installed-app list off the UI thread ----
        io.execute {
            val pm = packageManager
            val launcher = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolved: List<ResolveInfo> = try {
                pm.queryIntentActivities(launcher, 0)
            } catch (e: Exception) {
                emptyList()
            }
            // package -> label, de-duplicated, excluding ourselves, sorted by name
            val apps = resolved
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == packageName) return@mapNotNull null
                    val label = ri.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                    pkg to label
                }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }

            // Any ticked packages that AREN'T launchable (rare) still deserve a
            // row so the user can untick them.
            val known = apps.map { it.first }.toSet()
            val extras = chosen.filter { it !in known }.map { it to it }

            runOnUiThread {
                list.removeView(loading)
                if (apps.isEmpty()) {
                    list.addView(TextView(this).apply {
                        text = "Couldn't read the app list. You can still add apps by package name below."
                        textSize = 15f; alpha = 0.75f; setPadding(0, 8, 0, 16)
                    })
                }
                for ((pkg, label) in apps + extras) {
                    val cb = CheckBox(this).apply {
                        text = "$label\n$pkg"
                        textSize = 15f
                        isChecked = pkg in chosen
                        setOnCheckedChangeListener { _, on ->
                            if (on) chosen.add(pkg) else chosen.remove(pkg)
                            Prefs.setMirroredApps(this@AppPickerActivity, chosen)
                        }
                    }
                    list.addView(cb)
                }
                addManualBox(list, chosen)
            }
        }
    }

    private fun addManualBox(list: LinearLayout, chosen: MutableSet<String>) {
        list.addView(TextView(this).apply {
            text = "Add another app by package name (optional):"
            textSize = 15f; alpha = 0.75f; setPadding(0, 28, 0, 8)
        })
        val entry = EditText(this).apply { hint = "e.g. com.example.app" }
        list.addView(entry)
        list.addView(Button(this).apply {
            text = "Add"
            setOnClickListener {
                val pkg = entry.text.toString().trim()
                if (pkg.contains(".")) {
                    chosen.add(pkg)
                    Prefs.setMirroredApps(this@AppPickerActivity, chosen)
                    entry.text.clear()
                    Toast.makeText(this@AppPickerActivity, "Added $pkg", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this@AppPickerActivity, "That doesn't look like a package name.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        list.addView(TextView(this).apply {
            text = "Tip: your ticks are saved instantly. Just press back when you're done."
            textSize = 14f; alpha = 0.65f; setPadding(0, 24, 0, 0)
        })
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
