package app.vince.dock

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Grid of every launchable app. Tap opens it, long press pins or unpins it on the dock. */
object AppsDialog {

    fun show(ctx: Context, onPinsChanged: () -> Unit) {
        val grid = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, 6)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            clipToPadding = false
        }
        lateinit var dialog: AlertDialog
        grid.adapter = AppAdapter(
            Apps.all(ctx),
            onClick = {
                Apps.launch(ctx, it)
                dialog.dismiss()
            },
            onLongClick = {
                val pinned = Apps.togglePin(ctx, it.key)
                Toast.makeText(
                    ctx,
                    ctx.getString(if (pinned) R.string.pinned else R.string.unpinned),
                    Toast.LENGTH_SHORT
                ).show()
                onPinsChanged()
            }
        )
        dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.all_apps)
            .setMessage(R.string.long_press_hint)
            .setView(grid)
            .create()
        dialog.show()
    }
}
