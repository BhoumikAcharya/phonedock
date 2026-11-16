package app.vince.dock

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * One tile on the dock: either an installed app or a pinned shortcut (for example a PWA
 * added from Chrome). [key] is what the pin list stores.
 */
data class AppEntry(
    val label: String,
    val key: String,
    val icon: Drawable,
    val intent: Intent? = null,
    val shortcut: ShortcutInfo? = null
)

/** Launchable apps plus the user's pinned tiles, stored in SharedPreferences. */
object Apps {

    private const val PREFS = "dock"
    private const val KEY_PINS = "pins"
    private const val SHORTCUT_PREFIX = "sc:"

    private val defaultPins = listOf(
        MediaHub.YT_MUSIC,
        "com.android.chrome",
        "com.android.settings"
    )

    fun all(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(pkg, info.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                AppEntry(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm), launch)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun pins(ctx: Context): List<String> {
        val stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PINS, null)
        return stored?.split(',')?.filter { it.isNotBlank() } ?: defaultPins
    }

    private fun savePins(ctx: Context, pins: List<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PINS, pins.joinToString(","))
            .apply()
    }

    fun pinned(ctx: Context): List<AppEntry> {
        val byPkg = all(ctx).associateBy { it.key }
        return pins(ctx).mapNotNull { key ->
            if (key.startsWith(SHORTCUT_PREFIX)) shortcutEntry(ctx, key) else byPkg[key]
        }
    }

    /** Returns true if the key is pinned after the toggle. */
    fun togglePin(ctx: Context, key: String): Boolean {
        val current = pins(ctx).toMutableList()
        val nowPinned = if (current.remove(key)) false else current.add(key)
        savePins(ctx, current)
        return nowPinned
    }

    fun addShortcutPin(ctx: Context, pkg: String, id: String) {
        val key = "$SHORTCUT_PREFIX$pkg/$id"
        val current = pins(ctx).toMutableList()
        if (key !in current) current.add(key)
        savePins(ctx, current)
    }

    private fun shortcutEntry(ctx: Context, key: String): AppEntry? {
        val body = key.removePrefix(SHORTCUT_PREFIX)
        val slash = body.indexOf('/')
        if (slash <= 0) return null
        val pkg = body.substring(0, slash)
        val id = body.substring(slash + 1)
        val la = ctx.getSystemService(LauncherApps::class.java)
        val info = runCatching {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(pkg)
                .setShortcutIds(listOf(id))
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            la.getShortcuts(q, Process.myUserHandle())?.firstOrNull()
        }.getOrNull() ?: return null
        val density = ctx.resources.displayMetrics.densityDpi
        val icon = runCatching { la.getShortcutIconDrawable(info, density) }.getOrNull() ?: return null
        val label = (info.shortLabel ?: info.longLabel ?: pkg).toString()
        return AppEntry(label, key, icon, shortcut = info)
    }

    fun launch(ctx: Context, entry: AppEntry) {
        runCatching {
            val sc = entry.shortcut
            if (sc != null) {
                ctx.getSystemService(LauncherApps::class.java)
                    .startShortcut(sc.`package`, sc.id, null, null, Process.myUserHandle())
            } else {
                entry.intent?.let { ctx.startActivity(it) }
            }
        }
    }
}

class AppAdapter(
    private val items: List<AppEntry>,
    private val layoutRes: Int = R.layout.item_app,
    private val monochrome: Boolean = false,
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
    }

    private val monoFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(layoutRes, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = items[position]
        holder.icon.setImageDrawable(entry.icon)
        holder.icon.colorFilter = if (monochrome) monoFilter else null
        holder.label.text = entry.label
        holder.itemView.setOnClickListener { onClick(entry) }
        holder.itemView.setOnLongClickListener { onLongClick(entry); true }
    }
}
