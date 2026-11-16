package app.vince.dock

import android.content.Context
import android.media.RingtoneManager
import android.os.CountDownTimer
import java.util.Locale

/** Process-wide pomodoro state so it survives page swipes and fragment recreation. */
object PomodoroTimer {

    enum class Mode(val label: String) { FOCUS("FOCUS"), BREAK("BREAK") }

    private const val PREFS = "dock"
    private const val KEY_FOCUS = "focus_min"
    private const val KEY_BREAK = "break_min"
    private const val MIN_MINUTES = 1
    private const val MAX_MINUTES = 120

    var focusMinutes = 25
        private set
    var breakMinutes = 5
        private set
    var mode = Mode.FOCUS
        private set
    var remainingMs = 25 * 60_000L
        private set
    var running = false
        private set
    var listener: (() -> Unit)? = null

    private var timer: CountDownTimer? = null
    private var app: Context? = null
    private var initialised = false

    fun init(ctx: Context) {
        if (initialised) return
        app = ctx.applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        focusMinutes = prefs.getInt(KEY_FOCUS, 25)
        breakMinutes = prefs.getInt(KEY_BREAK, 5)
        remainingMs = minutesFor(mode) * 60_000L
        lastFullLengthMs = remainingMs
        initialised = true
    }

    private fun minutesFor(m: Mode) = if (m == Mode.FOCUS) focusMinutes else breakMinutes

    fun adjustFocus(delta: Int) {
        focusMinutes = (focusMinutes + delta).coerceIn(MIN_MINUTES, MAX_MINUTES)
        saveAndApply()
    }

    fun adjustBreak(delta: Int) {
        breakMinutes = (breakMinutes + delta).coerceIn(MIN_MINUTES, MAX_MINUTES)
        saveAndApply()
    }

    private fun saveAndApply() {
        app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_FOCUS, focusMinutes)
            ?.putInt(KEY_BREAK, breakMinutes)
            ?.apply()
        // A session that has not started counting yet picks up the new length immediately.
        if (!running && remainingMs == lastFullLengthMs) {
            remainingMs = minutesFor(mode) * 60_000L
            lastFullLengthMs = remainingMs
        }
        listener?.invoke()
    }

    /** Length the current session started from; equal to remainingMs until it starts ticking. */
    private var lastFullLengthMs = 25 * 60_000L

    fun startPause() = if (running) pause() else start()

    private fun start() {
        timer?.cancel()
        running = true
        timer = object : CountDownTimer(remainingMs, 250) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                listener?.invoke()
            }

            override fun onFinish() {
                remainingMs = 0
                running = false
                chime()
                switchMode()
                listener?.invoke()
            }
        }.start()
        listener?.invoke()
    }

    fun pause() {
        timer?.cancel()
        running = false
        listener?.invoke()
    }

    fun reset() {
        timer?.cancel()
        running = false
        remainingMs = minutesFor(mode) * 60_000L
        lastFullLengthMs = remainingMs
        listener?.invoke()
    }

    fun skip() {
        timer?.cancel()
        running = false
        switchMode()
        listener?.invoke()
    }

    private fun switchMode() {
        mode = if (mode == Mode.FOCUS) Mode.BREAK else Mode.FOCUS
        remainingMs = minutesFor(mode) * 60_000L
        lastFullLengthMs = remainingMs
    }

    private fun chime() {
        val ctx = app ?: return
        runCatching {
            RingtoneManager.getRingtone(
                ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )?.play()
        }
    }

    fun formatted(): String {
        val totalSeconds = (remainingMs + 999) / 1000
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}
