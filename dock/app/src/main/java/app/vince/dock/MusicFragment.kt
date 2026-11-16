package app.vince.dock

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Locale

class MusicFragment : Fragment(R.layout.page_music), MediaHub.Listener {

    private lateinit var art: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var seek: SeekBar
    private lateinit var position: TextView
    private lateinit var duration: TextView
    private lateinit var controls: View
    private lateinit var playPause: Button
    private lateinit var status: TextView
    private lateinit var grantAccess: Button

    private val handler = Handler(Looper.getMainLooper())
    private var userSeeking = false
    private val ticker = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        art = view.findViewById(R.id.art)
        title = view.findViewById(R.id.title)
        artist = view.findViewById(R.id.artist)
        seek = view.findViewById(R.id.seek)
        position = view.findViewById(R.id.position)
        duration = view.findViewById(R.id.duration)
        controls = view.findViewById(R.id.controls)
        playPause = view.findViewById(R.id.playPause)
        status = view.findViewById(R.id.status)
        grantAccess = view.findViewById(R.id.grantAccess)

        view.findViewById<View>(R.id.lockButton).setOnClickListener {
            (activity as? MainActivity)?.lockScreen()
        }
        view.findViewById<View>(R.id.prev).setOnClickListener {
            MediaHub.controller?.transportControls?.skipToPrevious()
        }
        view.findViewById<View>(R.id.next).setOnClickListener {
            MediaHub.controller?.transportControls?.skipToNext()
        }
        playPause.setOnClickListener {
            val tc = MediaHub.controller?.transportControls ?: return@setOnClickListener
            if (isPlaying()) tc.pause() else tc.play()
        }
        view.findViewById<View>(R.id.openMusic).setOnClickListener { openMusicApp() }
        grantAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) position.text = formatMs(progress.toLong())
            }

            override fun onStartTrackingTouch(bar: SeekBar) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(bar: SeekBar) {
                userSeeking = false
                MediaHub.controller?.transportControls?.seekTo(bar.progress.toLong())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        MediaHub.addListener(this)
        MediaHub.refresh()
        render()
        handler.post(ticker)
    }

    override fun onStop() {
        super.onStop()
        MediaHub.removeListener(this)
        handler.removeCallbacks(ticker)
    }

    override fun onMediaChanged() {
        if (isAdded) handler.post { render() }
    }

    private fun isPlaying() =
        MediaHub.playbackState?.state == PlaybackState.STATE_PLAYING

    private fun render() {
        val ctx = context ?: return
        val hasAccess = MediaHub.hasAccess(ctx)
        val controller = MediaHub.controller
        grantAccess.visibility = if (hasAccess) View.GONE else View.VISIBLE

        if (!hasAccess || controller == null) {
            title.text = ""
            artist.text = ""
            art.setImageDrawable(null)
            controls.visibility = View.INVISIBLE
            seek.visibility = View.INVISIBLE
            position.text = ""
            duration.text = ""
            status.text = getString(if (hasAccess) R.string.music_idle else R.string.music_no_access)
            return
        }

        val meta = MediaHub.metadata
        title.text = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        artist.text = listOfNotNull(
            meta?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        ).filter { it.isNotBlank() }.joinToString("  •  ")
        val bitmap = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        // Keep the art no larger than the view; large session bitmaps otherwise sit in our heap.
        val target = art.width.coerceAtLeast(1)
        val scaled = if (bitmap != null && bitmap.width > target * 1.2) {
            val h = (bitmap.height.toLong() * target / bitmap.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, target, h, true)
        } else bitmap
        art.setImageBitmap(scaled)

        val durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        seek.max = durationMs.coerceAtLeast(1L).toInt()
        seek.visibility = View.VISIBLE
        duration.text = if (durationMs > 0) formatMs(durationMs) else ""
        controls.visibility = View.VISIBLE
        playPause.setText(if (isPlaying()) R.string.pause_glyph else R.string.play)

        val appLabel = runCatching {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(controller.packageName, 0)).toString()
        }.getOrDefault(controller.packageName)
        status.text = appLabel
        updateProgress()
    }

    private fun updateProgress() {
        if (userSeeking) return
        val state = MediaHub.playbackState ?: return
        val playing = state.state == PlaybackState.STATE_PLAYING
        playPause.setText(if (playing) R.string.pause_glyph else R.string.play)
        var pos = state.position
        if (state.state == PlaybackState.STATE_PLAYING) {
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            pos += (elapsed * state.playbackSpeed).toLong()
        }
        pos = pos.coerceIn(0L, seek.max.toLong())
        seek.progress = pos.toInt()
        position.text = formatMs(pos)
    }

    private fun openMusicApp() {
        val ctx = context ?: return
        val intent = ctx.packageManager.getLaunchIntentForPackage(MediaHub.YT_MUSIC)
        if (intent != null) {
            startActivity(intent)
        } else {
            startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${MediaHub.YT_MUSIC}"))
            )
        }
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }
}
