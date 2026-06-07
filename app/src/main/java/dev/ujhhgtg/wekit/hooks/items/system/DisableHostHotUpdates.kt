package dev.ujhhgtg.wekit.hooks.items.system

import android.annotation.SuppressLint
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.reflection.resolve
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@HookItem(name = "禁用微信热更新", categories = ["系统与隐私"], description = "禁止微信热更新, 避免被强制更新到不兼容版本")
object DisableHostHotUpdates : SwitchHookItem() {

    @SuppressLint("SdCardPath")
    @OptIn(ExperimentalPathApi::class)
    override fun onEnable() {
        runCatching { Path("/data/data/${HostInfo.packageName}/tinker").deleteRecursively() }

        ShareTinkerInternals::class.resolve()
            .method {
                name {
                    it.startsWith("isTinkerEnabled")
                }
            }
            .forEach {
                it.hookBefore {
                    result = false
                }
            }
    }
}
