package com.linkora.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
const val KIND_URL = "url"
const val KIND_FILE = "file"
@Entity(tableName = "links")
data class LinkItem(
    @PrimaryKey val id: String, val kind: String, val url: String? = null,
    val title: String? = null, val desc: String? = null, val image: String? = null,
    val brand: String? = null, val fileName: String? = null, val fileType: String? = null,
    val fileSize: Long = 0, val filePath: String? = null, val thumbPath: String? = null,
    val note: String = "", val cat: String? = null, val t: Long = System.currentTimeMillis(),
    val fav: Boolean = false, val done: Boolean = false, val doneAt: Long? = null,
    val metaState: String? = null
) {
    val isFile get() = kind == KIND_FILE
    val displayTitle: String get() = if (isFile) (fileName ?: "Archivo")
        else title?.takeIf { it.isNotBlank() } ?: note.takeIf { it.isNotBlank() } ?: hostOf(url) ?: (url ?: "")
    val subtitle: String get() = if (isFile) when {
        fileType == "application/pdf" -> "PDF"; fileType?.startsWith("image/") == true -> "Imagen"; else -> "Archivo"
    } else hostOf(url) ?: ""
}
@Entity(tableName = "cats")
data class Category(@PrimaryKey val id: String, val name: String, val icon: String = "star",
    val color: Long = 0xFF6C63FF, val parent: String? = null, val pos: Int = 0)
@Entity(tableName = "prefs")
data class Pref(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "dashboards")
data class Dashboard(
    @PrimaryKey val id: String,
    val name: String,
    val desc: String = "",
    val author: String = "",
    val importedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dash_cats")
data class DashCat(
    @PrimaryKey val id: String,
    val dashId: String,
    val name: String,
    val icon: String = "star",
    val color: Long = 0xFF6C63FF,
    val parent: String? = null,
    val pos: Int = 0
)

@Entity(tableName = "dash_links")
data class DashLink(
    @PrimaryKey val id: String,
    val dashId: String,
    val kind: String,
    val url: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val image: String? = null,
    val brand: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Long = 0,
    val filePath: String? = null,
    val thumbPath: String? = null,
    val note: String = "",
    val cat: String? = null,
    val t: Long = System.currentTimeMillis()
) {
    val isFile get() = kind == KIND_FILE
    val displayTitle: String get() = if (isFile) (fileName ?: "Archivo")
        else title?.takeIf { it.isNotBlank() } ?: note.takeIf { it.isNotBlank() } ?: hostOf(url) ?: (url ?: "")
    val subtitle: String get() = if (isFile) when {
        fileType == "application/pdf" -> "PDF"; fileType?.startsWith("image/") == true -> "Imagen"; else -> "Archivo"
    } else hostOf(url) ?: ""
}
fun hostOf(url: String?): String? = runCatching { java.net.URI(url!!).host?.removePrefix("www.") }.getOrNull()
@Dao interface LinkoraDao {
    @Query("SELECT * FROM links ORDER BY t DESC") fun links(): Flow<List<LinkItem>>
    @Query("SELECT * FROM cats ORDER BY pos ASC") fun cats(): Flow<List<Category>>
    @Query("SELECT * FROM links") suspend fun allLinks(): List<LinkItem>
    @Query("SELECT * FROM cats") suspend fun allCats(): List<Category>
    @Upsert suspend fun put(item: LinkItem)
    @Upsert suspend fun putCat(cat: Category)
    @Upsert suspend fun putCats(cats: List<Category>)
    @Upsert suspend fun setPref(pref: Pref)
    @Query("SELECT value FROM prefs WHERE `key` = :key") suspend fun getPref(key: String): String?

    // ── Dashboards ──
    @Query("SELECT * FROM dashboards ORDER BY importedAt DESC") fun dashboards(): Flow<List<Dashboard>>
    @Query("SELECT * FROM dash_cats WHERE dashId = :dashId ORDER BY pos ASC") suspend fun dashCats(dashId: String): List<DashCat>
    @Query("SELECT * FROM dash_links WHERE dashId = :dashId ORDER BY t DESC") suspend fun dashLinks(dashId: String): List<DashLink>
    @Upsert suspend fun putDashboard(d: Dashboard)
    @Upsert suspend fun putDashCat(c: DashCat)
    @Upsert suspend fun putDashLink(l: DashLink)
    @Query("DELETE FROM dashboards WHERE id = :id") suspend fun removeDashboard(id: String)
    @Query("DELETE FROM dash_cats WHERE dashId = :dashId") suspend fun removeDashCats(dashId: String)
    @Query("DELETE FROM dash_links WHERE dashId = :dashId") suspend fun removeDashLinks(dashId: String)
    @Query("DELETE FROM links WHERE id = :id") suspend fun removeById(id: String)
    @Query("DELETE FROM cats WHERE id IN (:ids)") suspend fun removeCats(ids: List<String>)
    @Query("UPDATE links SET cat = NULL WHERE cat IN (:ids)") suspend fun clearCat(ids: List<String>)
    @Query("SELECT * FROM links WHERE done = 1 AND doneAt IS NOT NULL AND doneAt < :before") suspend fun expiredDone(before: Long): List<LinkItem>
}
@Database(entities = [LinkItem::class, Category::class, Pref::class, Dashboard::class, DashCat::class, DashLink::class], version = 3, exportSchema = false)
abstract class LinkoraDb : RoomDatabase() { abstract fun dao(): LinkoraDao }
val DEFAULT_CATS = listOf(
    Category("c1","Vídeos","movie",0xFFE5484D,pos=0), Category("c2","Lectura","book",0xFFF5A524,pos=1),
    Category("c3","Deporte","run",0xFF30A46C,pos=2), Category("c4","Trabajo","work",0xFF6C63FF,pos=3))
