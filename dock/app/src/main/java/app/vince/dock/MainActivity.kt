package app.vince.dock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val PAGE_POMODORO = 0
        const val PAGE_HOME = 1
        const val PAGE_MUSIC = 2
    }

    private lateinit var pager: ViewPager2
    private lateinit var flipClock: View
    private lateinit var flipDigits: List<FlipDigit>
    private lateinit var flipDate: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    private val flipTicker = object : Runnable {
        override fun run() {
            updateFlipClock(animate = true)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The dock is always plugged in: keep the screen on while it is showing.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        PomodoroTimer.init(applicationContext)
        MediaHub.start(applicationContext)

        pager = findViewById(R.id.pager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                PAGE_POMODORO -> PomodoroFragment()
                PAGE_MUSIC -> MusicFragment()
                else -> HomeFragment()
            }
        }
        pager.setCurrentItem(PAGE_HOME, false)

        flipClock = findViewById(R.id.flipClock)
        flipDigits = listOf(
            findViewById(R.id.flipH1), findViewById(R.id.flipH2),
            findViewById(R.id.flipM1), findViewById(R.id.flipM2)
        )
        flipDate = findViewById(R.id.flipDate)
        flipClock.setOnClickListener { hideFlipClock() }

        // Back on a launcher should never leave the app; close the clock or return home.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (flipClock.visibility == View.VISIBLE) hideFlipClock() else pager.currentItem = PAGE_HOME
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home key pressed while already running: close the clock and go home.
        if (::flipClock.isInitialized && flipClock.visibility == View.VISIBLE) hideFlipClock()
        if (::pager.isInitialized) pager.currentItem = PAGE_HOME
    }

    override fun onResume() {
        super.onResume()
        MediaHub.refresh()
        if (flipClock.visibility == View.VISIBLE) {
            handler.removeCallbacks(flipTicker)
            handler.post(flipTicker)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(flipTicker)
    }

    fun showFlipClock() {
        updateFlipClock(animate = false)
        flipClock.visibility = View.VISIBLE
        handler.removeCallbacks(flipTicker)
        handler.post(flipTicker)
    }

    private fun hideFlipClock() {
        handler.removeCallbacks(flipTicker)
        flipClock.visibility = View.GONE
    }

    private fun updateFlipClock(animate: Boolean) {
        val now = Date()
        val pattern = if (DateFormat.is24HourFormat(this)) "HHmm" else "hhmm"
        val digits = SimpleDateFormat(pattern, Locale.US).format(now)
        flipDigits.forEachIndexed { i, d -> d.set(digits[i].toString(), animate) }
        flipDate.text = dateFormat.format(now)
    }

    /** Turns the screen off via device admin; asks the user to enable the admin once. */
    fun lockScreen() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, DockAdmin::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
        } else {
            startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.admin_explanation)
                    )
            )
        }
    }
}
