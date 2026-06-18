package dev.ujhhgtg.wekit.hooks.items.profile

import android.view.View
import android.widget.TextView
import com.tencent.mm.plugin.setting.ui.setting.EditSignatureUI
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.toClass
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.hookBeforeDirectly
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "移除个性签名限制", categories = ["个人资料"], description = "允许大于 30 字与包含特殊字符的个性签名")
object RemoveSignatureLimits : SwitchHookItem(), IResolveDex {

    private lateinit var stringMatchesMethodUnhook: XC_MethodHook.Unhook

    private lateinit var setFiltersUnhook: XC_MethodHook.Unhook

    override fun onEnable() {
        EditSignatureUI::class.reflekt()
            .firstMethod { name = "initView" }.apply {
                hookBefore {
                    setFiltersUnhook = "${PackageNames.WECHAT}.ui.widget.MMEditText".toClass().reflekt()
                        .firstMethod {
                            name = "setFilters"
                        }.hookBeforeDirectly {
                            result = null
                        }
                }

                hookAfter {
                    val activity = thisObject as EditSignatureUI
                    activity.enableOptionMenu(true)
                    (activity.reflekt()
                        .firstField { type = TextView::class }
                        .get()!! as TextView).visibility = View.GONE
                }
            }

        methodTextWatcherAfterTextChanged.hookBefore {
            result = null
        }

        methodConfirmButtonOnClickListenerOnClick.apply {
            hookBefore {
                stringMatchesMethodUnhook = String::class.java.reflekt()
                    .firstMethod { name = "matches" }
                    .hookBeforeDirectly { result = false }
            }
            hookAfter {
                stringMatchesMethodUnhook.unhook()
                setFiltersUnhook.unhook()
            }
        }
    }

    private val methodTextWatcherAfterTextChanged by dexMethod()

    private val methodConfirmButtonOnClickListenerOnClick by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodTextWatcherAfterTextChanged.find(dexKit) {
            searchPackages("${PackageNames.WECHAT}.plugin.setting.ui.setting")
            matcher {
                declaredClass {
                    addMethod {
                        name = "<init>"
                        paramTypes("${PackageNames.WECHAT}.plugin.setting.ui.setting.EditSignatureUI", "java.lang.String")
                    }
                    addInterface { className = "android.text.TextWatcher" }
                }

                name = "afterTextChanged"
            }
        }

        methodConfirmButtonOnClickListenerOnClick.find(dexKit) {
            searchPackages("${PackageNames.WECHAT}.plugin.setting.ui.setting")
            matcher {
                declaredClass {
                    addMethod {
                        name = "<init>"
                        paramTypes("${PackageNames.WECHAT}.plugin.setting.ui.setting.EditSignatureUI")
                    }
                    addInterface { className = $$"android.view.MenuItem$OnMenuItemClickListener" }
                }

                name = "onMenuItemClick"
                usingEqStrings(".*[", "].*")
            }
        }
    }
}
