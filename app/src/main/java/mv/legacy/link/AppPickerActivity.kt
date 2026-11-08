package mv.legacy.link

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import mv.legacy.link.Design.add
import mv.legacy.link.Design.weight
import java.util.concurrent.Executors

class AppPickerActivity : LegacyActivity() {
    private data class Source(val pkg: String, val label: String, val icon: Drawable?, val installed: Boolean)
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private lateinit var allFilter: LinearLayout
    private lateinit var selectedFilter: LinearLayout
    private var sources = emptyList<Source>()
    private var chosen = mutableSetOf<String>()
    private var query = ""
    private var onlySelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chosen = Prefs.mirroredApps(this).toMutableSet()
        val root = shell("Apps") { next ->
            if (next != "Apps") {
                startActivity(Intent(this, MainActivity::class.java).putExtra("tab", next).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
        val header = Design.row(this)
        header.addView(Design.iconTile(this, "apps", Design.ink, 42))
        header.weight(Design.text(this, "Notification sources", 17f, bold = true))
        root.add(header)
        root.add(Design.text(this, "Only what\nyou choose.", 32f, bold = true).apply { setLineSpacing(0f, 1.12f) }, 30)
        root.add(Design.text(this, "Connect your banking apps and other essentials. Everything else stays on your phone.", 13f, Design.muted), 12)
        val summary = Design.card(this, 16)
        val summaryRow = Design.row(this)
        summaryRow.addView(Design.iconTile(this, "shield", Design.green, 40))
        summaryRow.weight(Design.column(this).apply {
            count = Design.text(context, selectionLabel(), 14f, bold = true)
            add(count)
            add(Design.text(context, "Changes save automatically.", 11f, Design.muted), 5)
        })
        summary.add(summaryRow)
        root.add(summary, 22)
        val search = Design.field(this, "Search apps or package names").apply {
            contentDescription = "Search apps"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString().trim(); renderList() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.add(search, 22)
        val filters = Design.row(this)
        allFilter = Design.button(this, "All apps", primary = false) { onlySelected = false; renderList() }
        selectedFilter = Design.button(this, "Selected", primary = false) { onlySelected = true; renderList() }
        filters.addView(allFilter, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = Design.dp(this@AppPickerActivity, 5) })
        filters.addView(selectedFilter, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Design.dp(this@AppPickerActivity, 5) })
        root.add(filters, 12)
        root.add(Design.eyebrow(this, "ON THIS PHONE"), 26)
        list = Design.column(this)
        list.add(Design.text(this, "Loading installed apps…", 13f, Design.muted), 14)
        root.add(list, 8)
        val manual = Design.card(this, 16)
        manual.add(Design.text(this, "Can't find an app?", 14f, bold = true))
        manual.add(Design.text(this, "Add its Android package name. This also supports apps without a launcher icon.", 11f, Design.muted), 8)
        val entry = Design.field(this, "com.example.banking").apply { contentDescription = "App package name" }
        manual.add(entry, 14)
        manual.add(Design.button(this, "Add app", "plus", false) {
            val pkg = entry.text.toString().trim()
            if (!Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+").matches(pkg) || pkg == packageName) {
                entry.error = "Enter an app package name such as com.example.banking"
            } else {
                chosen.add(pkg)
                Prefs.setMirroredApps(this, chosen)
                if (sources.none { it.pkg == pkg }) sources = sources + sourceFor(pkg)
                entry.text.clear()
                count.text = selectionLabel()
                renderList()
                Toast.makeText(this, "App added. Selection saved.", Toast.LENGTH_SHORT).show()
            }
        }, 12)
        root.add(manual, 24)
        root.add(Design.text(this, "Banking notifications can include balances, transactions and one-time codes. Select only what you want your assistant to receive.", 10f, Design.muted), 18)

        io.execute {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = runCatching { packageManager.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
                .mapNotNull { it.activityInfo?.packageName }.distinct().filter { it != packageName }
                .map(::sourceFor).sortedBy { it.label.lowercase() }
            val missing = chosen.filter { pkg -> apps.none { it.pkg == pkg } }.map(::sourceFor)
            runOnUiThread { if (!isDestroyed) { sources = apps + missing; renderList() } }
        }
    }

    private fun sourceFor(pkg: String): Source = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        Source(pkg, packageManager.getApplicationLabel(info).toString(), packageManager.getApplicationIcon(info), true)
    }.getOrElse { Source(pkg, pkg, null, false) }

    private fun selectionLabel() = "${chosen.size} ${if (chosen.size == 1) "app" else "apps"} selected"

    private fun renderList() {
        if (!::list.isInitialized) return
        allFilter.background = Design.shape(this, if (!onlySelected) Design.raised else Design.bg, 16, if (!onlySelected) Design.blue else Design.line)
        selectedFilter.background = Design.shape(this, if (onlySelected) Design.raised else Design.bg, 16, if (onlySelected) Design.blue else Design.line)
        allFilter.isSelected = !onlySelected
        selectedFilter.isSelected = onlySelected
        list.removeAllViews()
        val filtered = sources.filter { (!onlySelected || it.pkg in chosen) && (it.label.contains(query, true) || it.pkg.contains(query, true)) }
        if (filtered.isEmpty()) {
            val empty = Design.card(this)
            empty.add(Design.text(this, if (onlySelected && query.isBlank()) "Your sources start here" else "No matching apps", 16f, bold = true))
            empty.add(Design.text(this, if (onlySelected && query.isBlank()) "Select an app in All apps to share its notifications with Legacy." else "Try a different search, or add a package name below.", 12f, Design.muted), 8)
            list.add(empty, 8)
        }
        filtered.forEach { source ->
            val item = Design.row(this).apply {
                setPadding(Design.dp(context, 14), Design.dp(context, 12), Design.dp(context, 6), Design.dp(context, 12))
                background = Design.shape(context, Design.surface, 18)
            }
            if (source.icon != null) {
                item.addView(ImageView(this).apply { setImageDrawable(source.icon); importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(Design.dp(this, 38), Design.dp(this, 38)).apply { marginEnd = Design.dp(this@AppPickerActivity, 14) })
            } else item.addView(Design.iconTile(this, "apps", Design.muted, 38))
            item.weight(Design.column(this).apply {
                add(Design.text(context, source.label, 13f, bold = true).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END })
                add(Design.text(context, if (source.installed) source.pkg else "Not found on this phone", 9f, Design.muted).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }, 6)
            })
            val check = CheckBox(this).apply {
                isChecked = source.pkg in chosen
                buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(Design.blue, Design.muted))
                minimumWidth = Design.dp(context, 48)
                minimumHeight = Design.dp(context, 48)
                contentDescription = "Share notifications from ${source.label}"
                setOnCheckedChangeListener { _, on ->
                    if (on) chosen.add(source.pkg) else chosen.remove(source.pkg)
                    Prefs.setMirroredApps(this@AppPickerActivity, chosen)
                    count.text = selectionLabel()
                    if (onlySelected && !on) renderList()
                }
            }
            item.addView(check)
            Design.click(item) { check.isChecked = !check.isChecked }
            list.add(item, 8)
        }
    }
    override fun onDestroy() { io.shutdown(); super.onDestroy() }
}
