package dev.ujhhgtg.wekit.features.items.debug

import android.content.ContentValues
import androidx.activity.ComponentActivity
import dev.ujhhgtg.wekit.features.api.core.WeDatabaseListenerApi
import dev.ujhhgtg.wekit.features.api.core.WeMessageApi
import dev.ujhhgtg.wekit.features.api.core.models.MessageInfo
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.Feature
import dev.ujhhgtg.wekit.utils.WeLogger

@Feature(name = "测试", categories = ["调试"], description = "???")
object Experiments : ClickableFeature() {

    @Suppress("unused")
    private const val TAG = "Experiments"

    override fun onClick(context: ComponentActivity) {
        WeDatabaseListenerApi.addListener(object : WeDatabaseListenerApi.IInsertListener {
            override fun onInsert(table: String, values: ContentValues) {
                if (table == "message") {
                    val msgInfo = MessageInfo(WeMessageApi.convertMsgInfoInstanceFromContentValues(values))
                    if (msgInfo.toPatMessage() != null) {
                        WeLogger.d(TAG, msgInfo.content)
                    }
                }
            }
        })
    }
}
