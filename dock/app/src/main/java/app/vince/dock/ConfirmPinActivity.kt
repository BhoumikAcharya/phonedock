package app.vince.dock

import android.app.Activity
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Bundle
import android.widget.Toast

/**
 * Handles "Add to Home screen" requests from other apps (Chrome PWAs, app shortcuts).
 * Declaring this is what makes Android report the dock as a launcher that supports
 * pinned shortcuts, which Chrome checks before offering Install / Add to Home screen.
 */
class ConfirmPinActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = launcherApps.getPinItemRequest(intent)
        if (request != null && request.isValid &&
            request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT
        ) {
            val info = request.shortcutInfo
            if (info != null && request.accept()) {
                Apps.addShortcutPin(this, info.`package`, info.id)
                Toast.makeText(this, R.string.pinned, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
