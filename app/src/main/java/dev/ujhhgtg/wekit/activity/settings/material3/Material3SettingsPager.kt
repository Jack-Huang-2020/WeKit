package dev.ujhhgtg.wekit.activity.settings.material3

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Auto_delete
import com.composables.icons.materialsymbols.outlined.Block
import com.composables.icons.materialsymbols.outlined.Brightness_medium
import com.composables.icons.materialsymbols.outlined.Build_circle
import com.composables.icons.materialsymbols.outlined.Close
import com.composables.icons.materialsymbols.outlined.Colorize
import com.composables.icons.materialsymbols.outlined.Contrast
import com.composables.icons.materialsymbols.outlined.Delete_forever
import com.composables.icons.materialsymbols.outlined.Download
import com.composables.icons.materialsymbols.outlined.Frame_bug
import com.composables.icons.materialsymbols.outlined.Label
import com.composables.icons.materialsymbols.outlined.Layers
import com.composables.icons.materialsymbols.outlined.License
import com.composables.icons.materialsymbols.outlined.Lightbulb_2
import com.composables.icons.materialsymbols.outlined.Notifications
import com.composables.icons.materialsymbols.outlined.Palette
import com.composables.icons.materialsymbols.outlined.Rule_settings
import com.composables.icons.materialsymbols.outlined.Search
import com.composables.icons.materialsymbols.outlined.Style
import com.composables.icons.materialsymbols.outlined.Sync
import com.composables.icons.materialsymbols.outlined.Update
import com.composables.icons.materialsymbols.outlined.Upload
import com.composables.icons.materialsymbols.outlined.Volunteer_activism
import com.composables.icons.materialsymbols.outlined.Wallpaper
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.aboutlibraries.AboutLibrariesProvider
import dev.ujhhgtg.wekit.activity.settings.CONTENT_BOTTOM_INSET
import dev.ujhhgtg.wekit.activity.settings.LocalComponentActivity
import dev.ujhhgtg.wekit.activity.settings.miuix.checkForUpdate
import dev.ujhhgtg.wekit.activity.settings.miuix.exportConfig
import dev.ujhhgtg.wekit.activity.settings.miuix.importConfig
import dev.ujhhgtg.wekit.constants.Preferences
import dev.ujhhgtg.wekit.features.items.debug.ResetDexCache
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.ui.utils.GitHubIcon
import dev.ujhhgtg.wekit.ui.utils.TelegramIcon
import dev.ujhhgtg.wekit.ui.utils.theme.AppColorSpec
import dev.ujhhgtg.wekit.ui.utils.theme.AppPaletteStyle
import dev.ujhhgtg.wekit.ui.utils.theme.AppThemeMode
import dev.ujhhgtg.wekit.ui.utils.theme.AppUiEngine
import dev.ujhhgtg.wekit.ui.utils.theme.ThemeSettings
import dev.ujhhgtg.wekit.utils.AppUpdater
import dev.ujhhgtg.wekit.utils.UpdateResult
import dev.ujhhgtg.wekit.utils.android.showToastSuspend
import dev.ujhhgtg.wekit.utils.formatEpoch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ColorPicker

