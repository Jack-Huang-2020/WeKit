package dev.ujhhgtg.wekit.ui.agent.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Visibility
import com.composables.icons.materialsymbols.outlined.Visibility_off
import dev.ujhhgtg.wekit.agent.data.WeAgentRepository
import dev.ujhhgtg.wekit.agent.data.WeAgentSettings
import dev.ujhhgtg.wekit.agent.data.entity.WorkspaceEntity
import dev.ujhhgtg.wekit.agent.net.ExternalServiceId
import dev.ujhhgtg.wekit.agent.tool.BuiltinToolProvider
import dev.ujhhgtg.wekit.agent.tool.ToolMode
import dev.ujhhgtg.wekit.agent.workspace.WorkspaceStore
import dev.ujhhgtg.wekit.utils.android.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ---------------------------------------------------------------------------
//  Memory Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3MemoryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf<MemoryIndex?>(null) }

    LaunchedEffect(Unit) {
        enabled = WeAgentSettings.memoryEnabled()
        index = withContext(Dispatchers.IO) { parseMemoryIndex() }
        loaded = true
    }

    Material3AgentSettingsScaffold(title = "记忆", onBack = onBack) {
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                M3AgentSwitchItem(
                    title = "启用记忆",
                    summary = "允许 AI 通过 /memory/ 读写持久记忆（MEMORY.md 索引 + 记忆文件）",
                    checked = enabled,
                    onCheckedChange = { on ->
                        enabled = on
                        scope.launch {
                            WeAgentSettings.set(WeAgentSettings.KEY_MEMORY_ENABLED, on.toString())
                            BuiltinToolProvider.fsToolsVisible = on || WeAgentSettings.workspaceEnabled()
                        }
                    },
                )
            }
        }

        if (!loaded) {
            item { M3AgentEmptyHint("加载中…") }
            return@Material3AgentSettingsScaffold
        }

        item { M3AgentSectionTitle("记忆索引（MEMORY.md）") }
        val idx = index
        when {
            idx == null || idx.parseFailed -> item {
                OutlinedCard(Modifier.fillMaxWidth().padding(bottom = AGENT_CONTENT_BOTTOM_INSET)) {
                    Text(
                        "⚠ 记忆索引解析失败，仅影响此处的展示，不影响 AI 使用记忆的能力。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            idx.entries.isEmpty() -> item { M3AgentEmptyHint("记忆索引为空。AI 会在需要时自行写入记忆。") }
            else -> item {
                OutlinedCard(Modifier.fillMaxWidth().padding(bottom = AGENT_CONTENT_BOTTOM_INSET)) {
                    idx.entries.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentArrowItem(title = e.title, summary = e.description)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  External Services Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3ExternalServicesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var exaKey by remember { mutableStateOf("") }
    var braveKey by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exaKey = WeAgentRepository.getExternalServiceKey(ExternalServiceId.EXA) ?: ""
        braveKey = WeAgentRepository.getExternalServiceKey(ExternalServiceId.BRAVE) ?: ""
        loaded = true
    }

    Material3AgentSettingsScaffold(title = "外部服务", onBack = onBack) {
        if (!loaded) {
            item { M3AgentEmptyHint("加载中…") }
            return@Material3AgentSettingsScaffold
        }
        item {
            M3ServiceKeyCard(
                title = "Exa Search",
                description = "AI 语义搜索，需要 Exa API Key（exa.ai）",
                key = exaKey,
                onKeyChange = { exaKey = it },
                onSave = {
                    scope.launch {
                        WeAgentRepository.setExternalServiceKey(ExternalServiceId.EXA, exaKey)
                        BuiltinToolProvider.exaKeyPresent = exaKey.isNotBlank()
                    }
                },
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            M3ServiceKeyCard(
                title = "Brave Search",
                description = "隐私优先的网络搜索，需要 Brave Search API Key（api.search.brave.com）",
                key = braveKey,
                onKeyChange = { braveKey = it },
                onSave = {
                    scope.launch {
                        WeAgentRepository.setExternalServiceKey(ExternalServiceId.BRAVE, braveKey)
                        BuiltinToolProvider.braveKeyPresent = braveKey.isNotBlank()
                    }
                },
            )
        }
        item { Spacer(Modifier.height(AGENT_CONTENT_BOTTOM_INSET)) }
    }
}

@Composable
private fun M3ServiceKeyCard(
    title: String,
    description: String,
    key: String,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showKey by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = key,
                onValueChange = onKeyChange,
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) MaterialSymbols.Outlined.Visibility_off else MaterialSymbols.Outlined.Visibility,
                            contentDescription = if (showKey) "隐藏" else "显示",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (key.isNotBlank()) {
                    Button(onClick = { onKeyChange(""); onSave() }, modifier = Modifier.width(80.dp)) { Text("清除") }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = onSave, modifier = Modifier.width(80.dp)) { Text("保存") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Workspaces Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3WorkspacesScreen(onBack: () -> Unit) {
    val workspaces by WeAgentRepository.observeWorkspaces().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorkspaceEntity?>(null) }

    Material3AgentSettingsScaffold(title = "工作区", onBack = onBack) {
        if (workspaces.isEmpty()) item { M3AgentEmptyHint("还没有工作区。会话可绑定一个工作区以启用文件读写。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                workspaces.forEachIndexed { i, w ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentArrowItem(title = w.name, summary = "/workspace/ → ${w.name}", onClick = { editing = w })
                }
            }
        }
        item {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = AGENT_CONTENT_BOTTOM_INSET),
            ) { Text("添加工作区") }
        }
    }

    if (showAdd) {
        M3WorkspaceNameDialog(
            title = "添加工作区",
            initialName = "",
            label = "工作区名称（同时作为目录名）",
            confirmText = "添加",
            onDismiss = { showAdd = false },
            onConfirm = { name ->
                when (val v = WorkspaceStore.validateWorkspaceName(name)) {
                    is WorkspaceStore.NameValidation.Invalid -> showToast(v.reason)
                    WorkspaceStore.NameValidation.Ok -> scope.launch {
                        val n = name.trim()
                        WorkspaceStore.workspaceDir(n)
                        WeAgentRepository.upsertWorkspace(WorkspaceEntity(UUID.randomUUID().toString(), n))
                    }
                }
                showAdd = false
            },
        )
    }

    editing?.let { w ->
        M3WorkspaceNameDialog(
            title = "编辑工作区",
            initialName = w.name,
            label = "工作区名称（会重命名真实文件夹）",
            confirmText = "保存",
            onDismiss = { editing = null },
            onDelete = { scope.launch { WeAgentRepository.deleteWorkspace(w.id) }; editing = null },
            onConfirm = { newName ->
                when (val v = WorkspaceStore.validateWorkspaceName(newName)) {
                    is WorkspaceStore.NameValidation.Invalid -> showToast(v.reason)
                    WorkspaceStore.NameValidation.Ok -> scope.launch {
                        if (WorkspaceStore.renameWorkspaceDir(w.name, newName.trim())) {
                            WeAgentRepository.upsertWorkspace(w.copy(name = newName.trim()))
                        } else {
                            showToast("重命名失败：目标名称已存在或无效")
                        }
                    }
                }
                editing = null
            },
        )
    }
}

@Composable
private fun M3WorkspaceNameDialog(
    title: String,
    initialName: String,
    label: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    androidx.compose.material3.TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  Builtin Providers Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3BuiltinProvidersScreen(
    onBack: () -> Unit,
    onOpenProvider: (providerId: String, name: String) -> Unit,
) {
    Material3AgentSettingsScaffold(title = "内置工具", onBack = onBack) {
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                BuiltinToolProvider.all.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentArrowItem(
                        title = p.name,
                        summary = "${p.id} · ${p.seedInfos().size} 个工具",
                        onClick = { onOpenProvider(p.id, p.name) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(AGENT_CONTENT_BOTTOM_INSET)) }
    }
}

// ---------------------------------------------------------------------------
//  Tool Permission List Screen (Material 3) — four-state per-tool dropdown
// ---------------------------------------------------------------------------

private val M3_MODE_ORDER = listOf(
    ToolMode.ENABLED, ToolMode.MANUAL_APPROVAL, ToolMode.SMART_APPROVAL, ToolMode.DISABLED,
)

private fun ToolMode.m3Label(): String = when (this) {
    ToolMode.ENABLED -> "直接允许"
    ToolMode.MANUAL_APPROVAL -> "手动审批"
    ToolMode.SMART_APPROVAL -> "智能审批"
    ToolMode.DISABLED -> "禁用"
}

@Composable
fun Material3ToolPermissionListScreen(
    title: String,
    providerId: String,
    tools: List<Pair<String, ToolMode>>,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val perms by WeAgentRepository.observeToolPermissions().collectAsState(initial = emptyList())
    val permMap = perms.associate { (it.providerId to it.toolName) to it.mode }

    Material3AgentSettingsScaffold(title = title, onBack = onBack) {
        if (tools.isEmpty()) item { M3AgentEmptyHint("该提供方暂无可用工具。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                tools.forEachIndexed { i, (name, default) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    val mode = permMap[providerId to name] ?: default
                    M3AgentIndexDropdownItem(
                        title = name,
                        items = M3_MODE_ORDER.map { it.m3Label() },
                        selectedIndex = M3_MODE_ORDER.indexOf(mode).coerceAtLeast(0),
                        onSelected = { scope.launch { WeAgentRepository.setToolMode(providerId, name, M3_MODE_ORDER[it]) } },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(AGENT_CONTENT_BOTTOM_INSET)) }
    }
}
