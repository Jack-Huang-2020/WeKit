package dev.ujhhgtg.wekit.ui.agent.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.wekit.agent.data.WeAgentRepository
import dev.ujhhgtg.wekit.agent.data.entity.McpTransport
import dev.ujhhgtg.wekit.agent.data.entity.ModelEntity
import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderEntity
import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderType
import dev.ujhhgtg.wekit.agent.data.entity.ProviderEntity
import dev.ujhhgtg.wekit.agent.mcp.McpClientManager
import dev.ujhhgtg.wekit.agent.model.ModelProviderManager
import dev.ujhhgtg.wekit.agent.tool.ProviderKind
import dev.ujhhgtg.wekit.agent.tool.ToolMode
import dev.ujhhgtg.wekit.utils.android.showToast
import kotlinx.coroutines.launch
import java.util.UUID

// Shared with ToolPermissionsScreen (same package, internal visibility is fine)
private val EFFORT_GEARS = listOf("off", "minimal", "low", "medium", "high", "xhigh", "max")

// ---------------------------------------------------------------------------
//  Model Providers Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3ModelProvidersScreen(onBack: () -> Unit, onOpenProvider: (String) -> Unit) {
    val providers by WeAgentRepository.observeModelProviders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Material3AgentSettingsScaffold(title = "模型提供方", onBack = onBack) {
        if (providers.isEmpty()) item { M3AgentEmptyHint("还没有模型提供方，点击下方按钮添加。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                providers.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentArrowItem(
                        title = p.name.ifBlank { p.baseUrl },
                        summary = "${p.type.label()} · ${p.baseUrl}",
                        onClick = { onOpenProvider(p.id) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = AGENT_CONTENT_BOTTOM_INSET),
            ) { Text("添加提供方") }
        }
    }

    if (showAdd) {
        M3AddProviderDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, type, baseUrl, apiKey ->
                scope.launch {
                    WeAgentRepository.upsertModelProvider(
                        ModelProviderEntity(UUID.randomUUID().toString(), type, name, baseUrl, apiKey)
                    )
                }
                showAdd = false
            },
        )
    }
}

@Composable
private fun M3AddProviderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: ModelProviderType, baseUrl: String, apiKey: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf("") }
    var typeIndex by remember { mutableIntStateOf(0) }
    val types = listOf(ModelProviderType.OPENAI_CHAT_COMPLETION, ModelProviderType.OPENAI_RESPONSES, ModelProviderType.ANTHROPIC_MESSAGES)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型提供方") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                M3AgentIndexDropdownItem(
                    title = "接口类型",
                    items = types.map { it.label() },
                    selectedIndex = typeIndex,
                    onSelected = { typeIndex = it },
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onConfirm(name.ifBlank { baseUrl }, types[typeIndex], baseUrl, apiKey) },
                    enabled = baseUrl.isNotBlank(),
                ) { Text("添加") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  Model Provider Detail Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3ModelProviderDetailScreen(providerId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf<ModelProviderEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    LaunchedEffect(providerId) {
        val fresh = WeAgentRepository.getDecryptedModelProvider(providerId)
        provider = fresh
        if (fresh != null) { name = fresh.name; baseUrl = fresh.baseUrl; apiKey = fresh.apiKey }
    }

    val models by WeAgentRepository.observeModelsForProvider(providerId).collectAsState(initial = emptyList())
    var editingModel by remember { mutableStateOf<ModelEntity?>(null) }
    var importCandidates by remember { mutableStateOf<List<String>?>(null) }
    var importing by remember { mutableStateOf(false) }

    val p = provider

    Material3AgentSettingsScaffold(title = p?.name ?: "提供方", onBack = onBack) {
        if (p == null) { item { M3AgentEmptyHint("加载中…") }; return@Material3AgentSettingsScaffold }

        item { M3AgentSectionTitle("连接") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { scope.launch { WeAgentRepository.deleteModelProvider(p.id); onBack() } },
                            modifier = Modifier.weight(1f),
                        ) { Text("删除提供方", color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                scope.launch {
                                    WeAgentRepository.upsertModelProvider(p.copy(name = name, baseUrl = baseUrl, apiKey = apiKey))
                                    ModelProviderManager.invalidate(p.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("保存") }
                    }
                }
            }
        }

        item { M3AgentSectionTitle("模型") }
        if (models.isEmpty()) item { M3AgentEmptyHint("还没有模型。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                models.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentArrowItem(
                        title = m.displayName.ifBlank { m.modelIdRemote },
                        summary = "id=${m.modelIdRemote}" +
                            (m.reasoningEffort?.let { " · effort=$it" } ?: "") +
                            (m.contextWindow?.let { " · ctx=$it" } ?: "") +
                            (m.maxTokens?.let { " · max=$it" } ?: "") +
                            if (m.supportsVision) " · 视觉" else "",
                        onClick = { editingModel = m },
                    )
                }
            }
        }
        item {
            Button(
                onClick = { editingModel = ModelEntity("", providerId, "", null, null, "", null) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("添加模型") }
        }
        if (p.type != ModelProviderType.ANTHROPIC_MESSAGES) {
            item {
                TextButton(
                    onClick = {
                        importing = true
                        scope.launch {
                            val result = ModelProviderManager.listRemoteModels(
                                p.copy(name = name, baseUrl = baseUrl, apiKey = apiKey)
                            )
                            importing = false
                            result.fold(
                                onSuccess = { importCandidates = it },
                                onFailure = { showToast("获取失败：${it.message}") },
                            )
                        }
                    },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth().padding(bottom = AGENT_CONTENT_BOTTOM_INSET),
                ) { Text(if (importing) "获取模型列表中…" else "自动导入模型") }
            }
        }
    }

    importCandidates?.let { candidates ->
        M3ImportModelsDialog(
            candidates = candidates,
            existingRemoteIds = models.map { it.modelIdRemote }.toSet(),
            onDismiss = { importCandidates = null },
            onImport = { picked ->
                scope.launch {
                    val added = WeAgentRepository.importModels(providerId, picked)
                    showToast("已导入 $added 个模型")
                }
                importCandidates = null
            },
        )
    }

    editingModel?.let { m ->
        M3ModelDialog(
            existing = m,
            onDismiss = { editingModel = null },
            onDelete = m.id.takeIf { it.isNotEmpty() }
                ?.let { { scope.launch { WeAgentRepository.deleteModel(it) }; editingModel = null } },
            onSave = { remoteId, display, effort, customJson, contextWindow, maxTokens, supportsVision ->
                scope.launch {
                    WeAgentRepository.upsertModel(
                        m.copy(
                            id = m.id.ifEmpty { UUID.randomUUID().toString() },
                            providerId = providerId,
                            modelIdRemote = remoteId,
                            reasoningEffort = effort,
                            customJsonOverride = customJson,
                            displayName = display.ifBlank { remoteId },
                            contextWindow = contextWindow,
                            maxTokens = maxTokens,
                            supportsVision = supportsVision,
                        )
                    )
                }
                editingModel = null
            },
        )
    }
}

