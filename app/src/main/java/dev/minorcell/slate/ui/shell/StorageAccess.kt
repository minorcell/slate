package dev.minorcell.slate.ui.shell

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

internal object StorageAccess {
    const val REQUEST_CODE = 41

    enum class Kind { Images, Videos }

    fun permission(kind: Kind): String =
        when {
            Build.VERSION.SDK_INT >= 33 && kind == Kind.Images -> Manifest.permission.READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= 33 -> Manifest.permission.READ_MEDIA_VIDEO
            else -> Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun granted(
        activity: Activity,
        kind: Kind,
    ): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        return activity.checkSelfPermission(permission(kind)) == PackageManager.PERMISSION_GRANTED
    }

    fun request(
        activity: Activity,
        kind: Kind,
    ) {
        if (Build.VERSION.SDK_INT < 23) return
        activity.requestPermissions(arrayOf(permission(kind)), REQUEST_CODE)
    }

    fun filesGranted(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 33) return true
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestFiles(activity: Activity) {
        if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 33) return
        activity.requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CODE,
        )
    }
}
