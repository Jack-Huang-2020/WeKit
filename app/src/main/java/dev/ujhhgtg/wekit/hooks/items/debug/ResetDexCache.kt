package dev.ujhhgtg.wekit.hooks.items.debug

import android.content.Context
import androidx.compose.material3.Text
import dev.ujhhgtg.wekit.dexkit.cache.DexCacheManager
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.android.showToastSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@HookItem(name = "重置适配信息", categories = ["调试"], description = "清除全部 DEX 适配信息, 等待下次启动时重新适配")
object ResetDexCache : ClickableHookItem() {

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text("清除适配信息") },
                text = {
                    Text(
                        "这将删除所有的 DEX 适配信息，宿主重启后需要重新适配。\n" +
                                "确定清除吗？"
                    )
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        runBlocking(Dispatchers.IO) {
                            DexCacheManager.clearAllCache()
                            showToastSuspend("清除成功!")
                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    }) { Text("确定") }
                })
        }
    }

    override val noSwitchWidget = true
}
