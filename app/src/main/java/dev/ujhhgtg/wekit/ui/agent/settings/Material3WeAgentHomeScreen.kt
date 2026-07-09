package dev.ujhhgtg.wekit.ui.agent.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.wekit.activity.agent.AgentSettingsScreen
import dev.ujhhgtg.wekit.agent.data.WeAgentRepository
import dev.ujhhgtg.wekit.agent.data.WeAgentSettings
import dev.ujhhgtg.wekit.agent.data.entity.ModelEntity
import dev.ujhhgtg.wekit.agent.data.entity.SystemPromptEntity
import dev.ujhhgtg.wekit.agent.data.entity.WorkspaceEntity
import dev.ujhhgtg.wekit.features.api.agent.WeAgentService
import dev.ujhhgtg.wekit.features.items.system.agent.WeAgentOverlayController
import kotlinx.coroutines.launch

@Composable
fun Material3WeAgentHomeScreen(onOpen: (AgentSettingsScreen) -> Unit) {
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var dynamicTools by remember { mutableStateOf(false) }
    var overlayForegroundOnly by remember { mutableStateOf(false) }
    var sendWhileRunning by remember { mutableStateOf("QUEUE_AFTER_TURN") }
    var maxRequests by remember { mutableStateOf(WeAgentSettings.DEFAULT_MAX_MODEL_REQUESTS.toString()) }
    var smallModelId by remember { mutableStateOf<String?>(null) }
    var defaultModelId by remember { mutableStateOf<String?>(null) }
    var defaultSystemPromptId by remember { mutableStateOf<String?>(null) }
    var defaultWorkspaceId by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<ModelEntity>>(emptyList()) }
    var systemPrompts by remember { mutableStateOf<List<SystemPromptEntity>>(emptyList()) }
    var workspaces by remember { mutableStateOf<List<WorkspaceEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        dynamicTools = WeAgentSettings.toolLoadingMode() == dev.ujhhgtg.wekit.agent.tool.ToolLoadingMode.DYNAMIC
        overlayForegroundOnly = WeAgentSettings.overlayForegroundOnly()
        sendWhileRunning = WeAgentSettings.sendWhileRunningMode().name
        maxRequests = WeAgentSettings.maxModelRequests().toString()
        smallModelId = WeAgentSettings.smallModelId()
        defaultModelId = WeAgentSettings.defaultModelId()
        defaultSystemPromptId = WeAgentSettings.defaultSystemPromptId()
        defaultWorkspaceId = WeAgentSettings.defaultWorkspaceId()
        models = WeAgentRepository.getAllModelsOnce()
        systemPrompts = WeAgentRepository.getAllSystemPromptsOnce()
        workspaces = WeAgentRepository.observeWorkspacesOnce()
        loaded = true
    }

    Material3AgentSettingsScaffold(title = "WeAgent 设置", onBack = null) {
        // ---------- 界面 ----------
        item { M3AgentSectionTitle("界面") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                if (loaded) {
                    M3AgentSwitchItem(
                        title = "仅前台显示悬浮窗",
                        summary = "微信切到后台时自动隐藏悬浮窗，回到前台再显示",
                        checked = overlayForegroundOnly,
                        onCheckedChange = {
                            overlayForegroundOnly = it
                            WeAgentOverlayController.setForegroundOnly(it)
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_OVERLAY_FOREGROUND_ONLY, it.toString()) }
                        },
                    )
                }
            }
        }

        // ---------- 模型 ----------
        item { M3AgentSectionTitle("模型") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                M3AgentArrowItem(
                    title = "模型提供方",
                    summary = "配置 OpenAI Chat Completions / OpenAI Responses / Anthropic Messages 服务器、API Key、模型",
                    onClick = { onOpen(AgentSettingsScreen.ModelProviders) },
                )
                if (loaded) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("每轮请求上限", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = maxRequests,
                            onValueChange = { v -> maxRequests = v.filter { it.isDigit() }.take(3) },
                            label = { Text("上限") },
                            singleLine = true,
                            modifier = Modifier.width(96.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentGenericDropdown(
                        title = "审批 / 标题小模型",
                        items = buildList {
                            add(null to "（与主模型相同）")
                            models.forEach { add(it.id to it.displayName.ifBlank { it.modelIdRemote }) }
                        },
                        selectedId = smallModelId,
                        onSelected = { id ->
                            smallModelId = id
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_SMALL_MODEL_ID, id.orEmpty()) }
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentIndexDropdownItem(
                        title = "运行中发送行为",
                        items = listOf("队列（本轮对话结束后发送）", "引导（下次请求前插入）"),
                        selectedIndex = if (sendWhileRunning == "QUEUE_AS_STEER") 1 else 0,
                        onSelected = {
                            val mode = if (it == 1) "QUEUE_AS_STEER" else "QUEUE_AFTER_TURN"
                            sendWhileRunning = mode
                            WeAgentService.sendWhileRunningMode.value =
                                if (mode == "QUEUE_AS_STEER") WeAgentService.SendWhileRunningMode.QUEUE_AS_STEER
                                else WeAgentService.SendWhileRunningMode.QUEUE_AFTER_TURN
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_SEND_WHILE_RUNNING, mode) }
                        },
                    )
                }
            }
        }

        // ---------- 工具 ----------
        item { M3AgentSectionTitle("工具") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                M3AgentArrowItem(
                    title = "内置工具",
                    summary = "微信操作 / 数据库 SQL / 文件与技能，逐项设置权限",
                    onClick = { onOpen(AgentSettingsScreen.BuiltinTools) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem(
                    title = "MCP 服务器",
                    summary = "添加 Streamable HTTP / SSE 服务器",
                    onClick = { onOpen(AgentSettingsScreen.McpServers) },
                )
                if (loaded) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentSwitchItem(
                        title = "动态工具发现",
                        summary = "仅提供 discover_tools 元工具，按需暴露其余工具（工具很多时省 token）",
                        checked = dynamicTools,
                        onCheckedChange = {
                            dynamicTools = it
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_TOOL_LOADING_MODE, if (it) "DYNAMIC" else "STATIC") }
                        },
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem(
                    title = "工作区",
                    summary = "文件工作区目录管理",
                    onClick = { onOpen(AgentSettingsScreen.Workspaces) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem(
                    title = "记忆",
                    summary = "全局开关与记忆索引查看",
                    onClick = { onOpen(AgentSettingsScreen.Memory) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem(
                    title = "外部服务",
                    summary = "Exa Search、Brave Search 等网络工具的 API Key",
                    onClick = { onOpen(AgentSettingsScreen.ExternalServices) },
                )
            }
        }

        // ---------- 上下文 ----------
        item { M3AgentSectionTitle("上下文") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                M3AgentArrowItem("提示词", "系统 / 每轮 / 条件 / 预设 提示词", onClick = { onOpen(AgentSettingsScreen.Prompts) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem("技能", "任务操作手册, 可被 LLM 动态发现并按需加载", onClick = { onOpen(AgentSettingsScreen.Skills) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3AgentArrowItem("触发器", "定时 / 新消息 / 数据库事件自动唤起 AI, 支持会话级与全局触发器", onClick = { onOpen(AgentSettingsScreen.Triggers) })
            }
        }

        // ---------- 默认 ----------
        if (loaded) {
            item { M3AgentSectionTitle("默认") }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    M3AgentGenericDropdown(
                        title = "默认模型",
                        items = buildList {
                            add(null to "（使用第一个模型）")
                            models.forEach { add(it.id to it.displayName.ifBlank { it.modelIdRemote }) }
                        },
                        selectedId = defaultModelId,
                        onSelected = { id ->
                            defaultModelId = id
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_DEFAULT_MODEL_ID, id.orEmpty()) }
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentGenericDropdown(
                        title = "默认系统提示词",
                        items = buildList {
                            add(null to "（无）")
                            systemPrompts.forEach { add(it.id to it.name) }
                        },
                        selectedId = defaultSystemPromptId,
                        onSelected = { id ->
                            defaultSystemPromptId = id
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_DEFAULT_SYSTEM_PROMPT_ID, id.orEmpty()) }
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    M3AgentGenericDropdown(
                        title = "默认工作区",
                        items = buildList {
                            add(null to "（无）")
                            workspaces.forEach { add(it.id to it.name) }
                        },
                        selectedId = defaultWorkspaceId,
                        onSelected = { id ->
                            defaultWorkspaceId = id
                            scope.launch { WeAgentSettings.set(WeAgentSettings.KEY_DEFAULT_WORKSPACE_ID, id.orEmpty()) }
                        },
                    )
                }
            }
        }

        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = AGENT_CONTENT_BOTTOM_INSET)) }
    }

    LaunchedEffect(maxRequests, loaded) {
        if (!loaded) return@LaunchedEffect
        maxRequests.toIntOrNull()?.let {
            WeAgentSettings.set(WeAgentSettings.KEY_MAX_MODEL_REQUESTS, it.coerceIn(1, 100).toString())
        }
    }
}

/** (id?, label) pair dropdown via AlertDialog — M3 equivalent of [GenericDropdown]. */
@Composable
private fun M3AgentGenericDropdown(
    title: String,
    items: List<Pair<String?, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    val selectedIndex = items.indexOfFirst { it.first == selectedId }.coerceAtLeast(0)
    M3AgentIndexDropdownItem(
        title = title,
        items = items.map { it.second },
        selectedIndex = selectedIndex,
        onSelected = { onSelected(items[it].first) },
    )
}
