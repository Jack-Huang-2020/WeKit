package dev.ujhhgtg.wekit.hooks.api.core

import com.highcapable.kavaref.extension.createInstance
import dev.ujhhgtg.comptime.nameOf
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.reflection.asResolver
import org.luckypray.dexkit.DexKitBridge

@HookItem(name = "对话服务", categories = ["API"], description = "提供对话管理能力")
object WeConversationApi : ApiHookItem(), IResolvesDex {

    private val TAG = nameOf(WeConversationApi)
    private val classConversationStorage by dexClass()
    private val methodUpdateUnreadByTalker by dexMethod()
    private val methodHiddenConvParent by dexMethod()
    private val methodGetConvByName by dexMethod()
    private val methodChatroomStorageGetMemberCount by dexMethod()
    private val classChatroomMember by dexClass()
    private val methodSetDnd by dexMethod()
    private val methodSetNoDnd by dexMethod()

    val conversationStorage by lazy {
        WeServiceApi.storageFeatureService.asResolver()
            .firstMethod {
                returnType = classConversationStorage.clazz
            }
            .invoke()!!
    }

    val chatroomStorage by lazy {
        WeServiceApi.chatroomService.asResolver()
            .firstMethod {
                returnType = methodChatroomStorageGetMemberCount.method.declaringClass
            }
            .invoke()!!
    }

    // this is NOT group 'member'
    // this is the group itself
    fun getGroup(groupId: String): Any {
        return chatroomStorage.asResolver()
            .firstMethod {
                parameters(String::class)
                returnType = classChatroomMember.clazz
            }
            .invoke(groupId)!!
    }

    fun markAllAsRead() {
        val cursor = WeDatabaseApi.rawQuery("SELECT username FROM rconversation WHERE unReadCount>0 OR unReadMuteCount>0")
        while (cursor.moveToNext()) {
            val talker = cursor.getString(0)
            try {
                methodUpdateUnreadByTalker.method.invoke(conversationStorage, talker)
                WeLogger.d(TAG, "marked $talker as read")
            } catch (ex: Exception) {
                WeLogger.w(TAG, "exception while updating unread count for $talker", ex)
            }
        }
        cursor.close()
    }

    fun markAsRead(talker: String) {
        try {
            methodUpdateUnreadByTalker.method.invoke(conversationStorage, talker)
            WeLogger.d(TAG, "marked $talker as read")
        } catch (ex: Exception) {
            WeLogger.w(TAG, "exception while updating unread count for $talker", ex)
        }
    }

    fun setConversationsVisibility(visible: Boolean, talkers: Array<String>) {
        val operation = if (visible) "" else "hidden_conv_parent"
        if (methodHiddenConvParent.method.parameterCount == 4) {
            methodHiddenConvParent.method.invoke(
                conversationStorage,
                talkers,
                operation,
                true,
                false
            )
        } else {
            methodHiddenConvParent.method.invoke(
                conversationStorage,
                talkers,
                operation
            )
        }
    }

    fun setAllConversationVisibility(visible: Boolean) {
        val cursor = WeDatabaseApi.rawQuery("SELECT username FROM rconversation")
        val talkers = mutableListOf<String>()
        while (cursor.moveToNext()) {
            talkers += cursor.getString(0)
        }
        cursor.close()
        setConversationsVisibility(visible, talkers.toTypedArray())
    }

    fun onlyShowFilteredConversations(queryFilter: String, selectedColumns: String = "username") {
        setAllConversationVisibility(false)
        setFilteredConversationsVisibility(true, queryFilter, selectedColumns)
    }

    fun setFilteredConversationsVisibility(visible: Boolean, queryFilter: String, selectedColumns: String = "username") {
        val cursor = WeDatabaseApi.rawQuery("SELECT $selectedColumns FROM rconversation $queryFilter")
        val talkers = mutableListOf<String>()
        while (cursor.moveToNext()) {
            talkers += cursor.getString(0)
        }
        cursor.close()
        setConversationsVisibility(visible, talkers.toTypedArray())
    }

    private lateinit var contactType: Class<*>

    // TODO: this only updates local DB without syncing to server
    fun setIfNotifyNewMessages(convId: String, shouldNotify: Boolean) {
        if (!::contactType.isInitialized) {
            contactType = methodSetDnd.method.parameterTypes[0]
        }

        val contact = contactType.createInstance(convId)
        if (!shouldNotify) {
            methodSetDnd.method.invoke(null, contact, true)
        } else {
            methodSetNoDnd.method.invoke(null, contact, true)
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        classConversationStorage.find(dexKit) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                usingEqStrings("rconversation", "PRAGMA table_info( rconversation)")
            }
        }

        methodUpdateUnreadByTalker.find(dexKit) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("MicroMsg.ConversationStorage", "updateUnreadByTalker %s")
            }
        }

        methodHiddenConvParent.find(dexKit) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("Update rconversation set parentRef = '", "' where 1 != 1 ")
            }
        }

        methodGetConvByName.find(dexKit) {
            matcher {
                declaredClass(classConversationStorage.clazz)
                usingEqStrings("MicroMsg.ConversationStorage", "get null with username:")
            }
        }

        methodChatroomStorageGetMemberCount.find(dexKit) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                usingEqStrings("MicroMsg.ChatroomStorage", "[getMemberCount] cost:%sms")
            }
        }

        classChatroomMember.find(dexKit) {
            searchPackages("com.tencent.mm.storage")
            matcher {
                usingEqStrings("MicroMsg.ChatRoomMember", "service is null")
            }
        }

        methodSetDnd.find(dexKit) {
            matcher {
                usingEqStrings("MicroMsg.OpenImOpLogLogic", "OpenImOpLogLogic OpenIMModContactMuteOplog username:%s switch add")
            }
        }

        methodSetNoDnd.find(dexKit) {
            matcher {
                usingEqStrings("MicroMsg.OpenImOpLogLogic", "OpenImOpLogLogic OpenIMModContactMuteOplog username:%s switch cancel")
            }
        }
    }
}
