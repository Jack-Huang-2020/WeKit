package dev.ujhhgtg.wekit.hooks.items.moments

import com.tencent.mm.plugin.sns.storage.ADInfo
import dev.ujhhgtg.comptime.nameOf
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.reflection.resolve

@HookItem(name = "拦截朋友圈广告", categories = ["朋友圈"], description = "拦截朋友圈广告")
object RemoveMomentsAds : SwitchHookItem() {

    private val TAG = nameOf(RemoveMomentsAds)

    override fun onEnable() {
        ADInfo::class.resolve()
            .firstConstructor {
                parameters(String::class)
            }
            .hookBefore {
                WeLogger.i(TAG, "blocked ad")
                result = null
            }
    }
}
