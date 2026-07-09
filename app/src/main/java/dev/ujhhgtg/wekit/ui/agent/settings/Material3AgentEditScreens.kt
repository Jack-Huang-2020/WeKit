package dev.ujhhgtg.wekit.ui.agent.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.wekit.agent.data.WeAgentRepository
import dev.ujhhgtg.wekit.agent.data.entity.ConditionalPromptEntity
import dev.ujhhgtg.wekit.agent.data.entity.PerTurnPromptEntity
import dev.ujhhgtg.wekit.agent.data.entity.PresetPromptEntity
import dev.ujhhgtg.wekit.agent.data.entity.SessionEntity
import dev.ujhhgtg.wekit.agent.data.entity.SystemPromptEntity
import dev.ujhhgtg.wekit.agent.skill.SkillStore
import dev.ujhhgtg.wekit.utils.android.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ---------------------------------------------------------------------------
//  Prompts Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3PromptsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val systemPrompts by WeAgentRepository.observeSystemPrompts().collectAsState(initial = emptyList())
    val perTurn      by WeAgentRepository.observePerTurnPrompts().collectAsState(initial = emptyList())
    val conditionals by WeAgentRepository.observeConditionalPrompts().collectAsState(initial = emptyList())
    val presets      by WeAgentRepository.observePresetPrompts().collectAsState(initial = emptyList())

    var editSystem     by remember { mutableStateOf<SystemPromptEntity?>(null) }
    var editPerTurn    by remember { mutableStateOf<PerTurnPromptEntity?>(null) }
    var editConditional by remember { mutableStateOf<ConditionalPromptEntity?>(null) }
    var editPreset     by remember { mutableStateOf<PresetPromptEntity?>(null) }

    Material3AgentSettingsScaffold(title = "提示词", onBack = onBack) {
        // 系统提示词
        item { M3AgentSectionTitle("系统提示词") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                if (systemPrompts.isEmpty()) {
                    M3AgentEmptyHint("还没有系统提示词。会话可绑定其中一个。")
                } else {
                    systemPrompts.forEachIndexed { i, sp ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentArrowItem(title = sp.name, summary = sp.content.take(48), onClick = { editSystem = sp })
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                TextButton(
                    onClick = { editSystem = SystemPromptEntity("", "", "") },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) { Text("新增系统提示词") }
            }
        }

        // 每轮提示词
        item { M3AgentSectionTitle("每轮提示词") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                if (perTurn.isEmpty()) {
                    M3AgentEmptyHint("还没有每轮提示词。开启后会追加到每条用户消息前。")
                } else {
                    perTurn.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentSwitchItem(
                            title = p.title.ifBlank { p.content.take(24) },
                            summary = p.content.take(48),
                            checked = p.enabled,
                            onCheckedChange = { on -> scope.launch { WeAgentRepository.upsertPerTurnPrompt(p.copy(enabled = on)) } },
                        )
                        TextButton(
                            onClick = { editPerTurn = p },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) { Text("编辑") }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                TextButton(
                    onClick = { editPerTurn = PerTurnPromptEntity("", "", "", true) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) { Text("新增每轮提示词") }
            }
        }

        // 条件提示词
        item { M3AgentSectionTitle("条件提示词") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                if (conditionals.isEmpty()) {
                    M3AgentEmptyHint("还没有条件提示词。开启后按正则匹配模型回复并注入内容。")
                } else {
                    conditionals.forEachIndexed { i, c ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentSwitchItem(
                            title = "/${c.regex}/",
                            summary = c.content.take(48),
                            checked = c.enabled,
                            onCheckedChange = { on -> scope.launch { WeAgentRepository.upsertConditionalPrompt(c.copy(enabled = on)) } },
                        )
                        TextButton(
                            onClick = { editConditional = c },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) { Text("编辑") }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                TextButton(
                    onClick = { editConditional = ConditionalPromptEntity("", "", "", true) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) { Text("新增条件提示词") }
            }
        }

        // 预设提示词
        item { M3AgentSectionTitle("预设提示词") }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                if (presets.isEmpty()) {
                    M3AgentEmptyHint("还没有预设提示词。可在对话输入框的 + 菜单里插入。")
                } else {
                    presets.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentArrowItem(title = p.title, summary = p.content.take(48), onClick = { editPreset = p })
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                TextButton(
                    onClick = { editPreset = PresetPromptEntity("", "", "") },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) { Text("新增预设提示词") }
            }
        }
        item { Spacer(Modifier.height(AGENT_CONTENT_BOTTOM_INSET)) }
    }

    // Editors
    editSystem?.let { entity ->
        M3TwoFieldEditor(
            title = if (entity.id.isEmpty()) "新增系统提示词" else "编辑系统提示词",
            field1Label = "名称", field1 = entity.name,
            field2Label = "内容", field2 = entity.content, field2MaxLines = 12,
            onDismiss = { editSystem = null },
            onDelete = entity.id.takeIf { it.isNotEmpty() }
                ?.let { { scope.launch { WeAgentRepository.deleteSystemPrompt(it) }; editSystem = null } },
            onSave = { name, content ->
                scope.launch {
                    WeAgentRepository.upsertSystemPrompt(
                        entity.copy(id = entity.id.ifEmpty { UUID.randomUUID().toString() }, name = name, content = content)
                    )
                }
                editSystem = null
            },
        )
    }
    editPerTurn?.let { entity ->
        M3TwoFieldEditor(
            title = if (entity.id.isEmpty()) "新增每轮提示词" else "编辑每轮提示词",
            field1Label = "标题（可选）", field1 = entity.title,
            field2Label = "内容", field2 = entity.content, field2MaxLines = 8,
            onDismiss = { editPerTurn = null },
            onDelete = entity.id.takeIf { it.isNotEmpty() }
                ?.let { { scope.launch { WeAgentRepository.deletePerTurnPrompt(it) }; editPerTurn = null } },
            onSave = { title, content ->
                scope.launch {
                    WeAgentRepository.upsertPerTurnPrompt(
                        entity.copy(id = entity.id.ifEmpty { UUID.randomUUID().toString() }, title = title, content = content)
                    )
                }
                editPerTurn = null
            },
        )
    }
    editConditional?.let { entity ->
        M3TwoFieldEditor(
            title = if (entity.id.isEmpty()) "新增条件提示词" else "编辑条件提示词",
            field1Label = "触发正则", field1 = entity.regex,
            field2Label = "注入内容", field2 = entity.content, field2MaxLines = 8,
            onDismiss = { editConditional = null },
            onDelete = entity.id.takeIf { it.isNotEmpty() }
                ?.let { { scope.launch { WeAgentRepository.deleteConditionalPrompt(it) }; editConditional = null } },
            onSave = { regex, content ->
                scope.launch {
                    WeAgentRepository.upsertConditionalPrompt(
                        entity.copy(id = entity.id.ifEmpty { UUID.randomUUID().toString() }, regex = regex, content = content)
                    )
                }
                editConditional = null
            },
        )
    }
    editPreset?.let { entity ->
        M3TwoFieldEditor(
            title = if (entity.id.isEmpty()) "新增预设提示词" else "编辑预设提示词",
            field1Label = "标题", field1 = entity.title,
            field2Label = "内容", field2 = entity.content, field2MaxLines = 8,
            onDismiss = { editPreset = null },
            onDelete = entity.id.takeIf { it.isNotEmpty() }
                ?.let { { scope.launch { WeAgentRepository.deletePresetPrompt(it) }; editPreset = null } },
            onSave = { title, content ->
                scope.launch {
                    WeAgentRepository.upsertPresetPrompt(
                        entity.copy(id = entity.id.ifEmpty { UUID.randomUUID().toString() }, title = title, content = content)
                    )
                }
                editPreset = null
            },
        )
    }
}

/** Generic two-field AlertDialog editor used across all prompt types. */
@Composable
private fun M3TwoFieldEditor(
    title: String,
    field1Label: String, field1: String,
    field2Label: String, field2: String, field2MaxLines: Int,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, String) -> Unit,
) {
    var f1 by remember { mutableStateOf(field1) }
    var f2 by remember { mutableStateOf(field2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text(field1Label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text(field2Label) }, maxLines = field2MaxLines, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(f1, f2) }, enabled = f2.isNotBlank()) { Text("保存") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  Skills Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3SkillsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var reloadTick by remember { mutableIntStateOf(0) }
    var skills by remember { mutableStateOf<List<SkillStore.Skill>>(emptyList()) }
    LaunchedEffect(reloadTick) {
        skills = withContext(Dispatchers.IO) { SkillStore.list() }
    }
    var editing   by remember { mutableStateOf<SkillStore.Skill?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Material3AgentSettingsScaffold(title = "技能", onBack = onBack) {
        if (skills.isEmpty()) {
            item { M3AgentEmptyHint("还没有技能。技能是针对特定任务的操作手册，LLM 会按需加载。") }
        }
        items(skills.size, key = { skills[it].name }) { i ->
            val s = skills[i]
            OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                M3AgentSwitchItem(
                    title = s.name,
                    summary = s.description.ifBlank { "（无简介）" },
                    checked = s.enabled,
                    onCheckedChange = { on ->
                        scope.launch {
                            withContext(Dispatchers.IO) { SkillStore.setEnabled(s.name, on) }
                            reloadTick++
                        }
                    },
                )
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    TextButton(onClick = { editing = s; showEditor = true }, modifier = Modifier.weight(1f)) { Text("编辑") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            scope.launch { withContext(Dispatchers.IO) { SkillStore.delete(s.name) }; reloadTick++ }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            Button(
                onClick = { editing = null; showEditor = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = AGENT_CONTENT_BOTTOM_INSET),
            ) { Text("添加技能") }
        }
    }

    if (showEditor) {
        M3SkillEditorDialog(
            existing = editing,
            onDismiss = { showEditor = false },
            onSave = { name, description, body ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { SkillStore.save(name, description, body) }
                    if (ok == null) showToast("技能名称无效")
                    else {
                        editing?.name?.takeIf { it != ok }?.let { old ->
                            withContext(Dispatchers.IO) { SkillStore.delete(old) }
                        }
                        reloadTick++
                        showEditor = false
                    }
                }
            },
        )
    }
}

@Composable
private fun M3SkillEditorDialog(
    existing: SkillStore.Skill?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, body: String) -> Unit,
) {
    var name        by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var body        by remember(existing) { mutableStateOf(existing?.body.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "添加技能" else "编辑技能") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("技能名称（同时作为目录名）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("简介（决定 LLM 何时加载此技能）") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("技能正文（SKILL.md 指令内容）") }, maxLines = 10, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(name, description, body) }, enabled = name.isNotBlank() && body.isNotBlank()) { Text("保存") }
            }
        },
    )
}

// ---------------------------------------------------------------------------
//  Triggers Screen (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3TriggersScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val triggers by remember { WeAgentRepository.observeTriggers() }.collectAsState(initial = emptyList())
    val sessions by remember {
        WeAgentRepository.observeSessions().map { list: List<SessionEntity> -> list.associateBy { it.id } }
    }.collectAsState(initial = emptyMap())

    var editing    by remember { mutableStateOf<dev.ujhhgtg.wekit.agent.data.entity.TriggerEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val global     = triggers.filter { it.scope == dev.ujhhgtg.wekit.agent.trigger.TriggerScope.GLOBAL }
    val perSession = triggers.filter { it.scope == dev.ujhhgtg.wekit.agent.trigger.TriggerScope.SESSION }

    Material3AgentSettingsScaffold(title = "触发器", onBack = onBack) {
        if (triggers.isEmpty()) {
            item { M3AgentEmptyHint("还没有触发器。触发器可在定时、收到新消息或检测到数据库操作时自动唤起 AI。") }
        }
        if (global.isNotEmpty()) {
            item { M3AgentSectionTitle("全局触发器") }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    global.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentSwitchItem(
                            title = t.name.ifBlank { "（未命名触发器）" },
                            summary = "${typeLabel(t.type)} · ${configSummary(t)}",
                            checked = t.enabled,
                            onCheckedChange = { on -> scope.launch { WeAgentRepository.setTriggerEnabled(t.id, on) } },
                        )
                        TextButton(onClick = { editing = t; showEditor = true }, modifier = Modifier.padding(horizontal = 4.dp)) { Text("编辑") }
                    }
                }
            }
        }
        if (perSession.isNotEmpty()) {
            item { M3AgentSectionTitle("会话触发器") }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    perSession.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        M3AgentSwitchItem(
                            title = t.name.ifBlank { "（未命名触发器）" },
                            summary = "${typeLabel(t.type)} · ${sessions[t.sessionId]?.title ?: "（会话已删除）"}",
                            checked = t.enabled,
                            onCheckedChange = { on -> scope.launch { WeAgentRepository.setTriggerEnabled(t.id, on) } },
                        )
                        TextButton(onClick = { editing = t; showEditor = true }, modifier = Modifier.padding(horizontal = 4.dp)) { Text("编辑") }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { editing = null; showEditor = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = AGENT_CONTENT_BOTTOM_INSET),
            ) { Text("添加触发器") }
        }
    }

    if (showEditor) {
        M3TriggerEditorDialog(
            existing = editing,
            sessions = sessions,
            onDismiss = { showEditor = false },
            onSave = { built ->
                scope.launch { WeAgentRepository.upsertTrigger(built) }
                showEditor = false
            },
        )
    }
}

