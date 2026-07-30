package com.linkora.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.linkora.data.Brands
import com.linkora.data.Files
import com.linkora.data.LinkItem
import androidx.compose.ui.platform.LocalContext

/* ═════════════════════ BURBUJA DE CATEGORÍA (estilo Instagram) ═════════════════════ */
@Composable
fun CategoryBubble(
    label: String,
    count: Int?,
    selected: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val sc by animateFloatAsState(if (selected) 1.1f else 1f, spring(dampingRatio = 0.6f), label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .scale(sc)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Anillo exterior: gradiente si activo, gris si no
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(if (selected) RingGradient else RingInactive)
            )
            // Fondo interior (hueco del anillo)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
            // Círculo con el color e icono de la categoría
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                if (count != null) {
                    Text(
                        count.toString(),
                        color = Color.White,
                        fontSize = if (icon != null) 0.sp else 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            // Badge de cantidad (esquina inferior derecha)
            if (count != null && icon != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(22.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(),
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = if (selected) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
            else androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ═════════════════════ INDICADORES DE PUNTOS ═════════════════════ */
@Composable
fun DotIndicators(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Accent,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == current) 18.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (i == current) activeColor else inactiveColor)
            )
        }
    }
}

/* ═════════════════════ TARJETA DE LINK ═════════════════════ */
@Composable
fun LinkCard(
    item: LinkItem,
    compact: Boolean = false,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit,
    onToggleDone: () -> Unit
) {
    val ctx = LocalContext.current
    val brand = if (item.isFile) Brands.fileBrand(item.fileType) else Brands.byKey(item.brand)
    val alpha by animateFloatAsState(if (item.done) 0.5f else 1f, label = "doneAlpha")

    Card(
        onClick = onOpen,
        modifier = Modifier
            .then(if (compact) Modifier.width(136.dp) else Modifier.fillMaxWidth())
            .alpha(alpha),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (compact) 1f else 1.6f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Color(brand.color))
        ) {
            val localThumb = Files.resolve(ctx, item.thumbPath ?: item.filePath)
            when {
                localThumb != null && item.fileType?.startsWith("image/") == true ->
                    AsyncImage(
                        model = localThumb, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                !item.image.isNullOrBlank() ->
                    AsyncImage(
                        model = item.image, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                else -> Icon(
                    Glyphs.byKey(brand.icon), null, tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(if (compact) 30.dp else 36.dp)
                )
            }

            OverlayButton(
                onClick = onToggleFav,
                bg = if (item.fav) Heart else Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(
                    if (item.fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)
                )
            }
            OverlayButton(
                onClick = onToggleDone,
                bg = if (item.done) Ok else Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Text(
                item.displayTitle,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.subtitle} · ${ago(item.t)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverlayButton(
    onClick: () -> Unit,
    bg: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier.size(28.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

fun ago(t: Long): String {
    val s = (System.currentTimeMillis() - t) / 1000
    return when {
        s < 60 -> "ahora"
        s < 3600 -> "${s / 60} min"
        s < 86_400 -> "${s / 3600} h"
        s < 604_800 -> "${s / 86_400} d"
        else -> android.text.format.DateFormat.format("d MMM", t).toString()
    }
}

@Composable
fun SectionCaption(
    text: String,
    count: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count != null) {
            Spacer(Modifier.width(7.dp))
            Text(count, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 54.dp, horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun CategoryChip(
    label: String,
    count: Int?,
    selected: Boolean,
    color: Color? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (count != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.55f))
                }
            }
        },
        leadingIcon = when {
            icon != null -> { { Icon(icon, null, Modifier.size(16.dp)) } }
            color != null -> { { Box(Modifier.size(9.dp).clip(CircleShape).background(color)) } }
            else -> null
        },
        shape = MaterialTheme.shapes.small
    )
}
