package dev.ujhhgtg.wekit.hooks.api.core

import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.WeLogger
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

@HookItem(name = "AppMsg 发送服务", categories = ["API"], description = "提供 XML 卡片消息发送能力")
object WeAppMsgApi : ApiHookItem(), IResolvesDex {

    private val methodParseXml by dexMethod()    // op0.q.u(String)
    private val methodSendAppMsg by dexMethod()  // k0.J(...)

    private val TAG = This.Class.simpleName

    override fun resolveDex(dexKit: DexKitBridge) {
        val classAppMsgContent = dexKit.findClass {
            matcher {
                usingStrings("<appmsg appid=\"", "parse amessage xml failed")
            }
        }.single()

        val classAppMsgLogic = dexKit.findClass {
            matcher {
                usingStrings("MicroMsg.AppMsgLogic", "summerbig sendAppMsg attachFilePath")
            }
        }.single()

        methodParseXml.find(dexKit, true) {
            matcher {
                declaredClass = classAppMsgContent.name
                modifiers = Modifier.PUBLIC or Modifier.STATIC
                paramTypes(String::class.java.name)
                returnType = classAppMsgContent.name
                usingStrings("parse msg failed")
            }
        }

        methodSendAppMsg.find(dexKit) {
            matcher {
                declaredClass = classAppMsgLogic.name
                modifiers = Modifier.STATIC
                paramCount = 6
                paramTypes(
                    classAppMsgContent.name,
                    "java.lang.String",
                    null,
                    null,
                    null,
                    null
                )
            }
        }
    }

    fun sendXmlAppMsg(
        toUser: String,
        title: String,
        appId: String,
        url: String?,
        data: ByteArray?,
        xmlContent: String
    ): Boolean {
        return try {
            WeLogger.i(TAG, "sending appmsg to $toUser")
            val contentObj = methodParseXml.method.invoke(null, xmlContent)
            if (contentObj == null) {
                WeLogger.e(TAG, "failed to parse xml")
                return false
            }

            methodSendAppMsg.method.invoke(
                null,         // static
                contentObj, // content
                appId,             // appId
                title,             // title/appName
                toUser,            // toUser
                url,               // url
                data               // thumbDat
            )

            true
        } catch (e: Throwable) {
            WeLogger.e(TAG, "failed to send appmsg", e)
            false
        }
    }
}
