package dev.ujhhgtg.wekit.hooks.items.system

import android.provider.Settings
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.reflekt.reflekt
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "环境伪装", categories = ["系统与隐私"], description = "伪装未启用 ADB, 开发者选项或 VPN, 可能有助于通过人脸等场景下的环境安全性检测")
object SpoofEnvironment : SwitchHookItem(), IResolveDex {

    override fun onEnable() {
        Settings.Global::class.reflekt()
            .firstMethod {
                name = "getInt"
                parameterCount = 3
            }.hookBefore {
                val name = args[1] as? String? ?: return@hookBefore
                if (name == "adb_enabled")
                    result = 0
            }

        Settings.Secure::class.reflekt()
            .firstMethod {
                name = "getInt"
                parameterCount = 3
            }.hookBefore {
                val name = args[1] as? String? ?: return@hookBefore
                if (name == "development_settings_enabled")
                    result = 0
            }

        methodIsVpnEnabled.hookBefore {
            result = false
        }
    }

    private val methodIsVpnEnabled by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodIsVpnEnabled.find(dexKit) {
            matcher {
                declaredClass {
                    usingEqStrings("MicroMsg.WalletSecurityUtilService")
                }

                usingEqStrings("connectivity")
                usingNumbers(4)
            }
        }
    }
}
