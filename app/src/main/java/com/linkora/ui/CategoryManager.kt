package com.linkora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linkora.data.Category
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(
    cats: List<Category>,
    countOf: (String) -> Int,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onMove: (Category, Int) -> Unit
) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text("Categorías", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))

            val roots = cats.filter { it.parent == null }
            roots.forEach { root ->
                CatRow(root, countOf(root.id), false,
                    onUp = { onMove(root, -1) }, onDown = { onMove(root, 1) },
                    onEdit = { editing = root }, onDelete = { onDelete(root) })
                cats.filter { it.parent == root.id }.forEach { child ->
                    CatRow(child, countOf(child.id), true,
                        onUp = null, onDown = null,
                        onEdit = { editing = child }, onDelete = { onDelete(child) })
                }
            }
            if (roots.isEmpty()) {
                Text(
                    "Sin categorías todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { creating = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small
            ) { Text("Nueva categoría", fontWeight = FontWeight.ExtraBold) }
        }
    }

    if (creating || editing != null) {
        CategoryFormSheet(
            initial = editing,
            parents = cats.filter { it.parent == null && it.id != editing?.id },
            onDismiss = { creating = false; editing = null },
            onSave = { c -> onSave(c); creating = false; editing = null }
        )
    }
}

@Composable
private fun CatRow(
    cat: Category,
    count: Int,
    child: Boolean,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (child) 24.dp else 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(Color(cat.color)),
            contentAlignment = Alignment.Center
        ) { Icon(Glyphs.byKey(cat.icon), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cat.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$count ${if (count == 1) "elemento" else "elementos"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        onUp?.let { IconButton(it) { Icon(Icons.Outlined.KeyboardArrowUp, "Subir") } }
        onDown?.let { IconButton(it) { Icon(Icons.Outlined.KeyboardArrowDown, "Bajar") } }
        IconButton(onEdit) { Icon(Icons.Outlined.Edit, "Editar") }
        IconButton(onDelete) { Icon(Icons.Outlined.Delete, "Eliminar", tint = Danger) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormSheet(
    initial: Category?,
    parents: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: Glyphs.pickable.first()) }
    var color by remember { mutableStateOf(initial?.color ?: Glyphs.palette.first()) }
    var parent by remember { mutableStateOf(initial?.parent) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                if (initial == null) "Nueva categoría" else "Editar categoría",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre") }, placeholder = { Text("Recetas, viajes…") },
                singleLine = true, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("ICONO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRowSimple {
                Glyphs.pickable.forEach { key ->
                    val on = key == icon
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (on) Color(color) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Glyphs.byKey(key), null,
                            tint = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRowSimple {
                Glyphs.palette.forEach { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color(c))
                            .border(
                                width = if (c == color) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { color = c }
                    )
                }
            }

            if (parents.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("DENTRO DE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRowSimple {
                    CategoryChip("Ninguna", null, parent == null) { parent = null }
                    parents.forEach { p ->
                        CategoryChip(p.name, null, parent == p.id, icon = Glyphs.byKey(p.icon)) { parent = p.id }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(
                        (initial ?: Category(id = UUID.randomUUID().toString(), name = name))
                            .copy(name = name, icon = icon, color = color, parent = parent)
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small
            ) { Text(if (initial == null) "Crear" else "Guardar", fontWeight = FontWeight.ExtraBold) }
        }
    }
}
