package dev.ujhhgtg.wekit.utils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import dev.ujhhgtg.reflekt.reflected.BaseReflectedMethod
import java.lang.reflect.Executable

typealias HookAction = XC_MethodHook.MethodHookParam.() -> Unit

// most extension methods are inside BaseHookItem for enabled state checking

inline fun BaseReflectedMethod.hookBeforeDirectly(
    priority: Int = 50,
    crossinline action: HookAction
) = self.hookBeforeDirectly(priority, action)

inline fun Executable.hookBeforeDirectly(
    priority: Int = 50,
    crossinline action: HookAction
): XC_MethodHook.Unhook = XposedBridge.hookMethod(
    this, object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) {
            action(param)
        }
    }
)

inline fun BaseReflectedMethod.hookAfterDirectly(
    priority: Int = 50,
    crossinline action: HookAction
): XC_MethodHook.Unhook = self.hookAfterDirectly(priority, action)

inline fun Executable.hookAfterDirectly(
    priority: Int = 50,
    crossinline action: HookAction
): XC_MethodHook.Unhook = XposedBridge.hookMethod(
    this, object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) {
            action(param)
        }
    }
)

@Suppress("NOTHING_TO_INLINE")
inline fun XC_MethodHook.MethodHookParam.invokeOriginal(thisObject: Any? = null, args: Array<Any?>? = null): Any? =
    XposedBridge.invokeOriginalMethod(method, thisObject ?: this.thisObject, args ?: this.args)
