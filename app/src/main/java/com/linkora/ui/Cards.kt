package com.linkora.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.linkora.data.Brands
import com.linkora.data.Files
import com.linkora.data.LinkItem

enum class ViewMode { LIST, GRID, GRID_LARGE }
enum class SortOrder { DATE_DESC, DATE_ASC, ALPHA }

/* ═══════════ BURBUJA DE CATEGORÍA ═══════════ */
@Composable
fun CategoryBubble(label: String, count: Int?, selected: Boolean, color: Color,
                    icon: ImageVector? = null, onClick: () -> Unit) {
    val sc by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.65f), label = "bsc")
    val bg = if (selected) color else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(78.dp).clickable(onClick = onClick).scale(sc)) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (count != null) Text(count.toString(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.55f), textAlign = TextAlign.Center)
    }
}

/* ═══════════ INDICADORES DE PUNTOS ═══════════ */
@Composable
fun DotIndicators(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(if (i == current) 16.dp else 6.dp)
                .clip(CircleShape).background(if (i == current) Accent else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

/* ═══════════ BARRA DE MODO DE VISTA ═══════════ */
@Composable
fun ViewModeBar(mode: ViewMode, sort: SortOrder, onMode: (ViewMode) -> Unit, onSort: (SortOrder) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ onMode(ViewMode.LIST) }, Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ViewList, "Lista", tint = if (mode == ViewMode.LIST) Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp)) }
        IconButton({ onMode(ViewMode.GRID) }, Modifier.size(36.dp)) {
            Icon(Icons.Outlined.GridView, "Grid", tint = if (mode == ViewMode.GRID) Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp)) }
        IconButton({ onMode(ViewMode.GRID_LARGE) }, Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ViewAgenda, "Grande", tint = if (mode == ViewMode.GRID_LARGE) Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.weight(1f))
        val next = when (sort) { SortOrder.DATE_DESC -> SortOrder.DATE_ASC; SortOrder.DATE_ASC -> SortOrder.ALPHA; SortOrder.ALPHA -> SortOrder.DATE_DESC }
        val label = when (sort) { SortOrder.DATE_DESC -> "Reciente"; SortOrder.DATE_ASC -> "Antiguo"; SortOrder.ALPHA -> "A-Z" }
        TextButton({ onSort(next) }) { Icon(Icons.Outlined.SwapVert, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/* ═══════════ TARJETA GRID ═══════════ */
@Composable
fun LinkCard(item: LinkItem, compact: Boolean = false, large: Boolean = false,
             onOpen: () -> Unit, onToggleFav: () -> Unit, onShare: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val ctx = LocalContext.current
    val brand = if (item.isFile) Brands.fileBrand(item.fileType) else Brands.byKey(item.brand)
    val alpha by animateFloatAsState(if (item.done) 0.5f else 1f, label = "done")
    Card(onClick = onOpen,
        modifier = Modifier.then(when { compact -> Modifier.width(136.dp); else -> Modifier.fillMaxWidth() }).alpha(alpha),
        shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (compact) 1f else if (large) 1.78f else 1.6f)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(Color(brand.color))) {
            Thumb(item, ctx, brand)
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
            Text(item.displayTitle, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text("${item.subtitle} · ${ago(item.t)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallAction(if (item.fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (item.fav) Heart else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onToggleFav)
                    SmallAction(Icons.Outlined.Share, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onShare)
                    SmallAction(Icons.Outlined.Delete, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onDelete)
                }
            }
        }
    }
}

/* ═══════════ FILA (MODO LISTA) ═══════════ */
@Composable
fun LinkRow(item: LinkItem, onOpen: () -> Unit, onToggleFav: () -> Unit, onShare: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val ctx = LocalContext.current
    val brand = if (item.isFile) Brands.fileBrand(item.fileType) else Brands.byKey(item.brand)
    val alpha by animateFloatAsState(if (item.done) 0.5f else 1f, label = "done")
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().alpha(alpha), shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(Color(brand.color)), contentAlignment = Alignment.Center) { Thumb(item, ctx, brand, small = true) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayTitle, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${item.subtitle} · ${ago(item.t)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SmallAction(if (item.fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (item.fav) Heart else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onToggleFav)
            SmallAction(Icons.Outlined.Share, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onShare)
            SmallAction(Icons.Outlined.Delete, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), onDelete)
        }
    }
}

/* ═══════════ HELPERS ═══════════ */
@Composable private fun SmallAction(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
}
@Composable private fun Thumb(item: LinkItem, ctx: android.content.Context, brand: Brands.Brand, small: Boolean = false) {
    val local = Files.resolve(ctx, item.thumbPath ?: item.filePath)
    when {
        local != null && item.fileType?.startsWith("image/") == true -> AsyncImage(local, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        !item.image.isNullOrBlank() -> AsyncImage(item.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Glyphs.byKey(brand.icon), null, tint = Color.White, modifier = Modifier.size(if (small) 22.dp else 36.dp)) }
    }
}
fun ago(t: Long): String { val s = (System.currentTimeMillis() - t) / 1000; return when {
    s < 60 -> "ahora"; s < 3600 -> "${s/60} min"; s < 86400 -> "${s/3600} h"; s < 604800 -> "${s/86400} d"
    else -> android.text.format.DateFormat.format("d MMM", t).toString() } }

@Composable fun SectionCaption(text: String, count: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count != null) { Spacer(Modifier.width(7.dp)); Text(count, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) }
        Spacer(Modifier.weight(1f)); trailing?.invoke() } }

@Composable fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 54.dp, horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp)); Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } }

@Composable fun CategoryChip(label: String, count: Int?, selected: Boolean, color: Color? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Row(verticalAlignment = Alignment.CenterVertically) { Text(label)
        if (count != null) { Spacer(Modifier.width(6.dp)); Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(0.55f)) } } },
        leadingIcon = when { icon != null -> { { Icon(icon, null, Modifier.size(16.dp)) } }; color != null -> { { Box(Modifier.size(9.dp).clip(CircleShape).background(color)) } }; else -> null },
        shape = MaterialTheme.shapes.small) }
