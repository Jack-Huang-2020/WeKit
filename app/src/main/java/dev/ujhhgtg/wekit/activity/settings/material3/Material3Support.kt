package dev.ujhhgtg.wekit.activity.settings.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Arrow_back
import com.composables.icons.materialsymbols.outlined.Arrow_drop_down
import com.composables.icons.materialsymbols.outlined.Arrow_forward

// ---------------------------------------------------------------------------
//  Shared Material 3 scaffold
// ---------------------------------------------------------------------------

/**
 * Material 3 equivalent of [dev.ujhhgtg.wekit.activity.settings.MiuixListScaffold]: a [LargeTopAppBar] (collapses on scroll, no blur)
 * over a [LazyColumn] with standard edge-to-edge padding. Used by all M3 settings screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3ListScaffold(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = { navigationIcon?.invoke() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
//  Section title
// ---------------------------------------------------------------------------

/** Material 3 equivalent of [dev.ujhhgtg.wekit.ui.content.MiuixSmallTitle]: small label in [MaterialTheme.colorScheme.primary]. */
@Composable
fun Material3SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------------------
//  Preference row helpers
// ---------------------------------------------------------------------------

/** A non-interactive info [ListItem] (no trailing action, no click ripple). */
@Composable
fun M3InfoItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
    )
}

/** A [ListItem] with a trailing [Switch] and full-row click-to-toggle. */
@Composable
fun M3SwitchItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
    )
}

/** A [ListItem] with a trailing chevron arrow and a click handler. */
@Composable
fun M3ArrowItem(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = trailingContent ?: (
            if (onClick != null) ({ Icon(MaterialSymbols.Outlined.Arrow_forward, "打开") }) else null
        ),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

/**
 * A [ListItem] dropdown backed by an [AlertDialog] with [RadioButton]s — Material 3 equivalent of
 * Miuix's `WindowDropdownPreference`.
 */
@Composable
fun <T> M3EnumDropdownItem(
    title: String,
    entries: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    summaryOverride: String? = null,
) {
    var open by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summaryOverride ?: labelOf(selected)) },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = { Icon(MaterialSymbols.Outlined.Arrow_drop_down, null) },
        modifier = if (enabled) Modifier.clickable { open = true } else Modifier,
    )

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(entry); open = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = entry == selected,
                                onClick = { onSelected(entry); open = false },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(labelOf(entry), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("取消") }
            },
        )
    }
}

// ---------------------------------------------------------------------------
//  Back navigation icon
// ---------------------------------------------------------------------------

@Composable
fun M3BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(MaterialSymbols.Outlined.Arrow_back, contentDescription = "返回")
    }
}

// ---------------------------------------------------------------------------
//  Card group — wraps a list of items in an OutlinedCard with dividers
// ---------------------------------------------------------------------------

/**
 * Emits an M3-styled card group: an [androidx.compose.material3.OutlinedCard] containing each
 * [content] item separated by [HorizontalDivider]s. Equivalent to Miuix's `Card { forEach }`.
 */
@Composable
fun M3PreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        content()
    }
}
