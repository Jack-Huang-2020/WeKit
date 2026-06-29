package dev.ujhhgtg.wekit.features.api.core

import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.utils.createInstance
import dev.ujhhgtg.reflekt.utils.makeAccessible
import dev.ujhhgtg.reflekt.utils.toClass
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.features.api.net.WeNetSceneApi
import dev.ujhhgtg.wekit.features.core.ApiFeature
import dev.ujhhgtg.wekit.features.core.Feature
import dev.ujhhgtg.wekit.utils.WeLogger
import java.util.LinkedList

@Feature(name = "联系人标签服务", categories = ["API"], description = "提供联系人标签查询与修改能力")
object WeContactLabelApi : ApiFeature(), IResolveDex {

    private val TAG = This.Class.simpleName

    data class ContactLabel(val labelId: Int, val labelName: String)

    // MANY protobuf classes (~250) follow this pattern, and we have no more conditions
//    // mx4.so6
//    private val classContactLabelPb by dexClass {
//        matcher {
//            fields {
//                count(2)
//                add { type = "java.lang.String" }
//                add { type = "java.lang.String" }
//            }
//            methods {
//                add {
//                    name = "compareContent"
//                    // z01.f
//                    addInvoke {
//                        declaredClass {
//                            modifiers(AccessFlagsMatcher(Modifier.ABSTRACT))
//                            usingEqStrings("obj", "key")
//                        }
//                        modifiers(AccessFlagsMatcher(Modifier.STATIC or Modifier.FINAL))
//                        returnType = "boolean"
//                        paramTypes(Any::class.java, Any::class.java)
//                    }
//                }
//                add {
//                    name = "op"
//                }
//            }
//        }
//    }

    private val classNetSceneModifyContactLabelList by dexClass {
        matcher {
            usingEqStrings("/cgi-bin/micromsg-bin/modifycontactlabellist")
            addMethod {
                name = "<init>"
                paramTypes(LinkedList::class.java)
            }
        }
    }

    /**
     * Get all contact labels.
     * @return List of ContactLabel sorted alphabetically by name
     */
    fun getAllLabels(): List<ContactLabel> {
        return try {
            val cursor = WeDatabaseApi.rawQuery(
                "SELECT labelID, labelName FROM ContactLabel ORDER BY labelName"
            )
            val labels = mutableListOf<ContactLabel>()
            cursor.use {
                while (it.moveToNext()) {
                    labels.add(
                        ContactLabel(
                            it.getInt(it.getColumnIndexOrThrow("labelID")),
                            it.getString(it.getColumnIndexOrThrow("labelName"))
                        )
                    )
                }
            }
            labels
        } catch (e: Exception) {
            WeLogger.e(TAG, "getAllLabels failed", e)
            emptyList()
        }
    }

    /**
     * Get contacts belonging to a specific label, by label ID.
     */
    fun getContactsByLabelId(labelId: Int): List<String> {
        return try {
            val raw = WeDatabaseApi.rawQuery(
                "SELECT username FROM rcontact WHERE ',' || contactLabelIds || ',' LIKE ?",
                arrayOf("%,$labelId,%")
            )
            val wxids = mutableListOf<String>()
            raw.use {
                while (it.moveToNext()) {
                    wxids.add(it.getString(0))
                }
            }
            wxids
        } catch (e: Exception) {
            WeLogger.e(TAG, "getContactsByLabelId failed", e)
            emptyList()
        }
    }

    /**
     * Get contacts belonging to a specific label, by label name.
     * First resolves the label name to label ID, then queries contacts.
     */
    fun getContactsByLabelName(labelName: String): List<String> {
        return try {
            val labelId = getLabelIdByName(labelName) ?: return emptyList()
            getContactsByLabelId(labelId)
        } catch (e: Exception) {
            WeLogger.e(TAG, "getContactsByLabelName failed", e)
            emptyList()
        }
    }

    private val contactLabelPbClass by lazy { "mx4.so6".toClass() }

    /**
     * Modify contact labels.
     * @param username Target contact username
     * @param labelNames List of label names to associate with the contact
     */
    fun modifyLabel(username: String, labelNames: List<String>) {
        try {
            WeLogger.i(TAG, "modifyLabel: username=$username, labels=$labelNames")

            val labelIds = mutableListOf<String>()
            for (name in labelNames) {
                val id = getLabelIdByName(name)
                if (id != null) {
                    labelIds.add(id.toString())
                } else {
                    WeLogger.w(TAG, "modifyLabel: label '$name' not found in database, skipping")
                }
            }

            val pbInstance = contactLabelPbClass.createInstance()

            val stringFields = contactLabelPbClass.declaredFields.filter { it.type == String::class.java }
            if (stringFields.size < 2) {
                throw IllegalStateException("ContactLabelPb does not have at least 2 String fields")
            }

            val fieldUsername = stringFields[0].makeAccessible()
            val fieldLabelIds = stringFields[1].makeAccessible()

            fieldUsername.set(pbInstance, username)

            val joinedIds = labelIds.joinToString(",")
            fieldLabelIds.set(pbInstance, joinedIds)

            val linkedList = LinkedList<Any>()
            linkedList.push(pbInstance)

            val netSceneInstance = classNetSceneModifyContactLabelList.clazz.createInstance(linkedList)

            WeNetSceneApi.sendNetScene(netSceneInstance)
            WeLogger.i(TAG, "modifyLabel netscene dispatched successfully")
        } catch (e: Exception) {
            WeLogger.e(TAG, "modifyLabel failed", e)
        }
    }

    /**
     * Get a label ID from its name.
     */
    private fun getLabelIdByName(labelName: String): Int? {
        val cursor = WeDatabaseApi.rawQuery(
            "SELECT labelID FROM ContactLabel WHERE labelName = ?",
            arrayOf(labelName)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return null
    }
}
