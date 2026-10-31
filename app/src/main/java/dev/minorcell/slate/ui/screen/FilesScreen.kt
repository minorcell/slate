package dev.minorcell.slate.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.minorcell.slate.R
import dev.minorcell.slate.data.media.ApkFile
import dev.minorcell.slate.data.media.LocalMedia
import dev.minorcell.slate.install.InstallFileProvider
import dev.minorcell.slate.ui.shell.AppScreen
import dev.minorcell.slate.ui.shell.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件管理器：列出存储与 U 盘里的安装包，确定即安装。
 *
 * 目前只收安装包；照片和视频分别由相册、播放器读取 MediaStore。
 */
class FilesScreen(
    private val activity: Activity,
    private val media: LocalMedia,
    private val scope: CoroutineScope,
    private val launch: (Intent) -> Unit,
) : AppScreen {
    private val root = LayoutInflater.from(activity).inflate(R.layout.list_screen, null, false)
    private val rows = root.findViewById<LinearLayout>(R.id.rows)
    private val empty = root.findViewById<TextView>(R.id.empty)
    private var asked = false

    override val view: View = root

    init {
        root.findViewById<TextView>(R.id.screen_title).setText(R.string.files)
        root.findViewById<TextView>(R.id.screen_hint).setText(R.string.files_hint)
        empty.setOnClickListener { askOrLoad() }
    }

    override fun onShow() {
        if (StorageAccess.filesGranted(activity)) {
            load()
        } else {
            showDenied()
            if (!asked) {
                asked = true
                StorageAccess.requestFiles(activity)
            }
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) load() else showDenied()
    }

    private fun askOrLoad() {
        when {
            StorageAccess.filesGranted(activity) -> load()
            !asked || StorageAccess.canAskFiles(activity) -> {
                asked = true
                StorageAccess.requestFiles(activity)
            }
            else -> StorageAccess.openSettings(activity)
        }
    }

    private fun load() {
        empty.visibility = View.GONE
        empty.isFocusable = false
        scope.launch {
            val files = withContext(Dispatchers.IO) { media.apks() }
            rows.removeAllViews()
            if (files.isEmpty()) {
                showMessage(R.string.empty_apks)
                return@launch
            }
            files.forEach { file -> rows.addView(row(file)) }
            rows.getChildAt(0)?.requestFocus()
        }
    }

    private fun row(file: ApkFile): View {
        val row = LayoutInflater.from(activity).inflate(R.layout.setting_row, rows, false)
        row.findViewById<TextView>(R.id.label).text = file.name
        row.findViewById<TextView>(R.id.value).text = sizeLabel(file.size)
        row.setOnClickListener { install(file) }
        return row
    }

    private fun install(file: ApkFile) {
        if (!canInstall()) {
            Toast.makeText(activity, R.string.allow_install, Toast.LENGTH_SHORT).show()
            val intent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${activity.packageName}"))
            launch(intent)
            return
        }
        val data = file.uri ?: InstallFileProvider.uri(file.path)
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(data, APK_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launch(intent)
    }

    private fun canInstall(): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        return activity.packageManager.canRequestPackageInstalls()
    }

    private fun showDenied() {
        val permanent = !StorageAccess.filesGranted(activity) && !StorageAccess.canAskFiles(activity)
        showMessage(if (permanent) R.string.allow_files_settings else R.string.allow_files)
    }

    private fun showMessage(text: Int) {
        rows.removeAllViews()
        empty.setText(text)
        empty.visibility = View.VISIBLE
        empty.isFocusable = true
        empty.isClickable = true
        empty.requestFocus()
    }

    private fun sizeLabel(bytes: Long): String {
        val mb = bytes / BYTES_MB
        val tenth = (bytes % BYTES_MB) * 10 / BYTES_MB
        return "$mb.$tenth MB"
    }

    private companion object {
        const val APK_TYPE = "application/vnd.android.package-archive"
        const val BYTES_MB = 1024L * 1024L
    }
}
