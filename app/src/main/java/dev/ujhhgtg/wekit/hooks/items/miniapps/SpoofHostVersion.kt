package dev.ujhhgtg.wekit.hooks.items.miniapps

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexConstructor
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "伪装宿主版本", categories = ["小程序"], description = "解决提示版本较低无法使用部分小程序")
object SpoofHostVersion : SwitchHookItem(), IResolvesDex {

    override fun onEnable() {
        ctorCgiLaunchWxaAppFunc1122.hookBefore {
            args[6] = 9999
        }
    }

    private val ctorCgiLaunchWxaAppFunc1122 by dexConstructor()

    override fun resolveDex(dexKit: DexKitBridge) {
        ctorCgiLaunchWxaAppFunc1122.find(dexKit) {
            matcher {
                usingEqStrings(
                    "MicroMsg.AppBrand.CgiLaunchWxaApp|func:1122",
                    "<init> cgiHash[%d], username[%s] appId[%s] sync[%b] sessionId[%s] instanceId[%s] libVersion[%d], source:%s, launchMode:%d, migrate:%b, fallback:%b"
                )
            }
        }
    }
}
