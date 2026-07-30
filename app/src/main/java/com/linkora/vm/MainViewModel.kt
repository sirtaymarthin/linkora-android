package com.linkora.vm

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linkora.LinkoraApp
import com.linkora.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class Tab { HOME, SEARCH, DONE, SETTINGS }

const val RECENT_MAX = 12
const val DONE_TTL_DAYS = 7
const val OLD_DAYS = 30
private const val DAY = 86_400_000L

data class UiState(
    val links: List<LinkItem> = emptyList(),
    val cats: List<Category> = emptyList(),
    val tab: Tab = Tab.HOME,
    val query: String = "",
    val cur: String? = null,
    val sub: String? = null,
    val undoSize: Int = 0,
    val redoSize: Int = 0,
    val message: String? = null,
    val oldSeed: Int = 0
) {
    val live get() = links.filter { !it.done }
    val doneItems get() = links.filter { it.done }
    val favs get() = links.filter { it.fav && !it.done }
    val isHome get() = tab == Tab.HOME && cur == null && sub == null

    /** Lo hecho conserva categoría pero no suma en el contador. */
    fun countFor(cats: List<Category>, id: String): Int {
        val ids = mutableListOf(id)
        fun kids(p: String) { cats.filter { it.parent == p }.forEach { ids += it.id; kids(it.id) } }
        kids(id)
        return links.count { !it.done && it.cat in ids }
    }

    val visible: List<LinkItem>
        get() = when (tab) {
            Tab.SEARCH -> {
                val q = query.trim().lowercase()
                if (q.isBlank()) emptyList()
                // El buscador ve TODO, hecho o no: si no, lo archivado no se encontraría.
                else links.filter { l ->
                    (l.title ?: "").lowercase().contains(q) ||
                        l.note.lowercase().contains(q) ||
                        (l.fileName ?: "").lowercase().contains(q) ||
                        (hostOf(l.url) ?: "").lowercase().contains(q)
                }
            }
            Tab.DONE -> doneItems
            Tab.SETTINGS -> emptyList()
            Tab.HOME -> {
                var ls = live
                if (sub != null) ls = ls.filter { it.cat == sub }
                else if (cur != null) {
                    val ids = listOf(cur) + cats.filter { it.parent == cur }.map { it.id }
                    ls = ls.filter { it.cat in ids }
                }
                if (isHome) ls.filter { !it.fav }.take(RECENT_MAX) else ls
            }
        }

    /** Rescate: elementos antiguos, selección estable dentro del mismo día. */
    val rescued: List<LinkItem>
        get() {
            if (!isHome) return emptyList()
            val cut = System.currentTimeMillis() - OLD_DAYS * DAY
            val pool = links.filter { it.t < cut && !it.fav && !it.done }
            if (pool.isEmpty()) return emptyList()
            val today = (System.currentTimeMillis() / DAY).toInt() + oldSeed
            return (0 until minOf(2, pool.size))
                .map { pool[(today * 7 + it * 13).mod(pool.size)] }
                .distinct()
        }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as LinkoraApp).db.dao()
    private val undoStack = UndoStack(5)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val ctx: Context get() = getApplication()

    init {
        viewModelScope.launch {
            dao.links().collect { list -> _ui.update { it.copy(links = list) } }
        }
        viewModelScope.launch {
            dao.cats().collect { list ->
                if (list.isEmpty()) dao.putCats(DEFAULT_CATS)
                _ui.update { it.copy(cats = list) }
            }
        }
        viewModelScope.launch {
            purgeExpired()
            Files.collectOrphans(ctx, dao)
            resolvePendingMeta()
        }
    }

    // ─────────── navegación ───────────
    fun setTab(t: Tab) = _ui.update {
        if (t == Tab.HOME) it.copy(tab = t) else it.copy(tab = t, cur = null, sub = null)
    }
    fun selectCat(id: String?) = _ui.update { it.copy(cur = id, sub = null) }
    fun selectSub(id: String?) = _ui.update { it.copy(sub = id) }
    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun rerollRescued() = _ui.update { it.copy(oldSeed = it.oldSeed + 1) }
    fun consumeMessage() = _ui.update { it.copy(message = null) }
    private fun say(m: String) = _ui.update { it.copy(message = m) }

    // ─────────── acciones con deshacer ───────────
    private fun record(label: String, before: LinkItem?, after: LinkItem?) {
        undoStack.push(label, before, after)
        _ui.update { it.copy(undoSize = undoStack.undoSize, redoSize = undoStack.redoSize) }
    }

    fun toggleFav(item: LinkItem) = viewModelScope.launch {
        val after = item.copy(fav = !item.fav)
        dao.put(after)
        record(if (after.fav) "favorito" else "quitar favorito", item, after)
        say(if (after.fav) "En favoritos" else "Fuera de favoritos")
    }

    fun setDone(item: LinkItem, done: Boolean) = viewModelScope.launch {
        val after = item.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null)
        dao.put(after)
        record(if (done) "marcar hecho" else "volver a pendiente", item, after)
        say(if (done) "Hecho" else "De nuevo pendiente")
    }

    /** No borra el archivo: así se puede deshacer. Se recoge al arrancar si no se deshace. */
    fun delete(item: LinkItem) = viewModelScope.launch {
        dao.removeById(item.id)
        record("eliminar", item, null)
        say("Eliminado")
    }

    fun saveEdit(item: LinkItem, title: String, note: String, cat: String?) = viewModelScope.launch {
        val after = if (item.isFile) item.copy(fileName = title.ifBlank { item.fileName }, note = note, cat = cat)
        else item.copy(title = title.ifBlank { item.title }, note = note, cat = cat)
        dao.put(after)
        record(if (item.cat != cat) "cambiar categoría" else "editar", item, after)
        say("Actualizado")
    }

    fun undo() = viewModelScope.launch {
        val a = undoStack.popUndo() ?: return@launch say("Nada que deshacer")
        applySnapshot(a.before, (a.after ?: a.before)!!.id)
        _ui.update { it.copy(undoSize = undoStack.undoSize, redoSize = undoStack.redoSize) }
        say("Deshecho: ${a.label}")
    }

    fun redo() = viewModelScope.launch {
        val a = undoStack.popRedo() ?: return@launch say("Nada que rehacer")
        applySnapshot(a.after, (a.before ?: a.after)!!.id)
        _ui.update { it.copy(undoSize = undoStack.undoSize, redoSize = undoStack.redoSize) }
        say("Rehecho: ${a.label}")
    }

    private suspend fun applySnapshot(rec: LinkItem?, id: String) {
        if (rec != null) dao.put(rec) else dao.removeById(id)
    }

    // ─────────── altas ───────────
    fun addUrl(url: String, note: String, cat: String?) = viewModelScope.launch {
        val clean = url.trim()
        if (clean.isBlank()) return@launch
        val item = LinkItem(
            id = UUID.randomUUID().toString(),
            kind = KIND_URL,
            url = clean,
            brand = Brands.of(clean).key,
            note = note,
            cat = cat
        )
        dao.put(item)
        say("Guardado")
        resolveMeta(item)
    }

    fun addSharedFile(uri: Uri, note: String = "", cat: String? = null) = viewModelScope.launch {
        val imp = Files.importUri(ctx, uri) ?: return@launch say("No se pudo leer el archivo")
        val item = LinkItem(
            id = UUID.randomUUID().toString(),
            kind = KIND_FILE,
            fileName = imp.fileName,
            fileType = imp.mime,
            fileSize = imp.size,
            filePath = imp.relPath,
            thumbPath = imp.thumbRelPath,
            note = note,
            cat = cat
        )
        dao.put(item)
        say("Archivo guardado")
    }

    /** Punto único de entrada de lo compartido desde otras apps. */
    fun handleShare(intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return
        val type = intent.type ?: ""

        val uris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            else -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
        }
        if (uris.isNotEmpty()) {
            val note = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
            uris.forEach { addSharedFile(it, note) }
            return
        }
        if (type.startsWith("text/")) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = Regex("https?://\\S+").find(text)?.value ?: text
            if (url.isNotBlank()) addUrl(url, intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(), null)
        }
    }

    // ─────────── metadatos ───────────
    private fun resolveMeta(item: LinkItem) = viewModelScope.launch {
        if (item.isFile || item.url.isNullOrBlank()) return@launch
        val r = runCatching { Meta.fetch(item.url) }.getOrNull()
        val updated = item.copy(
            title = item.title ?: r?.title,
            desc = item.desc ?: r?.desc,
            image = item.image ?: r?.image,
            metaState = if (r?.title != null || r?.image != null) "ok" else "none"
        )
        dao.put(updated)
    }

    private suspend fun resolvePendingMeta() {
        dao.allLinks().filter { !it.isFile && it.metaState == null }.take(20).forEach { resolveMeta(it) }
    }

    fun retryMeta() = viewModelScope.launch {
        val pend = dao.allLinks().filter { !it.isFile && it.metaState == "none" }
        pend.forEach { resolveMeta(it.copy(metaState = null)) }
        say(if (pend.isEmpty()) "No hay miniaturas pendientes" else "Reintentando ${pend.size}…")
    }

    // ─────────── purga de hechos ───────────
    private suspend fun purgeExpired() {
        val before = System.currentTimeMillis() - DONE_TTL_DAYS * DAY
        dao.expiredDone(before).forEach { item ->
            Files.deleteFor(ctx, item)
            dao.removeById(item.id)
        }
    }

    // ─────────── categorías ───────────
    fun saveCat(cat: Category) = viewModelScope.launch { dao.putCat(cat) }

    fun deleteCat(cat: Category) = viewModelScope.launch {
        val ids = listOf(cat.id) + _ui.value.cats.filter { it.parent == cat.id }.map { it.id }
        dao.clearCat(ids)
        dao.removeCats(ids)
        if (_ui.value.cur in ids) selectCat(null)
    }

    fun moveCat(cat: Category, delta: Int) = viewModelScope.launch {
        val roots = _ui.value.cats.filter { it.parent == null }.sortedBy { it.pos }.toMutableList()
        val i = roots.indexOfFirst { it.id == cat.id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= roots.size) return@launch
        val tmp = roots[i]; roots[i] = roots[j]; roots[j] = tmp
        dao.putCats(roots.mapIndexed { idx, c -> c.copy(pos = idx) })
    }

    // ─────────── copia de seguridad ───────────
    fun export(dest: Uri) = viewModelScope.launch {
        runCatching { Backup.export(ctx, dao, dest) }
            .onSuccess { say("Copia exportada · $it elementos") }
            .onFailure { say("Error al exportar") }
    }

    fun import(src: Uri) = viewModelScope.launch {
        runCatching { Backup.import(ctx, dao, src) }
            .onSuccess { undoStack.clear(); say("Copia importada · $it elementos") }
            .onFailure { say("La copia no es válida") }
    }
}
