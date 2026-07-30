package com.linkora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Copia y miniaturas de archivos compartidos, en almacenamiento privado de la app. */
object Files {

    fun mediaDir(ctx: Context): File = File(ctx.filesDir, "media").apply { mkdirs() }
    fun thumbDir(ctx: Context): File = File(ctx.filesDir, "thumbs").apply { mkdirs() }

    fun resolve(ctx: Context, relPath: String?): File? =
        relPath?.let { File(ctx.filesDir, it) }?.takeIf { it.exists() }

    data class Imported(
        val fileName: String,
        val mime: String,
        val size: Long,
        val relPath: String,
        val thumbRelPath: String?
    )

    /** Copia el contenido de un Uri compartido a almacenamiento propio. */
    fun importUri(ctx: Context, uri: Uri): Imported? = runCatching {
        val cr = ctx.contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        var name = "archivo"
        cr.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i)?.let { name = it }
        }
        val ext = name.substringAfterLast('.', "").ifBlank { extFor(mime) }
        val id = UUID.randomUUID().toString()
        val target = File(mediaDir(ctx), if (ext.isBlank()) id else "$id.$ext")
        cr.openInputStream(uri).use { input ->
            requireNotNull(input) { "sin flujo de entrada" }
            FileOutputStream(target).use { out -> input.copyTo(out) }
        }
        val thumb = if (mime.startsWith("image/")) makeThumb(ctx, target, id) else null
        Imported(name, mime, target.length(), "media/${target.name}", thumb)
    }.getOrNull()

    private fun extFor(mime: String) = when {
        mime == "application/pdf" -> "pdf"
        mime == "image/png" -> "png"
        mime == "image/webp" -> "webp"
        mime.startsWith("image/") -> "jpg"
        else -> ""
    }

    /** Miniatura reducida para que la cuadrícula sea rápida. */
    private fun makeThumb(ctx: Context, src: File, id: String): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, bounds)
        val max = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = generateSequence(1) { it * 2 }.first { max / it <= 720 }
        }
        val bmp = BitmapFactory.decodeFile(src.absolutePath, opts) ?: return null
        val out = File(thumbDir(ctx), "$id.jpg")
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        bmp.recycle()
        "thumbs/${out.name}"
    }.getOrNull()

    fun deleteFor(ctx: Context, item: LinkItem) {
        resolve(ctx, item.filePath)?.delete()
        resolve(ctx, item.thumbPath)?.delete()
    }

    /** Borra binarios sin dueño (quedan tras eliminar sin deshacer). */
    suspend fun collectOrphans(ctx: Context, dao: LinkoraDao) {
        runCatching {
            val used = dao.allLinks()
                .flatMap { listOfNotNull(it.filePath, it.thumbPath) }
                .map { it.substringAfterLast('/') }
                .toSet()
            listOf(mediaDir(ctx), thumbDir(ctx)).forEach { dir ->
                dir.listFiles()?.forEach { f -> if (f.name !in used) f.delete() }
            }
        }
    }
}
