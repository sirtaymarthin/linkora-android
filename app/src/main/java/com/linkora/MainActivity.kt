package com.linkora

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linkora.data.Category
import com.linkora.data.LinkItem
import com.linkora.ui.*
import com.linkora.vm.MainViewModel
import com.linkora.vm.Tab
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntent = intent
        setContent {
            LinkoraTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LinkoraRoot(
                        shareIntent = pendingIntent,
                        onShareConsumed = { pendingIntent = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }
}

private data class NavEntry(val tab: Tab?, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkoraRoot(shareIntent: Intent?, onShareConsumed: () -> Unit) {
    val vm: MainViewModel = viewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Lo compartido desde otras apps entra por aquí, una sola vez por intent.
    LaunchedEffect(shareIntent) {
        shareIntent?.let { vm.handleShare(it); onShareConsumed() }
    }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val tabs = listOf(Tab.HOME, Tab.SEARCH, Tab.DONE, Tab.SETTINGS)

    var detail by remember { mutableStateOf<LinkItem?>(null) }
    var editing by remember { mutableStateOf<LinkItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var catManager by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { vm.export(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.import(it) } }

    val title = when (ui.tab) {
        Tab.HOME -> ui.cats.find { it.id == (ui.sub ?: ui.cur) }?.name ?: "Home"
        Tab.SEARCH -> "Buscar"
        Tab.DONE -> "Hechos"
        Tab.SETTINGS -> "Ajustes"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    Text(
                        "Linkora ${BuildConfigVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = { adding = true },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(40.dp)
                    ) { Icon(Icons.Filled.Add, "Añadir link") }
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = ui.tab == Tab.HOME,
                    onClick = { vm.setTab(Tab.HOME) },
                    icon = { Icon(Icons.Outlined.Home, null) },
                    label = { Text("Home", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = ui.tab == Tab.SEARCH,
                    onClick = { vm.setTab(Tab.SEARCH) },
                    icon = { Icon(Icons.Outlined.Search, null) },
                    label = { Text("Buscar", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    enabled = ui.undoSize > 0,
                    onClick = { vm.undo() },
                    icon = {
                        BadgedBox(badge = { if (ui.undoSize > 0) Badge { Text("${ui.undoSize}") } }) {
                            Icon(Icons.Outlined.Undo, null)
                        }
                    },
                    label = { Text("Deshacer", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    enabled = ui.redoSize > 0,
                    onClick = { vm.redo() },
                    icon = {
                        BadgedBox(badge = { if (ui.redoSize > 0) Badge { Text("${ui.redoSize}") } }) {
                            Icon(Icons.Outlined.Redo, null)
                        }
                    },
                    label = { Text("Rehacer", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = ui.tab == Tab.SETTINGS,
                    onClick = { vm.setTab(Tab.SETTINGS) },
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text("Ajustes", fontSize = 10.sp) }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (ui.tab) {
                Tab.HOME -> HomeScreen(
                    ui = ui,
                    onSelectCat = vm::selectCat,
                    onSelectSub = vm::selectSub,
                    onOpenDone = { vm.setTab(Tab.DONE) },
                    onReroll = vm::rerollRescued,
                    onOpen = { detail = it },
                    onFav = vm::toggleFav,
                    onDone = { vm.setDone(it, !it.done) }
                )
                Tab.SEARCH -> SearchScreen(
                    ui = ui,
                    onQuery = vm::setQuery,
                    onOpen = { detail = it },
                    onFav = vm::toggleFav,
                    onDone = { vm.setDone(it, !it.done) }
                )
                Tab.DONE -> DoneScreen(
                    ui = ui,
                    onOpen = { detail = it },
                    onFav = vm::toggleFav,
                    onDone = { vm.setDone(it, !it.done) }
                )
                Tab.SETTINGS -> SettingsScreen(
                    ui = ui,
                    version = BuildConfigVersion,
                    onCategories = { catManager = true },
                    onExport = { exportLauncher.launch("linkora-copia.zip") },
                    onImport = { importLauncher.launch(arrayOf("application/zip")) },
                    onRetryMeta = vm::retryMeta
                )
            }
        }
    }

    detail?.let { item ->
        val fresh = ui.links.find { it.id == item.id } ?: item
        DetailSheet(
            item = fresh,
            cats = ui.cats,
            onDismiss = { detail = null },
            onFav = { vm.toggleFav(fresh) },
            onDone = { vm.setDone(fresh, !fresh.done); detail = null },
            onEdit = { detail = null; editing = fresh },
            onDelete = { vm.delete(fresh); detail = null }
        )
    }

    editing?.let { item ->
        EditSheet(
            item = item,
            cats = ui.cats,
            onDismiss = { editing = null },
            onSave = { t, n, c -> vm.saveEdit(item, t, n, c); editing = null }
        )
    }

    if (adding) {
        AddSheet(
            cats = ui.cats,
            preselectedCat = ui.sub ?: ui.cur,
            onDismiss = { adding = false },
            onSave = { url, note, cat -> vm.addUrl(url, note, cat); adding = false }
        )
    }

    if (catManager) {
        CategoryManagerSheet(
            cats = ui.cats,
            countOf = { id -> ui.countFor(ui.cats, id) },
            onDismiss = { catManager = false },
            onSave = vm::saveCat,
            onDelete = vm::deleteCat,
            onMove = vm::moveCat
        )
    }
}

/** Versión mostrada junto al logo. Un único sitio que tocar en cada release. */
const val BuildConfigVersion = "v1.5.0"
