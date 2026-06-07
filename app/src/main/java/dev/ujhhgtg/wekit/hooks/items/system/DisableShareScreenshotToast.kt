package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "禁用「转发截图」提示", categories = ["系统与隐私"], description = "你在教我做事?")
object DisableShareScreenshotToast : SwitchHookItem(), IResolvesDex {

    private val methodDisplayToast by dexMethod()

    override fun onEnable() {
        methodDisplayToast.hookBefore {
            result = null
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodDisplayToast.find(dexKit) {
            searchPackages("com.tencent.mm.ui.feature.api.screenshot")
            matcher {
                usingEqStrings("MicroMsg.ScreenShotShareService", "showShareTongue, shareTongue already showing, reset onClick & countDown")
            }
        }
    }
}
