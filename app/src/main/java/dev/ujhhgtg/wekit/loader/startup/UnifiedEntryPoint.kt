package dev.ujhhgtg.wekit.loader.startup

import android.app.Instrumentation
import com.tencent.mm.app.Application
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.ReflectionClassLoader
import dev.ujhhgtg.wekit.loader.abc.IHookBridge
import dev.ujhhgtg.wekit.loader.abc.ILoaderService
import dev.ujhhgtg.wekit.loader.utils.HybridClassLoader
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.hookAfterDirectly
import dev.ujhhgtg.wekit.utils.reflection.ClassLoaders

object UnifiedEntryPoint {

    private val TAG = This.Class.simpleName

    fun entry(
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        hostClassLoader: ClassLoader,
        modulePath: String
    ) {
        ReflectionClassLoader.value = hostClassLoader
        val self = ClassLoaders.MODULE
        val selfParent = self.parent
        HybridClassLoader.moduleParentClassLoader = selfParent
        HybridClassLoader.hostClassLoader = hostClassLoader
        self.reflekt()
            .firstField { name = "parent"; superclass() }
            .set(HybridClassLoader)

        Application::class.reflekt()
            .firstMethod { name = "attachBaseContext" }
            .hookAfterDirectly {
                Instrumentation::class.reflekt()
                    .firstMethod {
                        name = "callApplicationOnCreate"
                    }
                    .hookAfterDirectly {
                        runCatching {
                            StartupAgent.startup(
                                loaderService,
                                hookBridge,
                                modulePath
                            )
                        }.onFailure { WeLogger.e(TAG, "StartupAgent failed", it) }
                    }
            }
    }
}
