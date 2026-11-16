package app.vince.dock

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment(R.layout.page_home) {

    private lateinit var shortcuts: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        shortcuts = view.findViewById(R.id.shortcuts)

        view.findViewById<View>(R.id.lockButton).setOnClickListener {
            (activity as? MainActivity)?.lockScreen()
        }
        view.findViewById<View>(R.id.timeClock).setOnClickListener {
            (activity as? MainActivity)?.showFlipClock()
        }
        view.findViewById<View>(R.id.allApps).setOnClickListener {
            AppsDialog.show(requireContext()) { refreshPins() }
        }

        shortcuts.layoutManager = GridLayoutManager(requireContext(), 5)
    }

    override fun onStart() {
        super.onStart()
        refreshPins()
    }

    private fun refreshPins() {
        val ctx = context ?: return
        shortcuts.adapter = AppAdapter(
            Apps.pinned(ctx),
            layoutRes = R.layout.item_app_large,
            monochrome = true,
            onClick = { Apps.launch(ctx, it) },
            onLongClick = {
                Apps.togglePin(ctx, it.key)
                refreshPins()
            }
        )
    }
}
