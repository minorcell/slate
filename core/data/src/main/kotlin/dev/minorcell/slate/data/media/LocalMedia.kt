package dev.minorcell.slate.data.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class MediaItem(
    val name: String,
    val uri: String,
)

data class ApkFile(
    val name: String,
    val path: String,
    val size: Long,
    /** MediaStore 给出的 content URI；scoped storage 下比文件路径可靠。 */
    val uri: Uri? = null,
)

class LocalMedia(private val context: Context) {
    fun images(): List<MediaItem> = query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    fun videos(): List<MediaItem> = query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

    fun thumb(
        uri: String,
        reqWidth: Int,
    ): Bitmap? =
        try {
            val parsed = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val opts =
                BitmapFactory.Options().apply {
                    inSampleSize = sample(bounds.outWidth, reqWidth)
                }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }

    /**
     * 先问 MediaStore，再退回目录遍历。
     *
     * Android 10 起应用不能随意遍历 /storage，MediaStore 是唯一稳定来源；
     * 老旧电视（API 28 及以下）没有 scoped storage，遍历能补上 MediaStore 没收录的文件。
     */
    fun apks(): List<ApkFile> {
        val found = linkedMapOf<String, ApkFile>()
        queryApks(found)
        if (found.size < LIMIT) {
            val roots = mutableListOf<File>()
            Environment.getExternalStorageDirectory()?.let { roots.add(it) }
            File("/storage").listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name != "self" && dir.name != "emulated") roots.add(dir)
            }
            roots.distinctBy { it.absolutePath }.forEach { walk(it, 0, found) }
        }
        return found.values.toList()
    }

    private fun queryApks(out: MutableMap<String, ApkFile>) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection =
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATA,
            )
        val cursor =
            try {
                context.contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                    arrayOf("%.apk"),
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC",
                )
            } catch (_: Exception) {
                null
            } ?: return
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dataColumn = it.getColumnIndex(MediaStore.MediaColumns.DATA)
            while (it.moveToNext() && out.size < LIMIT) {
                val id = it.getLong(idColumn)
                val path = if (dataColumn >= 0) it.getString(dataColumn).orEmpty() else ""
                val size = if (sizeColumn >= 0) it.getLong(sizeColumn) else 0L
                val name = it.getString(nameColumn).orEmpty().ifBlank { path.substringAfterLast('/') }
                val key = path.ifBlank { id.toString() }
                out[key] =
                    ApkFile(
                        name = name.ifBlank { key },
                        path = path,
                        size = size,
                        uri = ContentUris.withAppendedId(collection, id),
                    )
            }
        }
    }

    private fun query(collection: Uri): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor =
            try {
                context.contentResolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC",
                )
            } catch (_: SecurityException) {
                null
            } ?: return items
        cursor.use {
            var count = 0
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (it.moveToNext() && count < LIMIT) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn).orEmpty().ifBlank { id.toString() }
                items.add(MediaItem(name, ContentUris.withAppendedId(collection, id).toString()))
                count += 1
            }
        }
        return items
    }

    private fun walk(
        dir: File,
        depth: Int,
        out: MutableMap<String, ApkFile>,
    ) {
        if (depth > MAX_DEPTH || out.size >= LIMIT) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (out.size >= LIMIT) return
            if (child.name == "Android") continue
            if (child.isFile && child.name.endsWith(".apk", ignoreCase = true)) {
                out.getOrPut(child.absolutePath) {
                    ApkFile(child.name, child.absolutePath, child.length())
                }
            } else if (child.isDirectory) {
                walk(child, depth + 1, out)
            }
        }
    }

    private fun sample(
        width: Int,
        reqWidth: Int,
    ): Int {
        var sample = 1
        if (reqWidth <= 0 || width <= 0) return sample
        while (width / sample > reqWidth * 2) sample *= 2
        return sample
    }

    private companion object {
        const val LIMIT = 40
        const val MAX_DEPTH = 2
    }
}
