package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "阻止消息撤回 1", categories = ["聊天"], description = "无撤回提示")
object AntiMessageRecall1 : SwitchHookItem(), IResolveDex {

    private val methodRevokeMsg by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodRevokeMsg.find(dexKit) {
            matcher {
                usingEqStrings("doRevokeMsg xmlSrvMsgId=%d talker=%s isGet=%s")
            }
        }
    }

    override fun onEnable() {
        methodRevokeMsg.hookBefore {
            result = null
        }
    }
}
