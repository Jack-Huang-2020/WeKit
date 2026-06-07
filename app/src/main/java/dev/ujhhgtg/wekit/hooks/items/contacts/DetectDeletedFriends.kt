package dev.ujhhgtg.wekit.hooks.items.contacts

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.hooks.api.core.WeApi
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
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

    override val noSwitchWidget: Boolean
        get() = true

    private val TAG = This.Class.simpleName

    private enum class AbnormalFriendStatus(val displayName: String) {
        ThatAccountBanned("对方账号异常"),
        ThatBlockedThis("被拉黑"),
        ThatDeletedThis("被删除")
    }

    private data class AbnormalFriend(
        val nickname: String,
        val remarkName: String,
        val wxId: String,
        val customWxId: String,
        val status: AbnormalFriendStatus
    )

    private sealed class DialogPhase {
        data object Idle : DialogPhase()
        data class Scanning(var completed: MutableIntState, val total: Int) : DialogPhase()
        data class Done(val friends: List<AbnormalFriend>) : DialogPhase()
    }

    override fun onClick(context: Context) {
        val phaseState = mutableStateOf<DialogPhase>(DialogPhase.Idle)

        val friends = WeDatabaseApi.getFriends().filter { c ->
            c.type != 2051 && c.type != 2049 && c.wxId != WeApi.selfWxId
        }

        showComposeDialog(context) {
            var phase by phaseState

            LaunchedEffect(phase) {
                if (phase is DialogPhase.Scanning) {
                    dialog.setCancelable(false)
                    CoroutineScope(Dispatchers.IO).launch {
                        val abnormalFriends = mutableListOf<AbnormalFriend>()
                        for (friend in friends) {
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
                                        abnormalFriends += AbnormalFriend(
                                            nickname = friend.nickname,
                                            remarkName = friend.remarkName.ifBlank { "<无备注>" },
                                            wxId = friend.wxId,
                                            customWxId = friend.customWxId,
                                            // TODO: figure out status, might have to perform another request
                                            status = AbnormalFriendStatus.ThatDeletedThis,
                                        )
                                    }
                                    (phase as DialogPhase.Scanning).completed.intValue++
                                }

                                onFailure { errType, errCode, errMsg ->
                                    WeLogger.w(TAG, "failed friend ${friend.wxId}: $errType, $errCode, $errMsg")
                                    (phase as DialogPhase.Scanning).completed.intValue++
                                }
                            }
                            // seems like WeChat's server rate limits requests
                            delay(1.seconds)
                        }
                        phase = DialogPhase.Done(abnormalFriends)
                        dialog.setCancelable(true)
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
                                            WeApi.openContact(context, friend.wxId, WeApi.OpenContactDestination.HOMEPAGE)
                                        },
                                        headlineContent = { Text("${friend.nickname} (${friend.remarkName})") },
                                        supportingContent = {
                                            Column {
                                                Text("状态: ${friend.status.displayName}")
                                                Text("微信 ID: ${friend.wxId}")
                                                Text("微信号: ${friend.customWxId}")
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

                    is DialogPhase.Scanning -> null
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
                            Row {
                                Button(onClick = {
                                    val abnormalFriends = (phase as DialogPhase.Done).friends
                                    val text = abnormalFriends.joinToString("\n\n") { friend ->
                                        buildString {
                                            appendLine("昵称: ${friend.nickname}")
                                            appendLine("备注: ${friend.remarkName}")
                                            appendLine("微信 ID: ${friend.wxId}")
                                            appendLine("微信号: ${friend.customWxId}")
                                            appendLine("状态: ${friend.status.displayName}")
                                        }
                                    }
                                    copyToClipboard(context, text)
                                    showToast(context, "已复制")
                                }) { Text("复制") }
                                Button(onDismiss) { Text("关闭") }
                            }
                        }
                    }

                    else -> null
                }
            )
        }
    }
}
