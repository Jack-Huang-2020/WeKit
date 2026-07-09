@file:Suppress("DEPRECATION")

package dev.ujhhgtg.wekit.ui.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import dev.ujhhgtg.wekit.utils.android.ModuleRes
import dev.ujhhgtg.wekit.utils.reflection.ClassLoaders

class CommonContextWrapper(base: Context) : ContextWrapper(base) {

    private val _theme = ModuleRes.resources.newTheme()

    override fun getClassLoader(): ClassLoader {
        return ClassLoaders.MODULE
    }

    override fun getResources(): Resources {
        return ModuleRes.resources
    }

    override fun getAssets(): AssetManager? {
        return ModuleRes.resources.assets
    }

    override fun getTheme(): Resources.Theme {
        return _theme
    }

    override fun setTheme(resId: Int) {
        _theme.applyStyle(resId, true)
    }
}
