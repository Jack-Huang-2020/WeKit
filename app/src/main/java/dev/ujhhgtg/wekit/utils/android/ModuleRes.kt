package dev.ujhhgtg.wekit.utils.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.utils.WeLogger

@SuppressLint("StaticFieldLeak")
object ModuleRes {

    private const val TAG = "ModuleRes"

    lateinit var context: Context
    lateinit var resources: Resources

    @SuppressLint("DiscouragedApi")
    fun init(hostContext: Context) {
        if (::context.isInitialized) return

        runCatching {
            context = hostContext.createPackageContext(
                PackageNames.MODULE,
                Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
            )
            resources = context.resources
        }.onFailure { WeLogger.e(TAG, "failed to initialize module resources", it) }
    }
}
