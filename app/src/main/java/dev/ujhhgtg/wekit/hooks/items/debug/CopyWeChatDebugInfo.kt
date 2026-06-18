package dev.ujhhgtg.wekit.hooks.items.debug

import android.content.Context
import android.view.View
import android.widget.TextView
import dev.ujhhgtg.reflekt.utils.createInstance
import com.tencent.mm.plugin.setting.ui.setting.SettingsAboutMMHeaderPreference
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.android.copyToClipboard
import dev.ujhhgtg.wekit.utils.android.showToast
import dev.ujhhgtg.wekit.utils.hookBeforeDirectly
import dev.ujhhgtg.reflekt.reflekt
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "复制调试信息", categories = ["调试"], description = "在报告模块问题时, 请附上本功能的结果")
object CopyWeChatDebugInfo : ClickableHookItem(), IResolveDex {

    override val noSwitchWidget = true

    override fun onClick(context: Context) {
        val unhook = TextView::class.reflekt()
            .firstMethod {
                name = "setText"
                parameters(CharSequence::class)
            }.hookBeforeDirectly {
                val debugText = (args[0] as StringBuilder).toString()
                copyToClipboard(context, debugText)
                showToast(context, "已复制")
                throwable = RuntimeException("halt method")
            }

        val onClickListener = methodOnClick.method.declaringClass
            .createInstance(SettingsAboutMMHeaderPreference(context))
        // WeChat has a check:
        // long jCurrentTimeMillis = System.currentTimeMillis();
        //        long j16 = this.f158935d;
        //        if (j16 > jCurrentTimeMillis || jCurrentTimeMillis - j16 > 300) {
        //            this.f158935d = jCurrentTimeMillis;
        //            return;
        //        }
        onClickListener.reflekt()
            .firstField {
                type = Long::class
            }.set(System.currentTimeMillis())

        runCatching {
            methodOnClick.method.invoke(onClickListener, View(context))
        }
        unhook.unhook()
    }

    private val methodOnClick by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodOnClick.find(dexKit) {
            searchPackages("com.tencent.mm.plugin.setting.ui.setting")
            matcher {
                name = "onClick"
                usingEqStrings("com/tencent/mm/plugin/setting/ui/setting/SettingsAboutMMHeaderPreference$1", $$"android/view/View$OnClickListener", "onClick")
            }
        }
    }
}
