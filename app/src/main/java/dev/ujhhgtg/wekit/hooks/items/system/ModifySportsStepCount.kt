package dev.ujhhgtg.wekit.hooks.items.system

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.ujhhgtg.reflekt.utils.createInstance
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.android.showToast
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "修改运动步数", categories = ["系统与隐私"], description = "修改微信获取到的或手动上传运动步数")
object ModifySportsStepCount : ClickableHookItem(), IResolveDex {

    private val methodGetSteps by dexMethod()
    private val methodUploadSteps by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodGetSteps.find(dexKit) {
            searchPackages("com.tencent.mm.plugin.sport.model")
            matcher {
                usingEqStrings("MicroMsg.Sport.DeviceStepManager", "get today step from %s todayStep %d")
            }
        }

        methodUploadSteps.find(dexKit) {
            searchPackages("com.tencent.mm.plugin.sport.model")
            matcher {
                usingEqStrings("MicroMsg.Sport.DeviceStepManager", "update device Step time: %s stepCount: %s")
            }
        }
    }

    override fun onEnable() {
        methodGetSteps.hookBefore {
            val count = WePrefs.getLongOrDef(KEY_STEP_COUNT, -1L)
            if (count == -1L) return@hookBefore
            result = count
        }
    }

    private const val KEY_STEP_COUNT = "step_count"

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            var stepCount by remember { mutableStateOf("") }

            AlertDialogContent(
                title = { Text(text = "修改运动步数") },
                text = {
                    TextField(
                        value = stepCount,
                        onValueChange = {
                            stepCount = it.filter { c -> c.isDigit() }.trim()
                        },
                        label = { Text("步数") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val count = stepCount.toLongOrNull() ?: run {
                            showToast("格式不正确!")
                            return@Button
                        }
                        WePrefs.putLong(KEY_STEP_COUNT, count)
                        onDismiss()
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onDismiss) {
                        Text("取消")
                    }

                    TextButton(onClick = {
                        val count = stepCount.toLongOrNull() ?: run {
                            showToast("格式不正确!")
                            return@TextButton
                        }
                        val sportsMan = methodUploadSteps.method.declaringClass.createInstance()
                        val result = methodUploadSteps.method.invoke(sportsMan, count) as Boolean
                        showToast("已上传! 返回结果: ${if (result) "成功" else "失败"}")
                    }) {
                        Text("立即上传")
                    }
                }
            )
        }
    }

    override fun onBeforeToggle(newState: Boolean, context: Context): Boolean {
        if (newState) {
            showComposeDialog(context) {
                AlertDialogContent(
                    title = { Text(text = "警告") },
                    text = { Text(text = "此功能可能导致账号异常, 确定要启用吗?") },
                    confirmButton = {
                        Button(onClick = {
                            applyToggle(true)
                            onDismiss()
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onDismiss) {
                            Text("取消")
                        }
                    }
                )
            }
            return false
        }

        return true
    }
}
