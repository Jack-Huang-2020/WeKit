package dev.ujhhgtg.wekit.hooks.items.miniapps

import android.app.Activity
import com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "移除开屏广告", categories = ["小程序"], description = "跳过小程序开屏广告")
object RemoveSplashAds : SwitchHookItem(), IResolveDex {

    private val methodIsAdContact by dexMethod()
    private val methodAdDataCallback by dexMethod()
    private val methodCheckCanShowAd by dexMethod()

    override fun onEnable() {
        methodIsAdContact.hookBefore {
            result = false
        }

        methodAdDataCallback.hookBefore {
            result = null
        }

        methodCheckCanShowAd.hookBefore {
            result = false
        }

        AppBrandAdUI::class.java.hookBeforeOnCreate {
            val activity = thisObject as Activity
            activity.finish()
            result = null
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodIsAdContact.find(dexKit) {
            matcher {
                usingEqStrings("MicroMsg.AppBrandAdUtils[AppBrandSplashAd]", "isAdContact, appId:%s, canShowAd:%s")
            }
        }

        methodAdDataCallback.find(dexKit) {
            searchPackages("com.tencent.mm.plugin.appbrand.jsapi.auth")
            matcher {
                usingEqStrings(
                    "MicroMsg.AppBrand.JsApiAdOperateWXData[AppBrandSplashAd]", "cgi callback, callbackId:%s, service not running or preloaded"
                )
            }
        }

        methodCheckCanShowAd.find(dexKit) {
            searchPackages("com.tencent.mm.plugin.appbrand")
            matcher {
                usingEqStrings("MicroMsg.AppBrandAdUtils[AppBrandSplashAd]", "checkCanShowAd, show ad (splash ad debug mode open)")
            }
        }
    }
}
