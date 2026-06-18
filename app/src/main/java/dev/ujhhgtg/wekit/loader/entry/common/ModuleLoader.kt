package dev.ujhhgtg.wekit.loader.entry.common

import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.loader.abc.IHookBridge
import dev.ujhhgtg.wekit.loader.abc.ILoaderService
import dev.ujhhgtg.wekit.loader.startup.UnifiedEntryPoint
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.reflection.ClassLoaderRegistry

object ModuleLoader {

    private val TAG = This.Class.simpleName
    private var isInitialized = false

    @Suppress("unused")
    @JvmStatic
    fun init(
        hostDataDir: String,
        hostClassLoader: ClassLoader,
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        modulePath: String,
        allowDynamicLoad: Boolean
    ) {
        if (isInitialized) return
        isInitialized = true

        ClassLoaderRegistry.hookConstructors()

        WeLogger.i(TAG, "loading in entry point ${loaderService.entryPointName}")
        runCatching {
            UnifiedEntryPoint.entry(loaderService, hookBridge, hostClassLoader, modulePath)
        }.onFailure { WeLogger.e(TAG, "UnifiedEntryPoint failed", it) }
    }
}
