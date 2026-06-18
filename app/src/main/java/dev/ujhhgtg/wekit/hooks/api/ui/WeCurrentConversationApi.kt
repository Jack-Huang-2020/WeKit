package dev.ujhhgtg.wekit.hooks.api.ui

import com.tencent.mm.pluginsdk.ui.chat.ChatFooter
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.reflekt.reflekt

@HookItem(name = "当前聊天服务", categories = ["API"], description = "提供当前界面所在的聊天")
object WeCurrentConversationApi : ApiHookItem() {

    lateinit var value: String

    override fun onEnable() {
        ChatFooter::class.reflekt()
            .firstMethod {
                name = "setUserName"
            }.hookAfter {
                val conv = args[0] as? String
                if (!conv.isNullOrEmpty()) {
                    value = conv
                }
            }
    }
}
