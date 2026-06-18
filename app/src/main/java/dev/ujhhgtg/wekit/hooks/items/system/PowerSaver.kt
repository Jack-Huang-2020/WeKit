package dev.ujhhgtg.wekit.hooks.items.system

import android.os.PowerManager
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

@HookItem(name = "省电模式", categories = ["系统与隐私"], description = "通过一些措施, 减少微信耗电量")
object PowerSaver : SwitchHookItem() {

    override fun onEnable() {
        PowerManager.WakeLock::class.reflekt().apply {
            methods {
                name = "acquire"
            }.forEach {
                it.hookBefore { result = null }
            }

            firstMethod {
                name = "release"
                parameterCount = 1
            }.hookBefore { result = null }
        }
    }
}
