package dev.ujhhgtg.wekit.hooks.items.contacts

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.hooks.api.core.WeApi
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.hooks.api.core.models.WeContact
import dev.ujhhgtg.wekit.hooks.api.net.WePacketHelper
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.DefaultColumn
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.copyToClipboard
import dev.ujhhgtg.wekit.utils.android.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.seconds

@HookItem(name = "检测单向删除好友", categories = ["联系人与群组"], description = "批量扫描全部好友, 检测是否被对方单向删除")
object DetectDeletedFriends : ClickableHookItem() {

    override val noSwitchWidget = true

    private val TAG = This.Class.simpleName

    private enum class AbnormalFriendStatus(val displayName: String) {
        ThatAccountBanned("对方账号异常"),
        ThatBlockedThis("被拉黑"),
        ThatDeletedThis("被删除")
    }

    private data class AbnormalFriend(
        val contact: WeContact,
        val status: AbnormalFriendStatus
    )

    private sealed class DialogPhase {
        data object Idle : DialogPhase()
        data class Scanning(
            val completed: MutableIntState,
            val total: Int,
            val abnormalFriends: MutableList<AbnormalFriend> = mutableListOf()
        ) : DialogPhase()
        data class Done(val friends: List<AbnormalFriend>) : DialogPhase()
    }

    override fun onClick(context: Context) {
        val friends = WeDatabaseApi.getFriends().filter { c ->
            c.type != 2051 && c.type != 2049 && c.wxId != WeApi.selfWxId
        }

        showComposeDialog(context) {
            var phase by remember { mutableStateOf<DialogPhase>(DialogPhase.Idle) }

            LaunchedEffect(phase) {
                if (phase is DialogPhase.Scanning) {
                    dialog.setCancelable(false)
                    CoroutineScope(Dispatchers.IO).launch {
                        val scanningPhase = phase as DialogPhase.Scanning
                        val abnormalFriends = scanningPhase.abnormalFriends
                        for (friend in friends) {
                            // detect whether user quitted halfway
                            if (phase !is DialogPhase.Scanning) {
                                break
                            }

                            WePacketHelper.sendCgi(
                                "/cgi-bin/mmpay-bin/beforetransfer", 2783, 0, 0,
                                """{"2":"${friend.wxId}"}"""
                            ) {
                                // status is always success
                                onSuccess { json, _ ->
                                    val jsonObj = Json.parseToJsonElement(json).jsonObject
                                    val realName = jsonObj["4"]
                                    WeLogger.d(TAG, "realName=$realName")
                                    if (realName == null) {
                                        synchronized(abnormalFriends) {
                                            abnormalFriends += AbnormalFriend(
                                                contact = friend,
                                                // TODO: figure out status, might have to perform another request
                                                status = AbnormalFriendStatus.ThatDeletedThis,
                                            )
                                        }
                                    }
                                    scanningPhase.completed.intValue++
                                }

                                onFailure { errType, errCode, errMsg ->
                                    WeLogger.w(TAG, "failed friend ${friend.wxId}: $errType, $errCode, $errMsg")
                                    scanningPhase.completed.intValue++
                                }
                            }
                            // seems like WeChat's server rate limits requests
                            delay(1.seconds)
                        }

                        if (phase is DialogPhase.Scanning) {
                            phase = DialogPhase.Done(synchronized(abnormalFriends) { abnormalFriends.toList() })
                            dialog.setCancelable(true)
                        }
                    }
                }
            }

            AlertDialogContent(
                title = { Text(text = if (phase is DialogPhase.Idle) "警告" else "检测单向删除好友") },
                text = {
                    when (phase) {
                        is DialogPhase.Idle -> Text(text = "此功能可能导致账号异常, 确定要执行吗?")

                        is DialogPhase.Scanning -> {
                            val completed by (phase as DialogPhase.Scanning).completed
                            val total = (phase as DialogPhase.Scanning).total
                            DefaultColumn {
                                Text("正在扫描, 请稍等...\n已完成: $completed/$total")
                                LinearWavyProgressIndicator(progress = { completed.toFloat() / total })
                            }
                        }

                        is DialogPhase.Done -> {
                            val abnormalFriends = (phase as DialogPhase.Done).friends
                            Text("扫描完成, 有 ${abnormalFriends.size} 个状态异常的好友")
                            LazyColumn {
                                items(abnormalFriends) { friend ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            WeApi.openContact(context, friend.contact.wxId, WeApi.OpenContactDestination.HOMEPAGE)
                                        },
                                        headlineContent = { Text(friend.contact.displayName) },
                                        supportingContent = {
                                            Column {
                                                Text("状态: ${friend.status.displayName}")
                                                Text("昵称: ${friend.contact.nickname}")
                                                Text("备注: ${friend.contact.remarkName}")
                                                Text("微信 ID: ${friend.contact.wxId}")
                                                Text("微信号: ${friend.contact.customWxId}")
                                            }
                                        })
                                }
                            }
                        }
                    }
                },
                dismissButton = when (phase) {
                    is DialogPhase.Idle -> {
                        {
                            TextButton(onDismiss) { Text("取消") }
                        }
                    }

                    is DialogPhase.Scanning -> {
                        {
                            TextButton(onClick = {
                                val scanningPhase = phase as DialogPhase.Scanning
                                // display current snapshot immediately
                                val foundSoFar = synchronized(scanningPhase.abnormalFriends) {
                                    scanningPhase.abnormalFriends.toList()
                                }
                                phase = DialogPhase.Done(foundSoFar)
                                dialog.setCancelable(true)
                            }) { Text("终止") }
                        }
                    }
                    is DialogPhase.Done -> null
                },
                confirmButton = when (phase) {
                    is DialogPhase.Idle -> {
                        {
                            Button(onClick = {
                                phase = DialogPhase.Scanning(mutableIntStateOf(0), friends.size)
                            })
                            { Text("确定") }
                        }
                    }

                    is DialogPhase.Done -> {
                        {
                            Button(onClick = {
                                val abnormalFriends = (phase as DialogPhase.Done).friends
                                val text = abnormalFriends.joinToString("\n\n") { friend ->
                                    buildString {
                                        appendLine("昵称: ${friend.contact.nickname}")
                                        appendLine("备注: ${friend.contact.remarkName}")
                                        appendLine("微信 ID: ${friend.contact.wxId}")
                                        appendLine("微信号: ${friend.contact.customWxId}")
                                        appendLine("状态: ${friend.status.displayName}")
                                    }
                                }
                                copyToClipboard(context, text)
                                showToast(context, "已复制")
                            }) { Text("复制") }
                            Button(onDismiss) { Text("关闭") }
                        }
                    }

                    else -> null
                }
            )
        }
    }
}