@Composable
private fun M3ImportModelsDialog(
    candidates: List<String>,
    existingRemoteIds: Set<String>,
    onDismiss: () -> Unit,
    onImport: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(candidates.filter { it !in existingRemoteIds }) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入模型（${candidates.size}）") },
        text = {
            if (candidates.isEmpty()) {
                Text("该提供方未返回任何模型。")
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(candidates.size, key = { candidates[it] }) { i ->
                        val id = candidates[i]
                        val already = id in existingRemoteIds
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !already) {
                                    if (id in selected) selected.remove(id) else selected.add(id)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (already || id in selected) "☑" else "☐", modifier = Modifier.width(28.dp))
                            Text(
                                id + if (already) "（已添加）" else "",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onImport(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                ) { Text("导入（${selected.size}）") }
            }
        },
    )
}

@Composable
private fun M3ModelDialog(
    existing: ModelEntity,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (remoteId: String, display: String, effort: String?, customJson: String?, contextWindow: Int?, maxTokens: Int?, supportsVision: Boolean) -> Unit,
) {
    var remoteId by remember { mutableStateOf(existing.modelIdRemote) }
    var display by remember { mutableStateOf(existing.displayName) }
    var customJson by remember { mutableStateOf(existing.customJsonOverride.orEmpty()) }
    var contextWindow by remember { mutableStateOf(existing.contextWindow?.toString().orEmpty()) }
    var maxTokens by remember { mutableStateOf(existing.maxTokens?.toString().orEmpty()) }
    var supportsVision by remember { mutableStateOf(existing.supportsVision) }
    var effortIndex by remember { mutableIntStateOf(EFFORT_GEARS.indexOf(existing.reasoningEffort ?: "off").coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing.id.isEmpty()) "添加模型" else "编辑模型") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = remoteId, onValueChange = { remoteId = it }, label = { Text("模型 ID（传给 API）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = display, onValueChange = { display = it }, label = { Text("显示名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                M3AgentIndexDropdownItem(
                    title = "思考强度",
                    items = EFFORT_GEARS,
                    selectedIndex = effortIndex,
                    onSelected = { effortIndex = it },
                )
                OutlinedTextField(
                    value = contextWindow,
                    onValueChange = { v -> contextWindow = v.filter { it.isDigit() }.take(9) },
                    label = { Text("上下文窗口 (token, 可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { v -> maxTokens = v.filter { it.isDigit() }.take(9) },
                    label = { Text("最大输出 token (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = customJson, onValueChange = { customJson = it }, label = { Text("自定义 JSON 透传（可选）") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
                M3AgentSwitchItem(
                    title = "支持视觉（图片输入）",
                    summary = "开启后 AI 才能使用 ui-screenshot 截图工具查看界面",
                    checked = supportsVision,
                    onCheckedChange = { supportsVision = it },
                )
            }
        },
        confirmButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = {
                        val effort = EFFORT_GEARS[effortIndex].takeIf { it != "off" }
                        onSave(remoteId, display, effort, customJson.ifBlank { null }, contextWindow.toIntOrNull(), maxTokens.toIntOrNull(), supportsVision)
                    },
                    enabled = remoteId.isNotBlank(),
                ) { Text("保存") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  MCP Servers Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3McpServersScreen(onBack: () -> Unit, onOpenServer: (String) -> Unit) {
    val allProviders by WeAgentRepository.observeProviders().collectAsState(initial = emptyList())
    val servers = allProviders.filter { it.kind == ProviderKind.MCP }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Material3AgentSettingsScaffold(title = "MCP 服务器", onBack = onBack) {
        if (servers.isEmpty()) item { M3AgentEmptyHint("还没有 MCP 服务器。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                servers.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    val live = McpClientManager.connectedProviders().firstOrNull { it.id == s.id }
                    val status = live?.state?.name ?: "DISCONNECTED"
                    M3AgentArrowItem(
                        title = s.name.ifBlank { s.endpointUrl ?: s.id },
                        summary = "${s.transport?.name ?: "?"} · $status" + (live?.lastError?.let { " · $it" } ?: ""),
                        onClick = { onOpenServer(s.id) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = AGENT_CONTENT_BOTTOM_INSET),
            ) { Text("添加服务器") }
        }
    }

    if (showAdd) {
        M3AddMcpDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, transport, url, headersJson ->
                scope.launch {
                    WeAgentRepository.upsertMcpProvider(
                        ProviderEntity(
                            id = UUID.randomUUID().toString(),
                            kind = ProviderKind.MCP,
                            name = name.ifBlank { url },
                            transport = transport,
                            endpointUrl = url,
                            headersJson = headersJson.ifBlank { null },
                            enabled = true,
                        )
                    )
                }
                showAdd = false
            },
        )
    }
}

@Composable
private fun M3AddMcpDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, transport: McpTransport, url: String, headersJson: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf("") }
    var transportIndex by remember { mutableIntStateOf(0) }
    val transports = listOf(McpTransport.STREAMABLE_HTTP, McpTransport.SSE)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 MCP 服务器") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("服务器 URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                M3AgentIndexDropdownItem(
                    title = "传输方式",
                    items = listOf("Streamable HTTP", "SSE"),
                    selectedIndex = transportIndex,
                    onSelected = { transportIndex = it },
                )
                OutlinedTextField(
                    value = headers,
                    onValueChange = { headers = it },
                    label = { Text("""自定义请求头 JSON（可选，如 {"Authorization":"Bearer ..."}）""") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onConfirm(name, transports[transportIndex], url, headers) },
                    enabled = url.isNotBlank(),
                ) { Text("添加") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  MCP Server Detail Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3McpServerDetailScreen(serverId: String, onBack: () -> Unit) {
    val allProviders by WeAgentRepository.observeProviders().collectAsState(initial = emptyList())
    val server = allProviders.firstOrNull { it.id == serverId }
    val scope = rememberCoroutineScope()
    val perms by WeAgentRepository.observeToolPermissions().collectAsState(initial = emptyList())
    val permMap = perms.associate { (it.providerId to it.toolName) to it.mode }

    val live = McpClientManager.connectedProviders().firstOrNull { it.id == serverId }
    val tools = live?.listTools().orEmpty()

    Material3AgentSettingsScaffold(title = server?.name ?: "MCP 服务器", onBack = onBack) {
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                M3AgentArrowItem(
                    title = "连接状态",
                    summary = (live?.state?.name ?: "DISCONNECTED") +
                        (live?.lastError?.let { " · $it" } ?: "") + " · 点击刷新工具",
                    onClick = { scope.launch { McpClientManager.refreshTools(serverId) } },
                )
                server?.let {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentArrowItem(title = "地址", summary = "${it.transport?.name ?: "?"} · ${it.endpointUrl}")
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                TextButton(
                    onClick = { scope.launch { WeAgentRepository.deleteMcpProvider(serverId) }; onBack() },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) { Text("删除此服务器", color = MaterialTheme.colorScheme.error) }
            }
        }

        item { M3AgentSectionTitle("工具权限") }
        if (tools.isEmpty()) item { M3AgentEmptyHint("未连接或无工具。连接后可在此设置每个工具的权限。") }
        else item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                tools.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    val mode = permMap[serverId to t.name] ?: t.factoryDefaultMode
                    M3AgentIndexDropdownItem(
                        title = t.name,
                        items = MCP_MODE_ORDER.map { it.mcpLabel() },
                        selectedIndex = MCP_MODE_ORDER.indexOf(mode).coerceAtLeast(0),
                        onSelected = { scope.launch { WeAgentRepository.setToolMode(serverId, t.name, MCP_MODE_ORDER[it]) } },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(AGENT_CONTENT_BOTTOM_INSET)) }
    }
}
