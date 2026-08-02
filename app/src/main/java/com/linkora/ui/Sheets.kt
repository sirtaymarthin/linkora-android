package com.linkora.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.linkora.data.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(item: LinkItem, cats: List<Category>, onDismiss: () -> Unit,
                onFav: () -> Unit, onDone: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val brand = if (item.isFile) Brands.fileBrand(item.fileType) else Brands.byKey(item.brand)
    val cat = cats.find { it.id == item.cat }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16/9f).clip(MaterialTheme.shapes.medium).background(Color(brand.color))) {
                val local = Files.resolve(ctx, item.thumbPath ?: item.filePath)
                when {
                    local != null && item.fileType?.startsWith("image/") == true -> AsyncImage(local, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    !item.image.isNullOrBlank() -> AsyncImage(item.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else -> Icon(Glyphs.byKey(brand.icon), null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(46.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(item.displayTitle, style = MaterialTheme.typography.titleMedium)
            if (!item.desc.isNullOrBlank()) { Spacer(Modifier.height(6.dp)); Text(item.desc, maxLines = 3, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (item.note.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("\uD83D\uDCDD ${item.note}", style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(item.subtitle)
                if (item.isFile && item.fileSize > 0) InfoChip("${"%.1f".format(item.fileSize / 1048576f)} MB")
                cat?.let { InfoChip(it.name) }
                InfoChip(ago(item.t))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { openItem(ctx, item) }, modifier = Modifier.weight(1f).height(50.dp), shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Abrir", fontWeight = FontWeight.Bold) }
                Button(onClick = { shareItem(ctx, item, whatsapp = true) }, modifier = Modifier.weight(1f).height(50.dp), shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsApp)) { Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White) }
            }
            Spacer(Modifier.height(10.dp))
            SheetRow(if (item.fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (item.fav) "Quitar de favoritos" else "Añadir a favoritos", tint = if (item.fav) Heart else null, onClick = onFav)
            SheetRow(Icons.Filled.Check, if (item.done) "Volver a pendiente" else "Marcar como hecho", tint = if (item.done) Ok else null, onClick = onDone)
            SheetRow(Icons.Outlined.Share, "Compartir…") { shareItem(ctx, item, false) }
            if (!item.isFile) SheetRow(Icons.Outlined.ContentCopy, "Copiar enlace") { copyUrl(ctx, item) }
            SheetRow(Icons.Outlined.Edit, "Editar", onClick = onEdit)
            SheetRow(Icons.Outlined.Delete, "Eliminar", tint = Danger, onClick = onDelete)
        }
    }
}

@Composable private fun InfoChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }

@Composable private fun SheetRow(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp)); Text(label, color = tint ?: MaterialTheme.colorScheme.onSurface) } }

@OptIn(ExperimentalMaterial3Api::class) @Composable
fun AddSheet(cats: List<Category>, preselectedCat: String?, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var url by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var cat by remember { mutableStateOf(preselectedCat) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Guardar link", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(14.dp))
            OutlinedTextField(url, { url = it }, label = { Text("URL") }, placeholder = { Text("https://…") }, singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp)); CatPicker(cats, cat) { cat = it }; Spacer(Modifier.height(18.dp))
            Button(onClick = { onSave(url, note, cat) }, enabled = url.trim().startsWith("http"), modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.small) { Text("Guardar", fontWeight = FontWeight.ExtraBold) }
        } } }

@OptIn(ExperimentalMaterial3Api::class) @Composable
fun EditSheet(item: LinkItem, cats: List<Category>, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var title by remember { mutableStateOf(if (item.isFile) item.fileName.orEmpty() else item.title.orEmpty()) }
    var note by remember { mutableStateOf(item.note) }; var cat by remember { mutableStateOf(item.cat) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Editar", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(14.dp))
            OutlinedTextField(title, { title = it }, label = { Text(if (item.isFile) "Nombre" else "Título") }, singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Nota") }, singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp)); CatPicker(cats, cat) { cat = it }; Spacer(Modifier.height(18.dp))
            Button(onClick = { onSave(title, note, cat) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.small) { Text("Guardar cambios", fontWeight = FontWeight.ExtraBold) }
        } } }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun CatPicker(cats: List<Category>, selected: String?, onSelect: (String?) -> Unit) {
    Text("CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChip("Sin categoría", null, selected == null) { onSelect(null) }
        cats.forEach { c -> CategoryChip((if (c.parent != null) "· " else "") + c.name, null, selected == c.id, icon = Glyphs.byKey(c.icon)) { onSelect(c.id) } }
    } }

/* ═══════════ ACCIONES DEL SISTEMA ═══════════ */
private fun uriFor(ctx: Context, file: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)

fun openItem(ctx: Context, item: LinkItem) {
    if (!item.isFile) { item.url?.let { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }; return }
    val f = Files.resolve(ctx, item.filePath) ?: return
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uriFor(ctx, f), item.fileType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
}

fun shareItem(ctx: Context, item: LinkItem, whatsapp: Boolean) {
    val base = Intent(Intent.ACTION_SEND)
    if (item.isFile) { val f = Files.resolve(ctx, item.filePath) ?: return; base.type = item.fileType ?: "*/*"
        base.putExtra(Intent.EXTRA_STREAM, uriFor(ctx, f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else { base.type = "text/plain"; base.putExtra(Intent.EXTRA_TEXT, listOfNotNull(item.title, item.url).joinToString("\n")) }
    val target = if (whatsapp) (base.clone() as Intent).setPackage("com.whatsapp") else base
    runCatching { ctx.startActivity(target) }.onFailure { runCatching { ctx.startActivity(Intent.createChooser(base, "Compartir")) } }
}

fun copyUrl(ctx: Context, item: LinkItem) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("url", item.url ?: return))
}