@Composable
private fun M3TriggerEditorDialog(
    existing: dev.ujhhgtg.wekit.agent.data.entity.TriggerEntity?,
    sessions: Map<String, SessionEntity>,
    onDismiss: () -> Unit,
    onSave: (dev.ujhhgtg.wekit.agent.data.entity.TriggerEntity) -> Unit,
) {
    val creating = existing == null
    var name     by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var prompt   by remember(existing) { mutableStateOf(existing?.promptTemplate.orEmpty()) }

    val scopeOptions = listOf(
        dev.ujhhgtg.wekit.agent.trigger.TriggerScope.SESSION,
        dev.ujhhgtg.wekit.agent.trigger.TriggerScope.GLOBAL,
    )
    var scopeIndex by remember(existing) {
        mutableIntStateOf(scopeOptions.indexOf(existing?.scope ?: dev.ujhhgtg.wekit.agent.trigger.TriggerScope.GLOBAL).coerceAtLeast(0))
    }
    val selectedScope = scopeOptions[scopeIndex]
    val sessionList = remember(sessions) { sessions.values.toList() }
    var boundSessionIndex by remember(existing, sessionList) {
        mutableIntStateOf(sessionList.indexOfFirst { it.id == existing?.sessionId }.coerceAtLeast(0))
    }

    val typeOptions = listOf(
        dev.ujhhgtg.wekit.agent.trigger.TriggerType.SCHEDULE,
        dev.ujhhgtg.wekit.agent.trigger.TriggerType.MESSAGE,
        dev.ujhhgtg.wekit.agent.trigger.TriggerType.SQL,
    )
    var typeIndex by remember(existing) {
        mutableIntStateOf(typeOptions.indexOf(existing?.type ?: dev.ujhhgtg.wekit.agent.trigger.TriggerType.SCHEDULE).coerceAtLeast(0))
    }
    val type = typeOptions[typeIndex]

    val scheduleKinds = listOf(
        dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.INTERVAL,
        dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.DAILY,
        dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.CRON,
        dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.ONCE,
    )
    var kindIndex      by remember(existing) { mutableIntStateOf(scheduleKinds.indexOf(existing?.scheduleKind ?: dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.INTERVAL).coerceAtLeast(0)) }
    val kind = scheduleKinds[kindIndex]
    var intervalSeconds by remember(existing) { mutableStateOf((existing?.intervalSeconds ?: 3600).toString()) }
    var dailyHour       by remember(existing) { mutableStateOf(((existing?.dailyMinuteOfDay ?: 540) / 60).toString()) }
    var dailyMinute     by remember(existing) { mutableStateOf(((existing?.dailyMinuteOfDay ?: 540) % 60).toString()) }
    var cronExpr        by remember(existing) { mutableStateOf(existing?.cronExpr ?: "0 9 * * *") }

    val cond = remember(existing) { dev.ujhhgtg.wekit.agent.trigger.TriggerConditionsJson.decode(existing?.conditionsJson) }
    var contentRegex by remember(existing) { mutableStateOf(cond.contentRegex.orEmpty()) }
    var talkerRegex  by remember(existing) { mutableStateOf(cond.talkerRegex.orEmpty()) }
    var msgTypes     by remember(existing) { mutableStateOf(cond.msgTypes?.joinToString(",").orEmpty()) }
    val directions   = listOf(
        dev.ujhhgtg.wekit.agent.trigger.MessageDirection.RECEIVED,
        dev.ujhhgtg.wekit.agent.trigger.MessageDirection.SENT,
        dev.ujhhgtg.wekit.agent.trigger.MessageDirection.BOTH,
    )
    var directionIndex by remember(existing) { mutableIntStateOf(directions.indexOf(cond.direction).coerceAtLeast(0)) }
    var tableRegex   by remember(existing) { mutableStateOf(cond.tableRegex.orEmpty()) }
    var sqlRegex     by remember(existing) { mutableStateOf(cond.sqlRegex.orEmpty()) }
    var valuesRegex  by remember(existing) { mutableStateOf(cond.valuesRegex.orEmpty()) }
    var opInsert     by remember(existing) { mutableStateOf(cond.sqlOps.isEmpty() || dev.ujhhgtg.wekit.agent.trigger.SqlOp.INSERT in cond.sqlOps) }
    var opUpdate     by remember(existing) { mutableStateOf(cond.sqlOps.isEmpty() || dev.ujhhgtg.wekit.agent.trigger.SqlOp.UPDATE in cond.sqlOps) }
    var opQuery      by remember(existing) { mutableStateOf(cond.sqlOps.isEmpty() || dev.ujhhgtg.wekit.agent.trigger.SqlOp.QUERY in cond.sqlOps) }

    var debounceSec by remember(existing) { mutableStateOf(((existing?.bufferDebounceMillis ?: 3000) / 1000).toString()) }
    var maxEvents   by remember(existing) { mutableStateOf((existing?.bufferMaxEvents ?: 20).toString()) }
    var maxWaitSec  by remember(existing) { mutableStateOf(((existing?.bufferMaxWaitMillis ?: 30000) / 1000).toString()) }
    var cooldownSec by remember(existing) { mutableStateOf(((existing?.cooldownMillis ?: 0) / 1000).toString()) }
    var filterOwn   by remember(existing) { mutableStateOf(existing?.filterOwnEvents ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "添加触发器" else "编辑触发器") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                M3AgentIndexDropdownItem("类型", listOf("定时", "新消息", "数据库操作"), typeIndex, onSelected = { if (creating) typeIndex = it })
                M3AgentIndexDropdownItem("作用域", listOf("绑定会话", "全局（每次新建会话）"), scopeIndex, onSelected = { scopeIndex = it })
                if (selectedScope == dev.ujhhgtg.wekit.agent.trigger.TriggerScope.SESSION) {
                    if (sessionList.isEmpty()) Text("还没有会话，无法绑定。")
                    else M3AgentIndexDropdownItem(
                        title = "绑定到会话",
                        items = sessionList.map { s -> s.title.ifBlank { "（未命名会话）" } },
                        selectedIndex = boundSessionIndex.coerceIn(0, sessionList.lastIndex),
                        onSelected = { boundSessionIndex = it },
                    )
                }
                when (type) {
                    dev.ujhhgtg.wekit.agent.trigger.TriggerType.SCHEDULE -> {
                        M3AgentIndexDropdownItem("调度方式", listOf("固定间隔", "每天定时", "Cron 表达式", "一次性"), kindIndex, onSelected = { kindIndex = it })
                        when (kind) {
                            dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.INTERVAL ->
                                OutlinedTextField(value = intervalSeconds, onValueChange = { v -> intervalSeconds = v.filter { it.isDigit() }.take(7) }, label = { Text("间隔（秒）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.DAILY -> Row(Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = dailyHour, onValueChange = { v -> dailyHour = v.filter { it.isDigit() }.take(2) }, label = { Text("时 (0-23)") }, singleLine = true, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(value = dailyMinute, onValueChange = { v -> dailyMinute = v.filter { it.isDigit() }.take(2) }, label = { Text("分 (0-59)") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.CRON ->
                                OutlinedTextField(value = cronExpr, onValueChange = { cronExpr = it }, label = { Text("Cron（分 时 日 月 周）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            dev.ujhhgtg.wekit.agent.trigger.ScheduleKind.ONCE ->
                                Text("一次性请由 AI 通过工具配置触发时间。")
                        }
                    }
                    dev.ujhhgtg.wekit.agent.trigger.TriggerType.MESSAGE -> {
                        OutlinedTextField(value = contentRegex, onValueChange = { contentRegex = it }, label = { Text("内容匹配（正则，可空）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = talkerRegex, onValueChange = { talkerRegex = it }, label = { Text("会话/发送者匹配（正则，可空）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = msgTypes, onValueChange = { msgTypes = it }, label = { Text("消息类型码（逗号分隔，可空）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        M3AgentIndexDropdownItem("方向", listOf("收到", "发出", "两者"), directionIndex, onSelected = { directionIndex = it })
                        M3AgentSwitchItem(title = "过滤自己发出的消息", summary = "同时会挡住 AI 通过工具发出的消息，避免自触发循环", checked = filterOwn, onCheckedChange = { filterOwn = it })
                        M3BufferFields(debounceSec, maxEvents, maxWaitSec, cooldownSec, { debounceSec = it }, { maxEvents = it }, { maxWaitSec = it }, { cooldownSec = it })
                    }
                    dev.ujhhgtg.wekit.agent.trigger.TriggerType.SQL -> {
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(onClick = { opInsert = !opInsert }) { Text(if (opInsert) "✓ INSERT" else "INSERT") }
                            TextButton(onClick = { opUpdate = !opUpdate }) { Text(if (opUpdate) "✓ UPDATE" else "UPDATE") }
                            TextButton(onClick = { opQuery = !opQuery }) { Text(if (opQuery) "✓ QUERY" else "QUERY") }
                        }
                        OutlinedTextField(value = tableRegex, onValueChange = { tableRegex = it }, label = { Text("表名匹配（正则，INSERT/UPDATE）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = sqlRegex, onValueChange = { sqlRegex = it }, label = { Text("SQL 匹配（正则，QUERY）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = valuesRegex, onValueChange = { valuesRegex = it }, label = { Text("写入值匹配（正则，INSERT/UPDATE）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        M3BufferFields(debounceSec, maxEvents, maxWaitSec, cooldownSec, { debounceSec = it }, { maxEvents = it }, { maxWaitSec = it }, { cooldownSec = it })
                    }
                }
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("触发时发送的提示词") }, maxLines = 5, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row {
                if (!creating) {
                    TextButton(onClick = {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            WeAgentRepository.deleteTrigger(existing.id)
                        }
                        onDismiss()
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = {
                        val built = buildTrigger(
                            existing = existing,
                            name = name, promptTemplate = prompt,
                            type = type, scope = selectedScope,
                            sessionId = if (selectedScope == dev.ujhhgtg.wekit.agent.trigger.TriggerScope.SESSION)
                                sessionList.getOrNull(boundSessionIndex)?.id else null,
                            kind = kind,
                            intervalSeconds = intervalSeconds.toLongOrNull(),
                            dailyMinuteOfDay = (dailyHour.toIntOrNull() ?: 0) * 60 + (dailyMinute.toIntOrNull() ?: 0),
                            cronExpr = cronExpr,
                            conditions = dev.ujhhgtg.wekit.agent.trigger.TriggerConditions(
                                contentRegex = contentRegex.ifBlank { null },
                                talkerRegex = talkerRegex.ifBlank { null },
                                msgTypes = msgTypes.split(',').mapNotNull { it.trim().toIntOrNull() }.takeIf { it.isNotEmpty() },
                                direction = directions[directionIndex],
                                sqlOps = buildList {
                                    if (opInsert) add(dev.ujhhgtg.wekit.agent.trigger.SqlOp.INSERT)
                                    if (opUpdate) add(dev.ujhhgtg.wekit.agent.trigger.SqlOp.UPDATE)
                                    if (opQuery) add(dev.ujhhgtg.wekit.agent.trigger.SqlOp.QUERY)
                                }.takeIf { it.size < 3 } ?: emptyList(),
                                tableRegex = tableRegex.ifBlank { null },
                                sqlRegex = sqlRegex.ifBlank { null },
                                valuesRegex = valuesRegex.ifBlank { null },
                            ),
                            debounceSec = debounceSec.toLongOrNull(),
                            maxEvents = maxEvents.toIntOrNull(),
                            maxWaitSec = maxWaitSec.toLongOrNull(),
                            cooldownSec = cooldownSec.toLongOrNull(),
                            filterOwn = filterOwn,
                        )
                        onSave(built)
                    },
                    enabled = name.isNotBlank() && prompt.isNotBlank() &&
                        (selectedScope == dev.ujhhgtg.wekit.agent.trigger.TriggerScope.GLOBAL || sessionList.isNotEmpty()),
                ) { Text("保存") }
            }
        },
    )
}

@Composable
private fun M3BufferFields(
    debounceSec: String, maxEvents: String, maxWaitSec: String, cooldownSec: String,
    onDebounce: (String) -> Unit, onMax: (String) -> Unit, onWait: (String) -> Unit, onCooldown: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = debounceSec, onValueChange = { v -> onDebounce(v.filter { it.isDigit() }.take(7)) }, label = { Text("防抖（秒）") }, singleLine = true, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(value = maxEvents, onValueChange = { v -> onMax(v.filter { it.isDigit() }.take(7)) }, label = { Text("上限（条）") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = maxWaitSec, onValueChange = { v -> onWait(v.filter { it.isDigit() }.take(7)) }, label = { Text("最长等待（秒）") }, singleLine = true, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(value = cooldownSec, onValueChange = { v -> onCooldown(v.filter { it.isDigit() }.take(7)) }, label = { Text("冷却（秒）") }, singleLine = true, modifier = Modifier.weight(1f))
    }
}

