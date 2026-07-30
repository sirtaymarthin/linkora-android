package com.linkora.data

/** Reconocimiento de plataforma por dominio: color e icono de respaldo. */
object Brands {

    data class Brand(val key: String, val label: String, val color: Long, val icon: String)

    private val generic = Brand("web", "Web", 0xFF5E5CE6, "public")

    private val list: List<Pair<Brand, Regex>> = listOf(
        Brand("youtube", "YouTube", 0xFFFF0033, "play") to Regex("(^|\\.)(youtube\\.com|youtu\\.be)$"),
        Brand("instagram", "Instagram", 0xFFC13584, "camera") to Regex("(^|\\.)instagram\\.com$"),
        Brand("x", "X", 0xFF111111, "chat") to Regex("(^|\\.)(twitter|x)\\.com$"),
        Brand("facebook", "Facebook", 0xFF1877F2, "public") to Regex("(^|\\.)(facebook\\.com|fb\\.watch)$"),
        Brand("linkedin", "LinkedIn", 0xFF0A66C2, "work") to Regex("(^|\\.)linkedin\\.com$"),
        Brand("tiktok", "TikTok", 0xFF010101, "music") to Regex("(^|\\.)tiktok\\.com$"),
        Brand("spotify", "Spotify", 0xFF1DB954, "music") to Regex("(^|\\.)spotify\\.com$"),
        Brand("github", "GitHub", 0xFF24292F, "code") to Regex("(^|\\.)github\\.com$"),
        Brand("reddit", "Reddit", 0xFFFF4500, "forum") to Regex("(^|\\.)reddit\\.com$"),
        Brand("twitch", "Twitch", 0xFF9146FF, "videocam") to Regex("(^|\\.)twitch\\.tv$"),
        Brand("wikipedia", "Wikipedia", 0xFF4B5563, "book") to Regex("(^|\\.)wikipedia\\.org$"),
        Brand("maps", "Maps", 0xFF34A853, "map") to Regex("(^|\\.)(maps\\.google\\.[a-z.]+|maps\\.app\\.goo\\.gl)$"),
        Brand("amazon", "Amazon", 0xFFFF9900, "cart") to Regex("(^|\\.)amazon\\.[a-z.]+$"),
        Brand("booking", "Booking", 0xFF003580, "hotel") to Regex("(^|\\.)booking\\.com$")
    )

    fun of(url: String?): Brand {
        val h = hostOf(url) ?: return generic
        return list.firstOrNull { it.second.containsMatchIn(h) }?.first ?: generic
    }

    fun byKey(key: String?): Brand = list.firstOrNull { it.first.key == key }?.first ?: generic

    fun fileBrand(mime: String?): Brand = when {
        mime == "application/pdf" -> Brand("pdf", "PDF", 0xFFE5484D, "pdf")
        mime?.startsWith("image/") == true -> Brand("img", "Imagen", 0xFF0EA5E9, "image")
        else -> Brand("file", "Archivo", 0xFF64748B, "file")
    }
}
