package dev.minorcell.slate.ui.shell

import android.view.View

internal object FocusScale {
    fun bind(view: View) {
        view.setOnFocusChangeListener { focusedView, focused ->
            val scale = if (focused) SCALE else 1f
            focusedView.animate().scaleX(scale).scaleY(scale).setDuration(DURATION_MS).start()
        }
    }

    private const val SCALE = 1.085f
    private const val DURATION_MS = 120L
}
