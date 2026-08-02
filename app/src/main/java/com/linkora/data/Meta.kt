package com.linkora.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Metadatos de un enlace leyendo Open Graph directamente.
 * En nativo no hay CORS: no se necesitan proxies externos.
 */
object Meta {

    data class Result(val title: String?, val desc: String?, val image: String?)

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"

    private val YT =
        Regex("(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})")

    suspend fun fetch(url: String): Result = withContext(Dispatchers.IO) {
        // Atajo: YouTube da miniatura sin descargar la página.
        val ytThumb = YT.find(url)?.groupValues?.getOrNull(1)
            ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

        val parsed = runCatching {
            val doc = Jsoup.connect(url)
                .userAgent(UA)
                .timeout(9000)
                .followRedirects(true)
                .ignoreContentType(true)
                .get()

            fun meta(vararg names: String): String? = names.firstNotNullOfOrNull { n ->
                doc.selectFirst("meta[property=$n]")?.attr("content")?.takeIf { it.isNotBlank() }
                    ?: doc.selectFirst("meta[name=$n]")?.attr("content")?.takeIf { it.isNotBlank() }
            }

            Result(
                title = meta("og:title", "twitter:title") ?: doc.title().takeIf { it.isNotBlank() },
                desc = meta("og:description", "twitter:description", "description"),
                image = meta("og:image", "og:image:secure_url", "twitter:image")?.let { abs(url, it) }
            )
        }.getOrNull()

        // oEmbed: cubre Vimeo, SoundCloud y algunos más
        val oembed = if (parsed?.title == null && parsed?.image == null) runCatching {
            val oUrl = "https://noembed.com/embed?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            val oDoc = Jsoup.connect(oUrl).ignoreContentType(true).timeout(7000).get()
            val j = org.json.JSONObject(oDoc.text())
            Result(
                title = j.optString("title").takeIf { it.isNotBlank() },
                desc = j.optString("author_name").takeIf { it.isNotBlank() },
                image = j.optString("thumbnail_url").takeIf { it.isNotBlank() }
            )
        }.getOrNull() else null

        // Microlink: rendering completo, cubre Instagram y sitios que bloquean scrapers
        val micro = if (parsed?.image == null && oembed?.image == null) runCatching {
            val mUrl = "https://api.microlink.io/?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            val mDoc = Jsoup.connect(mUrl).ignoreContentType(true).timeout(9000).get()
            val d = org.json.JSONObject(mDoc.text()).optJSONObject("data")
            Result(
                title = d?.optString("title")?.takeIf { it.isNotBlank() },
                desc = d?.optString("description")?.takeIf { it.isNotBlank() },
                image = d?.optJSONObject("image")?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: d?.optJSONObject("logo")?.optString("url")?.takeIf { it.isNotBlank() }
            )
        }.getOrNull() else null

        Result(
            title = parsed?.title ?: oembed?.title ?: micro?.title,
            desc = parsed?.desc ?: oembed?.desc ?: micro?.desc,
            image = ytThumb ?: parsed?.image ?: oembed?.image ?: micro?.image
        )
    }

    private fun abs(base: String, maybeRelative: String): String = runCatching {
        java.net.URI(base).resolve(maybeRelative).toString()
    }.getOrDefault(maybeRelative)

    fun faviconOf(url: String?): String? =
        hostOf(url)?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }
}
