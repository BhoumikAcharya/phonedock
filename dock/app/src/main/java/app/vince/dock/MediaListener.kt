package app.vince.dock

import android.service.notification.NotificationListenerService

/**
 * Empty notification listener. Android only hands out active media sessions
 * to apps that hold notification access, and this service is what the user grants.
 */
class MediaListener : NotificationListenerService()
