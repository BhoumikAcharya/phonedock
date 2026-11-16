package app.vince.dock

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class PomodoroFragment : Fragment(R.layout.page_pomodoro) {

    private lateinit var modeText: TextView
    private lateinit var timeText: TextView
    private lateinit var startButton: Button
    private lateinit var focusValue: TextView
    private lateinit var breakValue: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        modeText = view.findViewById(R.id.pomodoroMode)
        timeText = view.findViewById(R.id.pomodoroTime)
        startButton = view.findViewById(R.id.pomodoroStart)
        focusValue = view.findViewById(R.id.focusValue)
        breakValue = view.findViewById(R.id.breakValue)

        view.findViewById<View>(R.id.lockButton).setOnClickListener {
            (activity as? MainActivity)?.lockScreen()
        }
        startButton.setOnClickListener { PomodoroTimer.startPause() }
        view.findViewById<View>(R.id.pomodoroReset).setOnClickListener { PomodoroTimer.reset() }
        view.findViewById<View>(R.id.pomodoroSkip).setOnClickListener { PomodoroTimer.skip() }

        view.findViewById<View>(R.id.focusMinus).setOnClickListener { PomodoroTimer.adjustFocus(-5) }
        view.findViewById<View>(R.id.focusPlus).setOnClickListener { PomodoroTimer.adjustFocus(+5) }
        view.findViewById<View>(R.id.breakMinus).setOnClickListener { PomodoroTimer.adjustBreak(-1) }
        view.findViewById<View>(R.id.breakPlus).setOnClickListener { PomodoroTimer.adjustBreak(+1) }
    }

    override fun onStart() {
        super.onStart()
        PomodoroTimer.listener = { render() }
        render()
    }

    override fun onStop() {
        super.onStop()
        PomodoroTimer.listener = null
    }

    private fun render() {
        if (!isAdded) return
        modeText.text = PomodoroTimer.mode.label
        timeText.text = PomodoroTimer.formatted()
        startButton.setText(if (PomodoroTimer.running) R.string.pause else R.string.start)
        focusValue.text = getString(R.string.minutes_value, PomodoroTimer.focusMinutes)
        breakValue.text = getString(R.string.minutes_value, PomodoroTimer.breakMinutes)
    }
}
