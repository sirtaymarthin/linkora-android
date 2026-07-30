package com.linkora.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.linkora.data.Brands
import com.linkora.data.Files
import com.linkora.data.LinkItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (compact) 1f else 1.6f)
                .background(Color(brand.color))
        ) {
            val localThumb = Files.resolve(ctx, item.thumbPath ?: item.filePath)
            when {
                localThumb != null && item.fileType?.startsWith("image/") == true ->
                    AsyncImage(
                        model = localThumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                !item.image.isNullOrBlank() ->
                    AsyncImage(
                        model = item.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                else -> Icon(
                    Glyphs.byKey(brand.icon), null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(if (compact) 30.dp else 36.dp)
                )
            }

            // Corazón (favorito) y check (hecho), acciones directas sin abrir la ficha
            OverlayButton(
                onClick = onToggleFav,
                bg = if (item.fav) Heart else Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
            ) {
                Icon(
                    if (item.fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (item.fav) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = Color.White, modifier = Modifier.size(14.dp)
                )
            }
            OverlayButton(
                onClick = onToggleDone,
                bg = if (item.done) Ok else Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (item.done) "Volver a pendiente" else "Marcar como hecho",
                    tint = Color.White, modifier = Modifier.size(15.dp)
                )
            }
        }

        Column(Modifier.padding(start = 11.dp, end = 11.dp, top = 9.dp, bottom = 11.dp)) {
            Text(
                item.displayTitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${item.subtitle} · ${ago(item.t)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        modifier
            .size(26.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick),
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
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count != null) {
            Spacer(Modifier.width(7.dp))
            Text(
                count,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
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
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(46.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.55f)
                    )
                }
            }
        },
        leadingIcon = when {
            icon != null -> { { Icon(icon, null, Modifier.size(16.dp)) } }
            color != null -> {
                {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                }
            }
            else -> null
        },
        shape = MaterialTheme.shapes.small
    )
}
