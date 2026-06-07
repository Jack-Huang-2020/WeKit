package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

@HookItem(name = "一键撤回并重新编辑", categories = ["聊天"], description = "向消息长按菜单添加菜单项, 可快捷撤回消息并将文本内容加入输入框 (没写完)")
object QuickRevokeAndEdit : SwitchHookItem()
