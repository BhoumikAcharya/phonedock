package app.vince.dock

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/** One flip-clock card: a big digit on a rounded card with a split line through the middle. */
class FlipDigit @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val text = TextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.fg))
        textSize = 150f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        includeFontPadding = false
    }

    private var current = ""

    init {
        background = ContextCompat.getDrawable(context, R.drawable.card_bg)
        addView(text, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val divider = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg))
        }
        val dividerHeight = (3 * resources.displayMetrics.density).toInt()
        addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, dividerHeight, Gravity.CENTER_VERTICAL))
        cameraDistance = 12000 * resources.displayMetrics.density
    }

    fun set(value: String, animate: Boolean) {
        if (value == current) return
        if (!animate) {
            current = value
            text.text = value
            rotationX = 0f
            return
        }
        animate().rotationX(90f).setDuration(160).withEndAction {
            current = value
            text.text = value
            rotationX = -90f
            animate().rotationX(0f).setDuration(160).start()
        }.start()
    }
}