// ---------------------------------------------------------------------------
//  Page 3 — Settings (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3SettingsPager(onOpenLicense: () -> Unit) {
    val context = LocalComponentActivity.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateResult.UpdateAvailable?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }

    M3ClearConfigDialog(show = showClearConfirm, onDismiss = { showClearConfirm = false })
    M3UpdateAvailableDialog(info = updateInfo, onDismiss = { updateInfo = null }, context = context)
    M3UpdateErrorDialog(message = updateError, onDismiss = { updateError = null })

    Material3ListScaffold(title = "设置") {
        // 界面
        item {
            Material3SectionTitle("界面")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                Material3ThemeSection()
            }
        }

        // 调试
        item {
            Material3SectionTitle("调试")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                M3PrefSwitch(
                    key = Preferences.VERBOSE_LOG,
                    title = "详细日志",
                    summary = "输出高频日志 (这可能会暴露你的隐私信息）",
                    icon = MaterialSymbols.Outlined.Frame_bug,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3PrefSwitch(
                    key = Preferences.SHOW_STARTUP_TOAST,
                    title = "显示加载完成 Toast",
                    summary = "全部功能加载完成后显示 Toast 提示",
                    icon = MaterialSymbols.Outlined.Notifications,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3PrefSwitch(
                    key = Preferences.MATCH_GENERIC_WXID_EXP,
                    title = "清理消息内容微信 ID 前缀时允许非标准 ID",
                    summary = "允许处理不带 'wxid_' 前缀的微信 ID, 可能导致误伤消息原始内容 (实验性)",
                    icon = MaterialSymbols.Outlined.Rule_settings,
                )
            }
        }

        // 兼容
        item {
            Material3SectionTitle("兼容")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                M3PrefSwitch(
                    key = Preferences.NO_DEX_RESOLVE,
                    title = "禁用版本适配",
                    summary = "不弹出 DEX 查找对话框，未适配功能将不会被加载",
                    icon = MaterialSymbols.Outlined.Block,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "重置适配信息",
                    summary = "清除 DEX 缓存, 等待下次启动时重新适配",
                    icon = MaterialSymbols.Outlined.Build_circle,
                    onClick = { ResetDexCache.onClick(context) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3PrefSwitch(
                    key = Preferences.RESET_DEX_ON_HOT_UPDATE,
                    title = "宿主热更新时重新适配",
                    summary = "宿主热更新时是否重置 DEX 缓存, 可能导致频繁重新适配 (实验性)",
                    icon = MaterialSymbols.Outlined.Auto_delete,
                )
            }
        }

        // 配置
        item {
            Material3SectionTitle("配置")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                M3ArrowItem(
                    title = "导出配置",
                    summary = "将模块配置导出为 JSON",
                    icon = MaterialSymbols.Outlined.Upload,
                    onClick = { exportConfig(context) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "导入配置",
                    summary = "从 JSON 导入模块配置; JSON 中的配置将会与现有配置合并, 覆盖所有已存在的配置",
                    icon = MaterialSymbols.Outlined.Download,
                    onClick = { importConfig(context) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "清除配置",
                    summary = "清除全部模块配置 (警告: 此操作不可逆!)",
                    icon = MaterialSymbols.Outlined.Delete_forever,
                    onClick = { showClearConfirm = true },
                )
            }
        }

        // 更新
        item {
            Material3SectionTitle("更新")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                M3ArrowItem(
                    title = "检查更新",
                    summary = "立即检查模块是否有新版本并自动下载",
                    icon = MaterialSymbols.Outlined.Update,
                    onClick = {
                        checkForUpdate(
                            onAvailable = { updateInfo = it },
                            onError = { updateError = it },
                        )
                    },
                )
            }
        }

        // 关于
        item {
            Material3SectionTitle("关于")
            M3PreferenceGroup(Modifier.padding(top = 4.dp)) {
                M3InfoItem(
                    title = "版本",
                    summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    icon = MaterialSymbols.Outlined.Label,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3InfoItem(
                    title = "构建提交时间",
                    summary = formatEpoch(BuildConfig.BUILD_TIMESTAMP, true),
                    icon = MaterialSymbols.Outlined.Build_circle,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3InfoItem(
                    title = "提示",
                    summary = "牙膏要一点一点挤, 显卡要一刀一刀切, PPT 要一张一张放, 代码要一行一行写, 单个功能预计自出现在 commit 之日起, 三年内开发完毕",
                    icon = MaterialSymbols.Outlined.Lightbulb_2,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "捐赠",
                    summary = "支持项目开发 (模块完全开源免费, 捐赠无特权)",
                    icon = MaterialSymbols.Outlined.Volunteer_activism,
                    onClick = { /* Same as Miuix version - open QR reward UI */ },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "开放源代码许可",
                    summary = "本项目使用的开放源代码库许可",
                    icon = MaterialSymbols.Outlined.License,
                    onClick = onOpenLicense,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "GitHub",
                    summary = "Ujhhgtg/WeKit",
                    icon = GitHubIcon,
                    onClick = { /* Open GitHub */ },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                M3ArrowItem(
                    title = "Telegram",
                    summary = "Telegram 超级群组",
                    icon = TelegramIcon,
                    onClick = { /* Open Telegram */ },
                )
            }
        }

        item { Spacer(Modifier.height(CONTENT_BOTTOM_INSET)) }
    }
}

// ---------------------------------------------------------------------------
//  Material 3 Theme Section
// ---------------------------------------------------------------------------

@Composable
private fun Material3ThemeSection() {
    M3EnumDropdownItem(
        title = "UI 组件引擎",
        entries = AppUiEngine.entries,
        selected = ThemeSettings.uiEngine,
        labelOf = { it.displayName },
        onSelected = { ThemeSettings.updateUiEngine(it) },
        icon = MaterialSymbols.Outlined.Layers,
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    M3EnumDropdownItem(
        title = "主题模式",
        entries = AppThemeMode.entries,
        selected = ThemeSettings.themeMode,
        labelOf = { it.displayName },
        onSelected = { ThemeSettings.updateThemeMode(it) },
        icon = MaterialSymbols.Outlined.Brightness_medium,
    )

    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    var customColor by remember { mutableStateOf(ThemeSettings.customColor) }
    M3SwitchItem(
        title = "自定义颜色",
        summary = "使用调色板样式生成配色, 而非 Miuix 默认蓝",
        checked = customColor,
        onCheckedChange = { customColor = it; ThemeSettings.updateCustomColor(it) },
        icon = MaterialSymbols.Outlined.Palette,
    )

    if (customColor) {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        var dynamicWallpaper by remember { mutableStateOf(ThemeSettings.dynamicWallpaper) }
        M3SwitchItem(
            title = "动态壁纸取色",
            summary = "使用系统壁纸的强调色作为种子\n需系统 Android SDK >= 31",
            checked = dynamicWallpaper,
            onCheckedChange = { dynamicWallpaper = it; ThemeSettings.updateDynamicWallpaper(it) },
            icon = MaterialSymbols.Outlined.Wallpaper,
        )

        if (!dynamicWallpaper) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            var showColorPicker by remember { mutableStateOf(false) }
            M3SeedColorPickerDialog(show = showColorPicker, onDismiss = { showColorPicker = false })
            M3ArrowItem(
                title = "种子颜色",
                summary = "点击选择配色的种子颜色",
                icon = MaterialSymbols.Outlined.Colorize,
                onClick = { showColorPicker = true },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(ThemeSettings.seedColor)),
                    )
                },
            )
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        M3EnumDropdownItem(
            title = "调色板样式",
            entries = AppPaletteStyle.entries,
            selected = ThemeSettings.paletteStyle,
            labelOf = { it.displayName },
            onSelected = {
                ThemeSettings.updatePaletteStyle(it)
                if (!it.supportsSpec2025 && ThemeSettings.colorSpec == AppColorSpec.SPEC_2025) {
                    ThemeSettings.updateColorSpec(AppColorSpec.SPEC_2021)
                }
            },
            icon = MaterialSymbols.Outlined.Style,
        )

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        val spec2025Supported = ThemeSettings.paletteStyle.supportsSpec2025
        M3EnumDropdownItem(
            title = "颜色规格",
            entries = if (spec2025Supported) AppColorSpec.entries else listOf(AppColorSpec.SPEC_2021),
            selected = ThemeSettings.effectiveColorSpec,
            labelOf = { it.displayName },
            onSelected = { ThemeSettings.updateColorSpec(it) },
            enabled = spec2025Supported,
            icon = MaterialSymbols.Outlined.Contrast,
            summaryOverride = if (!spec2025Supported) "当前调色板样式仅支持 Material 3 (2021)" else null,
        )

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        var applyToWechat by remember { mutableStateOf(ThemeSettings.applyToWechat) }
        M3SwitchItem(
            title = "同时对微信生效",
            summary = "将自定义配色应用到微信本身",
            checked = applyToWechat,
            onCheckedChange = {
                applyToWechat = it
                ThemeSettings.updateApplyToWechat(it)
                CoroutineScope(Dispatchers.Main).launch { showToastSuspend("重启微信生效") }
            },
            icon = MaterialSymbols.Outlined.Sync,
        )
    }
}

// ---------------------------------------------------------------------------
//  Material 3 Color Picker Dialog
// ---------------------------------------------------------------------------

@Composable
private fun M3SeedColorPickerDialog(show: Boolean, onDismiss: () -> Unit) {
    var picked by remember(show) { mutableStateOf(Color(ThemeSettings.seedColor)) }

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("自定义颜色") },
            text = {
                Column {
                    // Reuse Miuix ColorPicker (it's functional) but in an M3 dialog
                    ColorPicker(color = picked, onColorChanged = { picked = it })
                    Spacer(Modifier.height(16.dp))
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { picked = Color(ThemeSettings.DEFAULT_SEED_COLOR) }) {
                        Text("重置")
                    }
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        ThemeSettings.updateSeedColor(picked.toArgb())
                        onDismiss()
                    }) {
                        Text("确定")
                    }
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
//  Material 3 License Screen
// ---------------------------------------------------------------------------

