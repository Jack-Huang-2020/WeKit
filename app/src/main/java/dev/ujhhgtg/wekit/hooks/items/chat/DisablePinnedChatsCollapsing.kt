package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "禁用置顶聊天折叠", categories = ["聊天"], description = "隐藏「折叠置顶聊天」选项\n启用本功能后, 需重启微信 2 次以使更改完全生效")
object DisablePinnedChatsCollapsing : SwitchHookItem(), IResolveDex {

    private val methodAddCollapseChatItem by dexMethod()
    private val methodIfShouldAddCollapseChatItem by dexMethod()

    override fun onEnable() {
        methodAddCollapseChatItem.hookBefore {
            WeDatabaseApi.execStatement("DELETE FROM rconversation WHERE username = 'message_fold'")
            result = null
        }
        methodIfShouldAddCollapseChatItem.hookBefore {
            WeDatabaseApi.execStatement("DELETE FROM rconversation WHERE username = 'message_fold'")
            result = false
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodAddCollapseChatItem.find(dexKit) {
            searchPackages("com.tencent.mm.ui.conversation")
            matcher {
                usingEqStrings("MicroMsg.FolderHelper", "fold item exist")
            }
        }

        methodIfShouldAddCollapseChatItem.find(dexKit) {
            searchPackages("com.tencent.mm.ui.conversation")
            matcher {
                usingEqStrings("MicroMsg.FolderHelper", "checkIfShowFoldItem, ifShow:")
                returnType(Boolean::class.java)
            }
        }
    }
}
