package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "移除分享签名校验", categories = ["系统与隐私"], description = "移除第三方应用分享到微信的签名校验")
object RemoveExternalAppSharingSignatureVerify : SwitchHookItem(), IResolvesDex {

    private val methodSignCheck by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodSignCheck.find(dexKit) {
            searchPackages("com.tencent.mm.pluginsdk.model.app")
            matcher {
                usingEqStrings("checkAppSignature get local signature failed")
            }
        }
    }

    override fun onEnable() {
        methodSignCheck.hookBefore {
            result = true
        }
    }
}
