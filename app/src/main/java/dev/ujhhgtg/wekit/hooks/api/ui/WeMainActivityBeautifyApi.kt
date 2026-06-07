package dev.ujhhgtg.wekit.hooks.api.ui

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "微信主屏幕美化服务", categories = ["API"], description = "提供美化微信主屏幕的能力")
object WeMainActivityBeautifyApi : ApiHookItem(), IResolvesDex {

    val methodDoOnCreate by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodDoOnCreate.find(dexKit) {
            matcher {
                declaredClass = "com.tencent.mm.ui.MainTabUI"
                usingEqStrings("MicroMsg.LauncherUI.MainTabUI", "doOnCreate")
            }
        }
    }
}
