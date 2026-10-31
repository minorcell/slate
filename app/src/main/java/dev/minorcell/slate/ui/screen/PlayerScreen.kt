package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.net.Uri
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import dev.minorcell.slate.R
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.data.media.MediaItem
import dev.minorcell.slate.ui.TileFactory
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.FocusScale
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerScreen(
    private val activity: Activity,
    private val media: LocalMedia,
    private val tiles: TileFactory,
    private val scope: CoroutineScope,
) : AppScreen {
    private val root = LayoutInflater.from(activity).inflate(R.layout.strip_screen, null, false)
    private val strip = root.findViewById<LinearLayout>(R.id.strip)
    private val empty = root.findViewById<TextView>(R.id.empty)
    private val player = root.findViewById<VideoView>(R.id.player)
    private var asked = false
    private var playing = false
    private var alive = true

    override val view: View = root

    init {
        root.findViewById<TextView>(R.id.screen_title).setText(R.string.player)
        root.findViewById<TextView>(R.id.screen_hint).setText(R.string.player_hint)
        empty.setOnClickListener { askOrLoad() }
        player.setOnPreparedListener { playback ->
            if (!alive) return@setOnPreparedListener
            playing = true
            playback.start()
        }
        player.setOnCompletionListener { stop() }
        player.setOnErrorListener { _, _, _ ->
            Toast.makeText(activity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
            stop()
            true
        }
    }

    override fun onShow() {
        if (StorageAccess.granted(activity, StorageAccess.Kind.Videos)) {
            load()
        } else {
            showDenied()
            if (!asked) {
                asked = true
                StorageAccess.request(activity, StorageAccess.Kind.Videos)
            }
        }
    }

    override fun onDirection(event: KeyEvent): Boolean {
        if (!playing) return false
        val key = event.keyCode
        if (key !in DIRECTION_KEYS) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (key) {
                KeyEvent.KEYCODE_DPAD_LEFT -> seek(-SEEK_MS)
                KeyEvent.KEYCODE_DPAD_RIGHT -> seek(SEEK_MS)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> toggle()
            }
        }
        return true
    }

    override fun onBack(): Boolean {
        if (!playing) return false
        stop()
        return true
    }

    override fun onHostPause() {
        stop()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) load() else showDenied()
    }

    override fun destroy() {
        alive = false
        stop()
    }

    private fun askOrLoad() {
        if (StorageAccess.granted(activity, StorageAccess.Kind.Videos)) {
            load()
        } else {
            StorageAccess.request(activity, StorageAccess.Kind.Videos)
        }
    }

    private fun load() {
        if (playing) return
        empty.visibility = View.GONE
        empty.isFocusable = false
        scope.launch {
            val items = withContext(Dispatchers.IO) { media.videos() }
            if (!alive || playing) return@launch
            strip.removeAllViews()
            if (items.isEmpty()) {
                showMessage(R.string.empty_videos)
                return@launch
            }
            items.forEach { item -> strip.addView(tile(item)) }
            strip.getChildAt(0)?.requestFocus()
        }
    }

    private fun tile(item: MediaItem): View =
        tiles.plain(item.name, COLOR).apply {
            FocusScale.bind(this)
            setOnClickListener { play(item) }
        }

    private fun play(item: MediaItem) {
        playing = true
        player.visibility = View.VISIBLE
        player.setVideoURI(Uri.parse(item.uri))
    }

    private fun toggle() {
        if (player.isPlaying) player.pause() else player.start()
    }

    private fun seek(delta: Int) {
        val duration = player.duration
        if (duration <= 0) return
        player.seekTo((player.currentPosition + delta).coerceIn(0, duration))
    }

    private fun stop() {
        if (!playing && player.visibility != View.VISIBLE) return
        playing = false
        player.stopPlayback()
        player.visibility = View.GONE
    }

    private fun showDenied() {
        showMessage(R.string.allow_files)
    }

    private fun showMessage(text: Int) {
        strip.removeAllViews()
        empty.setText(text)
        empty.visibility = View.VISIBLE
        empty.isFocusable = true
        empty.isClickable = true
        empty.requestFocus()
    }

    private companion object {
        const val SEEK_MS = 10_000
        val COLOR = 0xFF141A16.toInt()
        val DIRECTION_KEYS =
            setOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
            )
    }
}
