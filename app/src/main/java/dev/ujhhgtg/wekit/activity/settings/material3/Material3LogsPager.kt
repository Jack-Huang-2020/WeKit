package dev.ujhhgtg.wekit.activity.settings.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Expand_more
import com.composables.icons.materialsymbols.outlined.More_vert
import com.composables.icons.materialsymbols.outlined.Refresh
import com.composables.icons.materialsymbols.outlined.Save
import com.composables.icons.materialsymbols.outlined.Share
import com.composables.icons.materialsymbols.outlined.Vertical_align_bottom
import com.composables.icons.materialsymbols.outlined.Vertical_align_top
import dev.ujhhgtg.wekit.activity.settings.CONTENT_BOTTOM_INSET
import dev.ujhhgtg.wekit.activity.settings.LocalComponentActivity
import dev.ujhhgtg.wekit.activity.settings.miuix.CrashSection
import dev.ujhhgtg.wekit.activity.settings.miuix.LOG_TABS
import dev.ujhhgtg.wekit.activity.settings.miuix.LogKind
import dev.ujhhgtg.wekit.activity.settings.miuix.RUN_LOG_COLLAPSE_LINES
import dev.ujhhgtg.wekit.activity.settings.miuix.RunLogEntry
import dev.ujhhgtg.wekit.activity.settings.miuix.levelColor
import dev.ujhhgtg.wekit.activity.settings.miuix.parseCrashLog
import dev.ujhhgtg.wekit.activity.settings.miuix.parseRunLog
import dev.ujhhgtg.wekit.activity.settings.miuix.readLog
import dev.ujhhgtg.wekit.activity.settings.miuix.saveLogFile
import dev.ujhhgtg.wekit.activity.settings.miuix.shareLogFile
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.showToastSuspend
import dev.ujhhgtg.wekit.utils.crash.CrashLogsManager
import dev.ujhhgtg.wekit.utils.formatBytesSize
import dev.ujhhgtg.wekit.utils.formatEpoch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import androidx.compose.animation.core.tween as animTween

