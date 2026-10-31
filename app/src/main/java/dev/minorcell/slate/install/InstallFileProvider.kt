package dev.minorcell.slate.install

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class InstallFileProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor? {
        val path = uri.getQueryParameter(QUERY_PATH) ?: return null
        val file = File(path).canonicalFile
        val allowed = file.path.startsWith("/storage/") && file.extension.equals("apk", ignoreCase = true)
        if (!allowed || !file.isFile) return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "dev.minorcell.slate.install"
        private const val QUERY_PATH = "path"

        fun uri(path: String): Uri =
            Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .path("apk")
                .appendQueryParameter(QUERY_PATH, path)
                .build()
    }
}
