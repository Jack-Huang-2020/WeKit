package dev.ujhhgtg.wekit.hooks.items.debug

import android.content.Context
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem

@HookItem(name = "测试", categories = ["调试"], description = "???")
object Experiments : ClickableHookItem() {

    override val noSwitchWidget = true

    override fun onClick(context: Context) {

    }
}
