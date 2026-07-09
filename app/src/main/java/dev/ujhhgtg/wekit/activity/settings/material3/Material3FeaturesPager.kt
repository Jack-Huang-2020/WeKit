package dev.ujhhgtg.wekit.activity.settings.material3

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Close
import com.composables.icons.materialsymbols.outlined.Search
import com.composables.icons.materialsymbols.outlined.Settings
import dev.ujhhgtg.wekit.activity.settings.CONTENT_BOTTOM_INSET
import dev.ujhhgtg.wekit.activity.settings.FEATURE_CATEGORIES
import dev.ujhhgtg.wekit.activity.settings.LocalComponentActivity
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeaturesProvider
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.features.items.easter_egg.AprilFools
import dev.ujhhgtg.wekit.features.items.easter_egg.isAprilFools
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.showToastSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------------------------------------------------------------------------
//  Page 1 — Features (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3FeaturesPager(onOpenCategory: (String) -> Unit) {
    val showAprilFools = remember { LocalDate.now().isAprilFools }
    val queryState = rememberTextFieldState()
    val query = queryState.text.toString()
    val searching = query.isNotBlank()

    val searchableItems = remember { FeaturesProvider.ALL_HOOK_ITEMS.filterIsInstance<SwitchFeature>() }
    val filteredItems = remember(query) {
        if (!searching) emptyList()
        else searchableItems.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
    }
    val switchStates = remember { mutableStateMapOf<String, Boolean>() }

    BackHandler(enabled = searching) { queryState.clearText() }

    Material3ListScaffold(title = "功能") {
        item {
            OutlinedTextField(
                state = queryState,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                placeholder = { Text("搜索功能") },
                leadingIcon = { Icon(MaterialSymbols.Outlined.Search, null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { queryState.clearText() }) {
                            Icon(MaterialSymbols.Outlined.Close, "清除搜索")
                        }
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        }

        if (searching) {
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "未匹配到任何相关功能",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        filteredItems.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            Material3FeatureRow(
                                item = item,
                                checked = switchStates[item.name] ?: WePrefs.getBoolOrFalse(item.name),
                                onCheckedChange = { switchStates[item.name] = it },
                            )
                        }
                    }
                }
            }
        } else {
            if (showAprilFools) {
                item {
                    OutlinedCard(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        M3ArrowItem(
                            title = "🏳",
                            summary = "投降喵投降喵",
                            onClick = {
                                WePrefs.putBool(
                                    AprilFools.KEY_SURRENDER,
                                    true
                                )
                                CoroutineScope(Dispatchers.Main).launch { showToastSuspend("重启生效") }
                            },
                        )
                    }
                }
            }

            item {
                OutlinedCard(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    FEATURE_CATEGORIES.forEachIndexed { index, (name, icon) ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text(name) },
                            leadingContent = { Icon(icon, contentDescription = null) },
                            trailingContent = { Icon(MaterialSymbols.Outlined.Settings, null) },
                            modifier = Modifier.clickable { onOpenCategory(name) },
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(CONTENT_BOTTOM_INSET)) }
    }
}

// ---------------------------------------------------------------------------
//  Category detail (Material 3)
// ---------------------------------------------------------------------------

@Composable
fun Material3CategoryDetailScreen(categoryName: String, onBack: () -> Unit) {
    val items = remember(categoryName) {
        FeaturesProvider.ALL_HOOK_ITEMS.filter { categoryName in it.categories }
    }
    val switchStates = remember(categoryName) {
        mutableStateMapOf<String, Boolean>().apply {
            items.forEach { put(it.name, WePrefs.getBoolOrFalse(it.name)) }
        }
    }

    Material3ListScaffold(
        title = categoryName,
        navigationIcon = { M3BackButton(onBack) },
    ) {
        if (items.isEmpty()) return@Material3ListScaffold

        item {
            OutlinedCard(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Column {
                        Material3FeatureRow(
                            item = item,
                            checked = switchStates[item.name] ?: false,
                            onCheckedChange = { switchStates[item.name] = it },
                        )
                        item.Ui()
                    }
                }
            }
        }

        item { Spacer(Modifier.height(CONTENT_BOTTOM_INSET)) }
    }
}

// ---------------------------------------------------------------------------
//  Feature row (Material 3)
// ---------------------------------------------------------------------------

/**
 * Material 3 equivalent of [dev.ujhhgtg.wekit.activity.settings.FeatureRow]: a [ListItem]-based preference row that handles both
 * [SwitchFeature] and [ClickableFeature].
 */
@Composable
fun Material3FeatureRow(
    item: BaseFeature,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalComponentActivity.current
    val configKey = item.name

    DisposableEffect(configKey) {
        (item as SwitchFeature).setToggleCompletionCallback { onCheckedChange(item.isEnabled) }
        onDispose {}
    }

    fun toggle(requested: Boolean) {
        item as SwitchFeature
        if (item.onBeforeToggle(requested, context)) {
            WePrefs.putBool(configKey, requested)
            item.isEnabled = requested
            onCheckedChange(requested)
        }
    }

    when (item) {
        is ClickableFeature -> {
            var localChecked by remember(configKey, checked) { mutableStateOf(checked) }
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Icon(
                            imageVector = MaterialSymbols.Outlined.Settings,
                            contentDescription = "可配置",
                            modifier = Modifier.padding(start = 6.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                supportingContent = { Text(item.description) },
                trailingContent = if (!item.noSwitchWidget) ({
                    Switch(
                        checked = localChecked,
                        onCheckedChange = { toggle(it); localChecked = it },
                    )
                }) else null,
                modifier = Modifier.clickable {
                    runCatching { item.onClick(context) }
                        .onFailure { WeLogger.e("SettingsActivity", "onClick failed for ${item.displayName}", it) }
                },
            )
        }

        is SwitchFeature -> {
            var localChecked by remember(configKey, checked) { mutableStateOf(checked) }
            ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = { Text(item.description) },
                trailingContent = {
                    Switch(
                        checked = localChecked,
                        onCheckedChange = { toggle(it); localChecked = it },
                    )
                },
                modifier = Modifier.clickable { toggle(!localChecked); localChecked = !localChecked },
            )
        }
    }
}
