package mv.legacy.link

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Small, native view primitives shared by the three companion screens. */
object Design {
    val bg = Color.rgb(11, 14, 19)
    val surface = Color.rgb(20, 25, 34)
    val raised = Color.rgb(28, 35, 47)
    val line = Color.rgb(42, 52, 68)
    val ink = Color.rgb(241, 244, 248)
    val muted = Color.rgb(160, 173, 192)
    val blue = Color.rgb(174, 205, 247)
    val green = Color.rgb(157, 219, 190)
    val amber = Color.rgb(235, 204, 151)
    val red = Color.rgb(246, 169, 170)

    fun dp(c: Context, n: Int) = (n * c.resources.displayMetrics.density).toInt()
    fun shape(c: Context, color: Int = surface, radius: Int = 22, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(c, radius).toFloat()
        stroke?.let { setStroke(dp(c, 1), it) }
    }

    fun text(c: Context, value: String, size: Float = 14f, color: Int = ink, bold: Boolean = false) = TextView(c).apply {
        text = value
        textSize = size
        setTextColor(color)
        val font = ResourcesCompat.getFont(c, R.font.manrope)
        typeface = if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(font, if (bold) 650 else 400, false)
            else Typeface.create(font, if (bold) Typeface.BOLD else Typeface.NORMAL)
        fontVariationSettings = if (bold) "'wght' 650" else "'wght' 450"
        includeFontPadding = false
        setLineSpacing(dp(c, 3).toFloat(), 1f)
    }

    fun column(c: Context, padding: Int = 0) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, padding), dp(c, padding), dp(c, padding), dp(c, padding))
    }
    fun row(c: Context) = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    fun space(c: Context, height: Int) = View(c).apply { layoutParams = LinearLayout.LayoutParams(1, dp(c, height)) }
    fun LinearLayout.add(view: View, top: Int = 0, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (height > 0) dp(context, height) else height).apply { topMargin = dp(context, top) })
    }
    fun LinearLayout.weight(view: View) { addView(view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
    fun card(c: Context, padding: Int = 20) = column(c, padding).apply { background = shape(c, surface, 24, line) }
    fun eyebrow(c: Context, value: String) = text(c, value, 10f, muted, true).apply { letterSpacing = .15f }
    fun icon(c: Context, name: String, color: Int = blue, size: Int = 24) = Glyph(c, name, color).apply {
        layoutParams = LinearLayout.LayoutParams(dp(c, size), dp(c, size))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    fun iconTile(c: Context, name: String, color: Int = blue, size: Int = 44) = FrameLayout(c).apply {
        background = shape(c, raised, 14)
        addView(icon(c, name, color), FrameLayout.LayoutParams(dp(c, 23), dp(c, 23), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(c, size), dp(c, size)).apply { marginEnd = dp(c, 14) }
    }
    fun click(view: View, action: () -> Unit) {
        view.isClickable = true
        view.isFocusable = true
        view.foreground = RippleDrawable(ColorStateList.valueOf(0x22AECDF7), null, shape(view.context, Color.WHITE))
        view.setOnClickListener { action() }
    }
    fun button(c: Context, title: String, glyph: String? = null, primary: Boolean = true, action: () -> Unit): LinearLayout = row(c).apply {
        gravity = Gravity.CENTER
        minimumHeight = dp(c, 54)
        setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12))
        background = shape(c, if (primary) blue else raised, 16, if (primary) null else line)
        glyph?.let { addView(icon(c, it, if (primary) bg else ink, 20), LinearLayout.LayoutParams(dp(c, 20), dp(c, 20)).apply { marginEnd = dp(c, 10) }) }
        addView(text(c, title, 14f, if (primary) bg else ink, true))
        contentDescription = title
        click(this, action)
    }
    fun field(c: Context, placeholder: String, secret: Boolean = false) = EditText(c).apply {
        hint = placeholder
        textSize = 14f
        setTextColor(ink)
        setHintTextColor(muted)
        setSingleLine(true)
        inputType = if (secret) android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        typeface = ResourcesCompat.getFont(c, R.font.manrope)
        fontVariationSettings = "'wght' 450"
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        background = shape(c, bg, 14, line)
        setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16))
        minimumHeight = dp(c, 54)
    }
    fun toggle(c: Context, checked: Boolean) = androidx.appcompat.widget.SwitchCompat(c).apply {
        isChecked = checked
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(blue, muted))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(0xFF3B526D.toInt(), line))
        minimumWidth = dp(c, 52)
        minimumHeight = dp(c, 48)
    }
    fun nav(c: Context, selected: String, onSelect: (String) -> Unit) = row(c).apply {
        background = shape(c, surface, 0)
        setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8))
        listOf("Overview" to "overview", "Apps" to "apps", "Settings" to "settings").forEach { (title, glyph) ->
            val active = selected == title
            val tab = column(c).apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(c, 58)
                if (active) background = shape(c, raised, 17)
                addView(icon(c, glyph, if (active) blue else muted, 22))
                addView(text(c, title, 10f, if (active) blue else muted, active), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(c, 5) })
                contentDescription = title
                isSelected = active
                click(this) { onSelect(title) }
            }
            addView(tab, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(c, 4); marginEnd = dp(c, 4) })
        }
    }
}

