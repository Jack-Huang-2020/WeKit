package dev.ujhhgtg.wekit.activity.settings.material3

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Check_circle
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.activity.settings.CONTENT_BOTTOM_INSET
import dev.ujhhgtg.wekit.activity.settings.miuix.openLsposedManager
import dev.ujhhgtg.wekit.features.core.FeaturesProvider
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.formatEpoch

@Composable
fun Material3HomePager(onOpenFeatures: () -> Unit) {
    val enabledCount = remember {
        FeaturesProvider.ALL_HOOK_ITEMS.count { WePrefs.getBoolOrFalse(it.name) }
    }
    val totalCount = remember { FeaturesProvider.ALL_HOOK_ITEMS.size }

    Material3ListScaffold(title = "WeKit") {
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                M3StatusRow(enabledCount = enabledCount, totalCount = totalCount, onOpenFeatures = onOpenFeatures)
                M3SystemInfoCard()
                Spacer(Modifier.height(CONTENT_BOTTOM_INSET))
            }
        }
    }
}

@Composable
private fun M3StatusRow(enabledCount: Int, totalCount: Int, onOpenFeatures: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Activation status card — tapping opens LSPosed manager
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1A3825) else Color(0xFFDFFAE4),
            ),
            onClick = { openLsposedManager(context) },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = MaterialSymbols.Outlined.Check_circle,
                        tint = Color(0xFF36D167),
                        contentDescription = null,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    Text("模块已激活", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(BuildConfig.VERSION_NAME, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Feature counts
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            M3CountCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "已启用功能",
                value = enabledCount.toString(),
                onClick = onOpenFeatures,
            )
            Spacer(Modifier.height(12.dp))
            M3CountCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "全部功能",
                value = totalCount.toString(),
                onClick = onOpenFeatures,
            )
        }
    }
}

@Composable
private fun M3CountCard(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun M3SystemInfoCard() {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            M3InfoText("微信版本", "${HostInfo.versionName} (${HostInfo.versionCode})")
            M3InfoText("模块版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            M3InfoText("构建时间", formatEpoch(BuildConfig.BUILD_TIMESTAMP, true))
            M3InfoText("设备型号", "${Build.MANUFACTURER} ${Build.MODEL}")
            M3InfoText("Android 版本", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", bottomPadding = 0.dp)
        }
    }
}

@Composable
private fun M3InfoText(title: String, content: String, bottomPadding: Dp = 20.dp) {
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
}
