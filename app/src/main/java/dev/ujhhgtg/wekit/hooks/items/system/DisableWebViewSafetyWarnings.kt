package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "禁用 WebView 安全警告", categories = ["系统与隐私"], description = "禁用 WebView 相关的安全警告提示")
object DisableWebViewSafetyWarnings : SwitchHookItem(), IResolveDex {
    private val methodGetIsInterceptEnabled by dexMethod()
    private val methodGetIsUrlSafe by dexMethod()

    override fun onEnable() {
        methodGetIsInterceptEnabled.hookBefore {
            result = false
        }

        methodGetIsUrlSafe.hookBefore {
            result = true
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetIsInterceptEnabled.find(dexKit) {
            matcher {
                usingEqStrings(
                    "MicroMsg.WebViewHighRiskAdH5Interceptor",
                    "isInterceptEnabled, expt="
                )
            }
        }

        methodGetIsUrlSafe.find(dexKit) {
            matcher {
                declaredClass(methodGetIsInterceptEnabled.method.declaringClass)
                usingEqStrings("http", "https")
            }
        }
    }
}
