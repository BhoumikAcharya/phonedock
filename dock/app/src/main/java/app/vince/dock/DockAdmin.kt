package app.vince.dock

import android.app.admin.DeviceAdminReceiver

/** Device admin receiver that only exists so the Lock button can call lockNow(). */
class DockAdmin : DeviceAdminReceiver()
