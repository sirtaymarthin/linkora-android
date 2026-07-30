package com.linkora.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Copia de seguridad en ZIP: data.json + los archivos reales (sin base64). */
object Backup {

    suspend fun export(ctx: Context, dao: LinkoraDao, dest: Uri): Int =
        withContext(Dispatchers.IO) {
            val links = dao.allLinks()
            val cats = dao.allCats()
            val root = JSONObject().apply {
                put("v", 3)
                put("links", JSONArray().apply { links.forEach { put(linkToJson(it)) } })
                put("cats", JSONArray().apply { cats.forEach { put(catToJson(it)) } })
            }
            ctx.contentResolver.openOutputStream(dest)!!.use { os ->
                ZipOutputStream(os.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(root.toString().toByteArray())
                    zip.closeEntry()
                    links.flatMap { listOfNotNull(it.filePath, it.thumbPath) }.distinct().forEach { rel ->
                        Files.resolve(ctx, rel)?.let { f ->
                            zip.putNextEntry(ZipEntry(rel))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            links.size
        }

    suspend fun import(ctx: Context, dao: LinkoraDao, src: Uri): Int =
        withContext(Dispatchers.IO) {
            var json: String? = null
            ctx.contentResolver.openInputStream(src)!!.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var e: ZipEntry? = zip.nextEntry
                    while (e != null) {
                        val name = e.name
                        if (name == "data.json") {
                            json = zip.readBytes().decodeToString()
                        } else if (name.startsWith("media/") || name.startsWith("thumbs/")) {
                            val out = File(ctx.filesDir, name)
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        e = zip.nextEntry
                    }
                }
            }
            val root = JSONObject(json ?: error("copia sin data.json"))
            val cats = root.optJSONArray("cats") ?: JSONArray()
            for (i in 0 until cats.length()) dao.putCat(catFromJson(cats.getJSONObject(i)))
            val links = root.optJSONArray("links") ?: JSONArray()
            for (i in 0 until links.length()) dao.put(linkFromJson(links.getJSONObject(i)))
            links.length()
        }

    private fun linkToJson(l: LinkItem) = JSONObject().apply {
        put("id", l.id); put("kind", l.kind); put("url", l.url ?: JSONObject.NULL)
        put("title", l.title ?: JSONObject.NULL); put("desc", l.desc ?: JSONObject.NULL)
        put("image", l.image ?: JSONObject.NULL); put("brand", l.brand ?: JSONObject.NULL)
        put("fileName", l.fileName ?: JSONObject.NULL); put("fileType", l.fileType ?: JSONObject.NULL)
        put("fileSize", l.fileSize); put("filePath", l.filePath ?: JSONObject.NULL)
        put("thumbPath", l.thumbPath ?: JSONObject.NULL); put("note", l.note)
        put("cat", l.cat ?: JSONObject.NULL); put("t", l.t); put("fav", l.fav)
        put("done", l.done); put("doneAt", l.doneAt ?: JSONObject.NULL)
        put("metaState", l.metaState ?: JSONObject.NULL)
    }

    private fun JSONObject.str(k: String): String? =
        if (isNull(k)) null else optString(k).takeIf { it.isNotBlank() }

    private fun linkFromJson(o: JSONObject) = LinkItem(
        id = o.getString("id"),
        kind = o.optString("kind", KIND_URL),
        url = o.str("url"), title = o.str("title"), desc = o.str("desc"),
        image = o.str("image"), brand = o.str("brand"),
        fileName = o.str("fileName"), fileType = o.str("fileType"),
        fileSize = o.optLong("fileSize"), filePath = o.str("filePath"),
        thumbPath = o.str("thumbPath"), note = o.optString("note", ""),
        cat = o.str("cat"), t = o.optLong("t", System.currentTimeMillis()),
        fav = o.optBoolean("fav"), done = o.optBoolean("done"),
        doneAt = if (o.isNull("doneAt")) null else o.optLong("doneAt"),
        metaState = o.str("metaState")
    )

    private fun catToJson(c: Category) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("icon", c.icon)
        put("color", c.color); put("parent", c.parent ?: JSONObject.NULL); put("pos", c.pos)
    }

    private fun catFromJson(o: JSONObject) = Category(
        id = o.getString("id"), name = o.optString("name", "Sin nombre"),
        icon = o.optString("icon", "star"), color = o.optLong("color", 0xFF5E5CE6),
        parent = o.str("parent"), pos = o.optInt("pos")
    )
}