@Composable
fun Material3LicenseScreen(onBack: () -> Unit) {
    val libraries = remember {
        Libs.Builder().withJson(AboutLibrariesProvider.ABOUT_LIBRARIES_JSON).build().libraries
    }

    val queryState = rememberTextFieldState()
    val query = queryState.text.toString()
    val filtered = remember(query, libraries) {
        if (query.isBlank()) libraries
        else libraries.filter { lib ->
            lib.name.contains(query, ignoreCase = true) ||
                    lib.developers.any { it.name?.contains(query, ignoreCase = true) == true } ||
                    lib.description?.contains(query, ignoreCase = true) == true
        }
    }

    Material3ListScaffold(
        title = "开放源代码库",
        navigationIcon = { M3BackButton(onBack) },
    ) {
        item {
            OutlinedTextField(
                state = queryState,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                placeholder = { Text("搜索库") },
                leadingIcon = { Icon(MaterialSymbols.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { queryState.clearText() }) {
                            Icon(MaterialSymbols.Outlined.Close, "清除")
                        }
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        }

        item {
            Material3SectionTitle(
                if (query.isBlank()) "${libraries.size} 个库" else "${filtered.size}/${libraries.size} 个库"
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("找不到「$query」的结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filtered, key = { it.uniqueId }) { library ->
                M3LibraryRow(library, modifier = Modifier.padding(top = 12.dp))
            }
        }

        item { Spacer(Modifier.height(CONTENT_BOTTOM_INSET)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3LibraryRow(library: Library, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                library.artifactVersion?.let { version ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val author = library.developers.firstOrNull()?.name ?: library.organization?.name
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            library.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (library.licenses.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    library.licenses.forEach { license ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        ) {
                            Text(
                                text = license.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  M3 preference helpers
// ---------------------------------------------------------------------------

@Composable
private fun M3PrefSwitch(key: String, title: String, summary: String, icon: ImageVector) {
    var checked by remember { mutableStateOf(WePrefs.getBoolOrFalse(key)) }
    M3SwitchItem(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { checked = it; WePrefs.putBool(key, it) },
        icon = icon,
    )
}

// ---------------------------------------------------------------------------
//  M3 dialog helpers
// ---------------------------------------------------------------------------

@Composable
private fun M3ClearConfigDialog(show: Boolean, onDismiss: () -> Unit) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("清除模块配置") },
            text = { Text("确定清除配置? (警告: 此操作不可逆!)") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        CoroutineScope(Dispatchers.IO).launch {
                            showToastSuspend("正在清除...")
                            WePrefs.default.clear()
                            showToastSuspend("清除成功!")
                        }
                    },
                ) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        )
    }
}

@Composable
private fun M3UpdateAvailableDialog(info: UpdateResult.UpdateAvailable?, onDismiss: () -> Unit, context: Context) {
    if (info != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("检测到新版本") },
            text = {
                Text("当前版本: ${BuildConfig.VERSION_NAME}\n新版本: ${info.info.versionName}\n是否下载并安装?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        CoroutineScope(Dispatchers.Default).launch {
                            AppUpdater.downloadAndInstall(context, info.info)
                        }
                    },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        )
    }
}

@Composable
private fun M3UpdateErrorDialog(message: String?, onDismiss: () -> Unit) {
    if (message != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("检查更新失败") },
            text = { Text("错误信息: $message") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        )
    }
}
