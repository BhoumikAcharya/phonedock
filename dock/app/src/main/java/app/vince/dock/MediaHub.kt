package app.vince.dock

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat

/** Tracks the active media session, preferring YT Music, and notifies the music page. */
object MediaHub {

    const val YT_MUSIC = "com.google.android.apps.youtube.music"

    fun interface Listener {
        fun onMediaChanged()
    }

    var controller: MediaController? = null
        private set
    val metadata: MediaMetadata? get() = controller?.metadata
    val playbackState: PlaybackState? get() = controller?.playbackState

    private var app: Context? = null
    private var manager: MediaSessionManager? = null
    private val listeners = mutableSetOf<Listener>()
    private var started = false

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = notifyChanged()
        override fun onPlaybackStateChanged(state: PlaybackState?) = notifyChanged()
        override fun onSessionDestroyed() {
            controller = null
            refresh()
        }
    }

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start(ctx: Context) {
        app = ctx.applicationContext
        refresh()
    }

    /** Registers the sessions listener once access is granted. Returns true when registered. */
    private fun ensureStarted(ctx: Context): Boolean {
        if (started) return true
        if (manager == null) {
            manager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        }
        val component = ComponentName(ctx, MediaListener::class.java)
        try {
            manager?.addOnActiveSessionsChangedListener({ pick(it) }, component)
            started = true
        } catch (_: SecurityException) {
            // Notification access not granted yet; a later refresh() retries.
        }
        return started
    }

    fun refresh() {
        val ctx = app ?: return
        if (!ensureStarted(ctx)) {
            pick(null)
            return
        }
        val component = ComponentName(ctx, MediaListener::class.java)
        try {
            pick(manager?.getActiveSessions(component))
        } catch (_: SecurityException) {
            pick(null)
        }
    }

    private fun pick(sessions: List<MediaController>?) {
        val list = sessions.orEmpty()
        val chosen = list.firstOrNull { it.packageName == YT_MUSIC }
            ?: list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = chosen
            chosen?.registerCallback(callback)
        }
        notifyChanged()
    }

    fun addListener(l: Listener) {
        listeners += l
    }

    fun removeListener(l: Listener) {
        listeners -= l
    }

    private fun notifyChanged() {
        listeners.toList().forEach { it.onMediaChanged() }
    }
}
