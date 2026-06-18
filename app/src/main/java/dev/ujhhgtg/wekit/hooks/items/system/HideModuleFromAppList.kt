package dev.ujhhgtg.wekit.hooks.items.system

import android.app.ApplicationPackageManager
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.WeLogger

@HookItem(name = "隐藏模块应用", categories = ["系统与隐私"], description = "在不影响模块功能的情况下防止微信查询模块安装状态 (实验性)")
object HideModuleFromAppList : SwitchHookItem() {

    private val TAG = This.Class.simpleName

    override fun onEnable() {
        ApplicationPackageManager::class.reflekt().apply {
            firstMethod {
                name = "queryIntentActivities"
            }.hookAfter {
                @Suppress("UNCHECKED_CAST")
                val infos = result as MutableList<ResolveInfo>
                infos.removeAll { info ->
                    (info.activityInfo.packageName == PackageNames.THIS).also {
                        if (it) WeLogger.i(TAG, "removed module from PackageManager::queryIntentActivities")
                    }
                }
            }

            methods {
                name = "getPackageInfo"
                parameters { it[0] == String::class.java }
            }.forEach {
                it.hookBefore {
                    val pkg = args[0] as String
                    if (pkg == PackageNames.THIS) {
                        throwable = PackageManager.NameNotFoundException(pkg)
                        WeLogger.i(TAG, "thrown NameNotFoundException from PackageManager::getPackageInfo")
                    }
                }
            }

            methods {
                name = "getApplicationInfo"
                parameters { it[0] == String::class.java }
            }.forEach {
                it.hookBefore {
                    val pkg = args[0] as String
                    if (pkg == PackageNames.THIS) {
                        throwable = PackageManager.NameNotFoundException(pkg)
                        WeLogger.i(TAG, "thrown NameNotFoundException from PackageManager::getApplicationInfo")
                    }
                }
            }
        }
    }
}