abstract class LegacyActivity : AppCompatActivity() {
    protected var pageScroll: ScrollView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    protected fun shell(selected: String, onSelect: (String) -> Unit): LinearLayout {
        val root = Design.column(this).apply { setBackgroundColor(Design.bg) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        pageScroll = scroll
        val content = Design.column(this, 24)
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Design.nav(this, selected, onSelect))
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        return content
    }
}

/** Original vector geometry: the Legacy wordmark's angular A joined to an L. */
class Glyph @JvmOverloads constructor(c: Context, private val name: String = "brand", private val tint: Int = Design.blue) : View(c) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePath = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val side = minOf(width, height).toFloat()
        canvas.translate((width - side) / 2, (height - side) / 2)
        canvas.scale(side / 24f, side / 24f)
        val p = paint.apply { color = tint; style = Paint.Style.STROKE; strokeWidth = 1.55f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        fun path(vararg points: Float) {
            strokePath.rewind()
            strokePath.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) strokePath.lineTo(points[i], points[i + 1])
            canvas.drawPath(strokePath, p)
        }
        fun box(l: Float, t: Float, r: Float, b: Float, radius: Float = 2f) { canvas.drawRoundRect(l, t, r, b, radius, radius, p) }
        when (name) {
            "brand" -> {
                canvas.save(); canvas.scale(24f / 60f, 24f / 60f); canvas.translate(-25f, -24f)
                p.style = Paint.Style.FILL
                path(28f, 30f, 35f, 30f, 35f, 71f, 53f, 71f, 49.5f, 78f, 28f, 78f)
                path(42f, 63f, 60f, 29f, 83f, 78f, 75f, 78f, 60f, 45f, 49f, 66f)
                p.color = Design.blue
                path(46f, 71f, 66f, 71f, 69.3f, 78f, 42.3f, 78f)
                canvas.restore()
            }
            "phone" -> { box(6f, 2f, 18f, 22f, 3f); path(10f, 18.5f, 14f, 18.5f) }
            "overview" -> { box(3f, 3f, 10f, 13f); box(14f, 3f, 21f, 9f); box(3f, 17f, 10f, 21f); box(14f, 13f, 21f, 21f) }
            "apps" -> { box(3f, 3f, 9f, 9f); box(15f, 3f, 21f, 9f); box(3f, 15f, 9f, 21f); box(15f, 15f, 21f, 21f) }
            "settings" -> { path(4f, 6f, 20f, 6f); path(4f, 12f, 20f, 12f); path(4f, 18f, 20f, 18f); p.style = Paint.Style.FILL; canvas.drawCircle(9f, 6f, 2.4f, p); canvas.drawCircle(16f, 12f, 2.4f, p); canvas.drawCircle(9f, 18f, 2.4f, p) }
            "chevron" -> path(9f, 6f, 15f, 12f, 9f, 18f)
            "check" -> path(5f, 12f, 10f, 17f, 20f, 7f)
            "shield" -> { path(12f, 2f, 21f, 6f, 20f, 15f, 17f, 19f, 12f, 22f, 7f, 19f, 4f, 15f, 3f, 6f, 12f, 2f); path(8f, 12f, 11f, 15f, 16f, 9f) }
            "bell" -> { canvas.drawArc(6f, 3f, 18f, 17f, 180f, 180f, false, p); path(6f, 10f, 6f, 15f, 4f, 18f, 20f, 18f, 18f, 15f, 18f, 10f); canvas.drawArc(10f, 18f, 14f, 22f, 0f, 180f, false, p) }
            "qr" -> { box(3f, 3f, 9f, 9f, .5f); box(15f, 3f, 21f, 9f, .5f); box(3f, 15f, 9f, 21f, .5f); path(15f, 15f, 18f, 15f, 18f, 18f, 21f, 18f, 21f, 21f, 15f, 21f) }
            "link" -> { canvas.drawArc(3f, 7f, 14f, 17f, 75f, 260f, false, p); canvas.drawArc(10f, 7f, 21f, 17f, 255f, 260f, false, p); path(8f, 12f, 16f, 12f) }
            "activity" -> path(2f, 12f, 6f, 12f, 9f, 5f, 14f, 20f, 18f, 12f, 22f, 12f)
            "clock" -> { canvas.drawCircle(12f, 12f, 9f, p); path(12f, 6f, 12f, 12f, 16f, 14f) }
            "pause" -> { path(8f, 5f, 8f, 19f); path(16f, 5f, 16f, 19f) }
            "plus" -> { path(5f, 12f, 19f, 12f); path(12f, 5f, 12f, 19f) }
        }
        canvas.restore()
    }
}
