package com.linkora.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.linkora.data.LinkItem
import com.linkora.vm.*
import kotlin.math.abs

private val GAP = 13.dp

/* ═══════════════════════ HOME ═══════════════════════ */
@Composable
fun HomeScreen(
    ui: UiState,
    onSelectCat: (String?) -> Unit,
    onSelectSub: (String?) -> Unit,
    onOpenDone: () -> Unit,
    onReroll: () -> Unit,
    onOpen: (LinkItem) -> Unit,
    onFav: (LinkItem) -> Unit,
    onDone: (LinkItem) -> Unit,
    onShare: (LinkItem) -> Unit,
    onDelete: (LinkItem) -> Unit,
    onViewMode: (ViewMode) -> Unit,
    onSortOrder: (SortOrder) -> Unit
) {
    val visible = ui.visible
    val favs = ui.favs
    val rescued = ui.rescued
    val mode = ui.viewMode
    val sort = ui.sortOrder

    val roots = ui.cats.filter { it.parent == null }
    val catIds = listOf<String?>(null) + roots.map { it.id }
    val curIdx = catIds.indexOf(ui.cur).coerceAtLeast(0)

    val bubbleState = rememberLazyListState()
    LaunchedEffect(curIdx) { bubbleState.animateScrollToItem(maxOf(0, curIdx - 1)) }

    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }

    // Contenido compartido: cabecera (burbujas + barra modo) y secciones
    // En modo lista usamos LazyColumn, en grid usamos LazyVerticalGrid
    if (mode == ViewMode.LIST) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().pointerInput(catIds, ui.cur) {
                var startX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { startX = it.x }, onDragEnd = {}, onDragCancel = {},
                    onHorizontalDrag = { change, _ ->
                        val total = change.position.x - startX
                        if (abs(total) > threshold) {
                            val next = if (total < 0) curIdx + 1 else curIdx - 1
                            if (next in catIds.indices && catIds[next] != ui.cur) onSelectCat(catIds[next])
                            startX = change.position.x
                        }
                    }
                )
            }
        ) {
            item { BubbleCarousel(ui, roots, catIds, curIdx, bubbleState, onSelectCat, onOpenDone) }
            item { SubCategoryRow(ui, onSelectSub) }
            item { ViewModeBar(mode, sort, onViewMode, onSortOrder) }
            if (favs.isNotEmpty() && ui.isHome) item { FavoritesRow(favs, onOpen, onFav, onShare, onDelete) }
            item { SectionLabel(ui) }
            items(visible, key = { it.id }) { l ->
                LinkRow(l, { onOpen(l) }, { onFav(l) }, { onShare(l) }, { onDelete(l) })
            }
            if (visible.isEmpty() && favs.isEmpty()) item { EmptyHome(ui) }
            if (rescued.isNotEmpty()) {
                item { RescuedHeader(onReroll) }
                items(rescued, key = { "old-${it.id}" }) { l ->
                    LinkRow(l, { onOpen(l) }, { onFav(l) }, { onShare(l) }, { onDelete(l) })
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    } else {
        val cols = if (mode == ViewMode.GRID_LARGE) 1 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalArrangement = Arrangement.spacedBy(GAP),
            modifier = Modifier.fillMaxSize().pointerInput(catIds, ui.cur) {
                var startX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { startX = it.x }, onDragEnd = {}, onDragCancel = {},
                    onHorizontalDrag = { change, _ ->
                        val total = change.position.x - startX
                        if (abs(total) > threshold) {
                            val next = if (total < 0) curIdx + 1 else curIdx - 1
                            if (next in catIds.indices && catIds[next] != ui.cur) onSelectCat(catIds[next])
                            startX = change.position.x
                        }
                    }
                )
            }
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BubbleCarousel(ui, roots, catIds, curIdx, bubbleState, onSelectCat, onOpenDone)
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SubCategoryRow(ui, onSelectSub) }
            item(span = { GridItemSpan(maxLineSpan) }) { ViewModeBar(mode, sort, onViewMode, onSortOrder) }
            if (favs.isNotEmpty() && ui.isHome) {
                item(span = { GridItemSpan(maxLineSpan) }) { FavoritesRow(favs, onOpen, onFav, onShare, onDelete) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel(ui) }
            items(visible, key = { it.id }) { l ->
                LinkCard(l, large = mode == ViewMode.GRID_LARGE,
                    onOpen = { onOpen(l) }, onToggleFav = { onFav(l) }, onShare = { onShare(l) }, onDelete = { onDelete(l) })
            }
            if (visible.isEmpty() && favs.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyHome(ui) }
            }
            if (rescued.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { RescuedHeader(onReroll) }
                items(rescued, key = { "old-${it.id}" }) { l ->
                    LinkCard(l, large = mode == ViewMode.GRID_LARGE,
                        onOpen = { onOpen(l) }, onToggleFav = { onFav(l) }, onShare = { onShare(l) }, onDelete = { onDelete(l) })
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/* ── Piezas extraídas para reutilizar entre LazyColumn y LazyVerticalGrid ── */

@Composable
private fun BubbleCarousel(
    ui: UiState,
    roots: List<com.linkora.data.Category>,
    catIds: List<String?>,
    curIdx: Int,
    state: androidx.compose.foundation.lazy.LazyListState,
    onSelectCat: (String?) -> Unit,
    onOpenDone: () -> Unit
) {
    Column {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
        ) {
            item {
                CategoryBubble("Todo", ui.live.size, ui.cur == null, Accent,
                    Icons.Outlined.Apps) { onSelectCat(null) }
            }
            items(roots.size) { i ->
                val c = roots[i]
                CategoryBubble(c.name, ui.countFor(ui.cats, c.id), ui.cur == c.id,
                    Color(c.color), Glyphs.byKey(c.icon)) { onSelectCat(c.id) }
            }
            if (ui.doneItems.isNotEmpty()) {
                item {
                    CategoryBubble("Hechos", ui.doneItems.size, false, Ok,
                        Icons.Filled.Check, onOpenDone)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        DotIndicators(total = catIds.size, current = curIdx)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SubCategoryRow(ui: UiState, onSelectSub: (String?) -> Unit) {
    val subs = ui.cur?.let { cur -> ui.cats.filter { it.parent == cur } }.orEmpty()
    if (subs.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip("Todo", null, ui.sub == null) { onSelectSub(null) }
            subs.forEach { c ->
                CategoryChip(c.name, ui.countFor(ui.cats, c.id), ui.sub == c.id,
                    icon = Glyphs.byKey(c.icon)) { onSelectSub(c.id) }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun FavoritesRow(
    favs: List<LinkItem>,
    onOpen: (LinkItem) -> Unit,
    onFav: (LinkItem) -> Unit,
    onShare: (LinkItem) -> Unit,
    onDelete: (LinkItem) -> Unit
) {
    Column {
        SectionCaption("Favoritos", favs.size.toString())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            items(favs, key = { it.id }) { l ->
                LinkCard(l, compact = true, onOpen = { onOpen(l) },
                    onToggleFav = { onFav(l) })
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SectionLabel(ui: UiState) {
    val cat = ui.cats.find { it.id == (ui.sub ?: ui.cur) }
    SectionCaption(cat?.name ?: "Recientes")
}

@Composable
private fun EmptyHome(ui: UiState) {
    if (ui.links.isEmpty())
        EmptyState(Icons.Outlined.Link, "Todavía no hay nada aquí",
            "Pulsa + arriba para guardar tu primer link, o compártelo desde cualquier app con Linkora.")
    else
        EmptyState(Icons.Outlined.CheckCircle, "Nada por aquí",
            if (ui.cur != null) "Esta categoría no tiene nada pendiente."
            else "Has marcado todo como hecho. Lo tienes en la pestaña Hechos.")
}

@Composable
private fun RescuedHeader(onReroll: () -> Unit) {
    Column {
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionCaption("Rescatado del archivo", trailing = {
            IconButton(onClick = onReroll, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Refresh, "Otros", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        })
    }
}

/* ═══════════════════════ BUSCAR ═══════════════════════ */
@Composable
fun SearchScreen(
    ui: UiState, onQuery: (String) -> Unit,
    onOpen: (LinkItem) -> Unit, onFav: (LinkItem) -> Unit, onDone: (LinkItem) -> Unit, onShare: (LinkItem) -> Unit, onDelete: (LinkItem) -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val res = ui.visible

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.query, onValueChange = onQuery,
            placeholder = { Text("Buscar links, notas, dominios…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (ui.query.isNotEmpty()) IconButton({ onQuery("") }) { Icon(Icons.Outlined.Close, "Limpiar") } },
            singleLine = true, shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).focusRequester(focus)
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.padding(horizontal = 18.dp)) {
            SectionCaption(if (ui.query.isBlank()) "Escribe para buscar" else "Resultados",
                if (ui.query.isBlank()) null else res.size.toString())
        }
        if (res.isEmpty()) {
            EmptyState(Icons.Outlined.Search,
                if (ui.query.isBlank()) "Busca en toda tu biblioteca" else "Sin resultados",
                if (ui.query.isBlank()) "Por título, nota, nombre de archivo o dominio. También encuentra lo que ya marcaste como hecho."
                else "Nada coincide con «${ui.query}». Prueba con otro término.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(GAP), verticalArrangement = Arrangement.spacedBy(GAP)
            ) {
                items(res, key = { it.id }) { l ->
                    LinkCard(l, onOpen = { onOpen(l) }, onToggleFav = { onFav(l) }, onShare = { onShare(l) }, onDelete = { onDelete(l) })
                }
            }
        }
    }
}

/* ═══════════════════════ HECHOS ═══════════════════════ */
@Composable
fun DoneScreen(ui: UiState, onOpen: (LinkItem) -> Unit, onFav: (LinkItem) -> Unit, onDone: (LinkItem) -> Unit, onShare: (LinkItem) -> Unit, onDelete: (LinkItem) -> Unit) {
    val items = ui.doneItems
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        if (items.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Text("Lo marcado como hecho se elimina solo a los $DONE_TTL_DAYS días. Puedes recuperarlo antes con el mismo botón.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(13.dp))
            }
            SectionCaption("Hechos", items.size.toString())
            LazyVerticalGrid(columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(GAP), verticalArrangement = Arrangement.spacedBy(GAP)) {
                items(items, key = { it.id }) { l ->
                    LinkCard(l, onOpen = { onOpen(l) }, onToggleFav = { onFav(l) }, onShare = { onShare(l) }, onDelete = { onDelete(l) })
                }
            }
        } else {
            EmptyState(Icons.Outlined.CheckCircle, "Nada marcado como hecho",
                "Cuando marques algo, aparecerá aquí durante $DONE_TTL_DAYS días antes de eliminarse.")
        }
    }
}

/* ═══════════════════════ AJUSTES ═══════════════════════ */
@Composable
fun SettingsScreen(ui: UiState, version: String, onCategories: () -> Unit,
                    onExport: () -> Unit, onImport: () -> Unit, onRetryMeta: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        SettingRow(Icons.Outlined.Folder, "Categorías", onCategories)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingRow(Icons.Outlined.Upload, "Exportar copia de seguridad", onExport)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingRow(Icons.Outlined.Download, "Importar copia", onImport)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingRow(Icons.Outlined.Refresh, "Reintentar miniaturas fallidas", onRetryMeta)
        Spacer(Modifier.height(18.dp))
        Text("Linkora $version · ${ui.live.size} elementos activos y ${ui.doneItems.size} marcados como hecho.\n" +
            "Todo se guarda en este dispositivo. La copia incluye links, categorías y archivos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