// ---------------------------------------------------------------------------
//  Page 2 — Logs (Material 3)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3LogsPager() {
    val context = LocalComponentActivity.current
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val kind = LOG_TABS[selectedTab].second

    val runListState = rememberLazyListState()
    val crashListState = rememberLazyListState()
    val listState = if (kind == LogKind.RUN) runListState else crashListState

    var refreshKey by remember { mutableIntStateOf(0) }
    var currentFile by remember { mutableStateOf<Path?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            // Material 3 TopAppBar doesn't have a bottomContent slot, so we combine it with
            // the tab row in a Column — the whole column is the topBar slot.
            Column {
                TopAppBar(
                    title = { Text("日志") },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = {
                            currentFile?.let { shareLogFile(context, it) }
                                ?: scope.launch { showToastSuspend("暂无可分享的日志") }
                        }) {
                            Icon(MaterialSymbols.Outlined.Share, "分享")
                        }
                        IconButton(onClick = {
                            currentFile?.let { saveLogFile(context, it) }
                                ?: scope.launch { showToastSuspend("暂无可保存的日志") }
                        }) {
                            Icon(MaterialSymbols.Outlined.Save, "保存")
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) {
                                Icon(MaterialSymbols.Outlined.More_vert, "菜单")
                            }
                            DropdownMenu(
                                expanded = overflowOpen,
                                onDismissRequest = { overflowOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("刷新") },
                                    leadingIcon = { Icon(MaterialSymbols.Outlined.Refresh, null) },
                                    onClick = { overflowOpen = false; refreshKey++ },
                                )
                                DropdownMenuItem(
                                    text = { Text("转到顶部") },
                                    leadingIcon = { Icon(MaterialSymbols.Outlined.Vertical_align_top, null) },
                                    onClick = {
                                        overflowOpen = false
                                        scope.launch { listState.animateScrollToItem(0) }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("转到底部") },
                                    leadingIcon = { Icon(MaterialSymbols.Outlined.Vertical_align_bottom, null) },
                                    onClick = {
                                        overflowOpen = false
                                        scope.launch {
                                            val end = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                            listState.animateScrollToItem(end)
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
                SecondaryTabRow(selectedTab) {
                    LOG_TABS.forEachIndexed { index, (label, _) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Crossfade(targetState = kind, animationSpec = tween(200), label = "logKind") { k ->
            M3LogTabContent(
                kind = k,
                listState = if (k == LogKind.RUN) runListState else crashListState,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                refreshKey = refreshKey,
                isRefreshing = isRefreshing,
                onRefreshingChange = { isRefreshing = it },
                onRefreshRequested = { refreshKey++ },
                onCurrentFileChange = { if (k == kind) currentFile = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3LogTabContent(
    kind: LogKind,
    listState: LazyListState,
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    refreshKey: Int,
    isRefreshing: Boolean,
    onRefreshingChange: (Boolean) -> Unit,
    onRefreshRequested: () -> Unit,
    onCurrentFileChange: (Path?) -> Unit,
) {
    var files by remember(kind) { mutableStateOf<List<Path>>(emptyList()) }
    var selectedIndex by rememberSaveable(kind) { mutableIntStateOf(0) }
    var runEntries by remember(kind) { mutableStateOf<List<RunLogEntry>>(emptyList()) }
    var crashSections by remember(kind) { mutableStateOf<List<CrashSection>>(emptyList()) }
    var loading by remember(kind) { mutableStateOf(true) }
    var listed by remember(kind) { mutableStateOf(false) }

    LaunchedEffect(kind, refreshKey) {
        val result = withContext(Dispatchers.IO) {
            when (kind) {
                LogKind.RUN   -> WeLogger.allLogFiles
                LogKind.CRASH -> CrashLogsManager.allCrashLogs
            }
        }
        files = result
        if (selectedIndex >= result.size) selectedIndex = 0
        listed = true
    }

    val selectedFile = files.getOrNull(selectedIndex)
    LaunchedEffect(selectedFile) { onCurrentFileChange(selectedFile) }

    LaunchedEffect(selectedFile, refreshKey, listed) {
        loading = true
        if (selectedFile == null) {
            runEntries = emptyList(); crashSections = emptyList()
            if (listed) { loading = false; onRefreshingChange(false) }
            return@LaunchedEffect
        }
        val text = withContext(Dispatchers.IO) { readLog(selectedFile) }
        when (kind) {
            LogKind.RUN   -> runEntries   = withContext(Dispatchers.Default) { parseRunLog(text) }
            LogKind.CRASH -> crashSections = withContext(Dispatchers.Default) { parseCrashLog(text) }
        }
        loading = false
        onRefreshingChange(false)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing || loading,
        onRefresh = { onRefreshingChange(true); onRefreshRequested() },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "picker") {
                M3FileSelector(
                    files = files,
                    selectedIndex = selectedIndex.coerceIn(0, (files.size - 1).coerceAtLeast(0)),
                    onSelected = { selectedIndex = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (files.isEmpty()) {
                if (listed) {
                    item(key = "empty-files") {
                        M3LogsEmpty(if (kind == LogKind.RUN) "暂无运行日志" else "暂无崩溃日志")
                    }
                }
            } else when (kind) {
                LogKind.RUN -> {
                    if (runEntries.isEmpty() && !loading) {
                        item(key = "empty-run") { M3LogsEmpty("此日志文件为空") }
                    }
                    items(runEntries.size, key = { "run-$it" }) { i -> M3RunLogCard(runEntries[i]) }
                }
                LogKind.CRASH -> {
                    if (crashSections.isEmpty() && !loading) {
                        item(key = "empty-crash") { M3LogsEmpty("此日志文件为空") }
                    }
                    items(crashSections.size, key = { "crash-$it" }) { i -> M3CrashSectionCard(crashSections[i]) }
                }
            }

            item(key = "bottom-inset") { Spacer(Modifier.height(CONTENT_BOTTOM_INSET)) }
        }
    }
}

@Composable
private fun M3FileSelector(
    files: List<Path>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) return
    var menuOpen by remember { mutableStateOf(false) }
    val labels = remember(files) {
        files.map { "${it.name}  ·  ${formatBytesSize(runCatching { it.fileSize() }.getOrDefault(0))}" }
    }
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Box {
            ListItem(
                headlineContent = { Text("选择日志文件") },
                supportingContent = {
                    Text(
                        files.getOrNull(selectedIndex)
                            ?.let { formatEpoch(it.getLastModifiedTime().toMillis(), true) }
                            ?: labels.getOrElse(selectedIndex) { "" }
                    )
                },
                trailingContent = { Icon(MaterialSymbols.Outlined.Expand_more, null) },
                modifier = Modifier.fillMaxWidth(),
            )
            // Invisible clickable overlay so the whole row opens the menu
            Surface(
                onClick = { menuOpen = true },
                color = Color.Transparent,
                modifier = Modifier.matchParentSize(),
            ) {}
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                labels.forEachIndexed { i, label ->
                    DropdownMenuItem(
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelected(i); menuOpen = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun M3RunLogCard(entry: RunLogEntry) {
    val lines = remember(entry.message) { entry.message.split("\n") }
    val isLong = lines.size > RUN_LOG_COLLAPSE_LINES
    val head = remember(lines) { lines.take(RUN_LOG_COLLAPSE_LINES).joinToString("\n") }
    val rest = remember(lines) { lines.drop(RUN_LOG_COLLAPSE_LINES).joinToString("\n") }
    var expanded by remember(entry) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = animTween(250),
        label = "chevron",
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                entry.level?.let { level ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelColor(level),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                    ) {
                        Text(
                            level.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    entry.tag?.let {
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    entry.time?.let {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isLong) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Outlined.Expand_more,
                            contentDescription = if (expanded) "折叠" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).rotate(chevronRotation),
                        )
                    }
                }
            }
            if (entry.message.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Column {
                        Text(
                            text = if (isLong) head else entry.message,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        if (isLong) {
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Text(text = rest, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3CrashSectionCard(section: CrashSection) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            if (section.title.isNotEmpty()) {
                Text(
                    text = section.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }
            SelectionContainer {
                Text(text = section.body, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun M3LogsEmpty(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
