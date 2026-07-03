package dev.ujhhgtg.wekit.features.items.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.DexClassDelegate
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.features.api.core.WeConversationApi
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.Feature
import dev.ujhhgtg.wekit.preferences.WePrefs.Companion.prefOption
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.DefaultColumn
import dev.ujhhgtg.wekit.ui.utils.dpToPx
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.findActivity
import dev.ujhhgtg.wekit.utils.android.getTopMostActivity
import dev.ujhhgtg.wekit.utils.android.runOnUiThread
import dev.ujhhgtg.wekit.utils.android.showToast
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs

@Feature(
    name = "左划删除对话",
    categories = ["聊天"],
    description = "在主页对话列表的对话上左划以删除对话\n点击可配置删除方式: 「删除该聊天」(彻底删除, 弹确认框) 或「不显示该聊天」(仅从列表隐藏, 重新收到消息会再出现)"
)
object SwipeToDeleteConversation : ClickableFeature(), IResolveDex {

    // false (default) = 不显示该聊天 (WeConversationApi.deleteConversation, silent + reappears on new
    //   message); true = 删除该聊天 (permanent delete). Breaking change: renamed from the old
    //   swipe_delete_hide_only pref with inverted meaning; existing configs are intentionally reset.
    private var deleteInsteadOfHide by prefOption("swipe_delete_delete_instead_of_hide", false)

    // Only meaningful when deleteInsteadOfHide == true. When true, skip WeChat's confirm dialog and
    // delete the conversation immediately via s1.d (doDeleteConv). Off by default (delete is
    // irreversible).
    private var skipConfirm by prefOption("swipe_delete_skip_confirm", false)

    // Mutable per-row gesture state. The list recycles row views, so the talker / conversation
    // object are refreshed on every getView bind (see hookAdapterBind) rather than captured once.
    private class SwipeState(
        val touchSlop: Int,
        val triggerThreshold: Float,
        var talker: String? = null,
        var conversation: Any? = null,
        var startX: Float = 0f,
        var startY: Float = 0f,
        var isDragging: Boolean = false,
        var triggered: Boolean = false,
    )

    // WeakHashMap: entries are removed automatically once the recycled row view is GC'd.
    private val states: MutableMap<View, SwipeState> =
        Collections.synchronizedMap(WeakHashMap())

    private val springInterpolator = OvershootInterpolator(1.3f)

    private val TAG = This.Class.simpleName

    // WeChat has TWO home conversation-list adapters and picks one at runtime in MainUI.onCreate
    // (o75.s.f347101a.b()): the legacy ListView adapter com.tencent.mm.ui.conversation.p3
    // (ConversationWithCacheAdapter) and the newer MVVM adapter o75.v0
    // (ConversationAdapter.MvvmConversationAdapter). Both expose getView(int,View,ViewGroup) that
    // returns the clickable row root and getItem(position) -> com.tencent.mm.storage.m3, and both
    // re-install their own row OnTouchListener on every bind — so we hook getView on whichever is
    // present. allowFailure so a build that only ships one of them still resolves the other.
    private val classConversationAdapter by dexClass(allowFailure = true) {
        searchPackages("com.tencent.mm.ui.conversation")
        matcher {
            usingStrings(
                "MicroMsg.ConversationWithCacheAdapter",
                "[getView] position="
            )
        }
    }

    private val classMvvmConversationAdapter by dexClass(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.ConversationAdapter.MvvmConversationAdapter")
        }
    }

    // com.tencent.mm.ui.conversation.s1#c(talker, context, m3, boolean, Runnable, Runnable,
    // boolean, boolean): WeChat's own "删除该聊天" helper. It shows the native confirmation dialog
    // and, on confirm, deletes the conversation through the cache-aware storage and refreshes the
    // list — exactly what the long-press "删除" menu item does. The no-dialog variant lives in
    // WeConversationApi.deleteConversationPermanently.
    private val methodDeleteConversation by dexMethod {
        searchPackages("com.tencent.mm.ui.conversation")
        matcher {
            usingStrings("del all qmessage", "del all tmessage", "del all notify message")
            paramCount = 8
            returnType(Void.TYPE)
        }
    }

    override fun onEnable() {
        hookAdapter(classConversationAdapter)
        hookAdapter(classMvvmConversationAdapter)
    }

    override fun onDisable() {
        states.clear()
    }

    // ── row binding: attach the swipe listener + keep talker / conversation fresh ─

    private fun hookAdapter(adapter: DexClassDelegate) {
        if (adapter.isPlaceholder) return
        adapter.reflekt()
            .firstMethod { name = "getView"; parameterCount = 3 }
            .hookAfter {
                val view = result as? View ?: return@hookAfter
                val position = args[0] as? Int ?: return@hookAfter

                // getItem(position) -> com.tencent.mm.storage.m3 (rconversation model).
                val conversation = runCatching {
                    thisObject.reflekt()
                        .firstMethod { name = "getItem"; parameterCount = 1 }
                        .invoke(position)
                }.getOrNull() ?: return@hookAfter

                val talker = runCatching {
                    conversation.reflekt()
                        .firstFieldOrNull { name = "field_username"; superclass() }
                        ?.get() as? String
                }.getOrNull()

                val state = states.getOrPut(view) {
                    val ctx = view.context
                    SwipeState(
                        touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop,
                        triggerThreshold = 80.dpToPx(ctx).toFloat(),
                    )
                }
                state.talker = talker
                state.conversation = conversation

                // p3.getView (re)installs WeChat's own OnTouchListener (v3, for the ripple hotspot)
                // on the row root on EVERY bind — see p3.java:891, which runs before this hookAfter.
                // If we only set ours once, that call clobbers it on the next recycle and the swipe
                // silently stops working. So we re-install our wrapper every bind, delegating to
                // whatever listener is currently attached (unless it is already ours).
                attachSwipeListener(view, state)
            }
    }

    // Marks our wrapper so a re-bind can tell its own listener apart from WeChat's v3.
    private class SwipeTouchListener(
        val state: SwipeState,
        val delegate: View.OnTouchListener?,
    ) : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val consumed = handleSwipe(v, state, event)
            // Always let WeChat's listener observe the event too (it only sets a ripple hotspot and
            // returns false), but our return value decides whether the row's click path proceeds.
            runCatching { delegate?.onTouch(v, event) }
            return consumed
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachSwipeListener(view: View, state: SwipeState) {
        val current = getAttachedTouchListener(view)
        if (current is SwipeTouchListener) return  // already wrapped for this stream
        view.setOnTouchListener(SwipeTouchListener(state, current))
    }

    // Reads the View's current OnTouchListener out of its ListenerInfo, so we can chain to WeChat's.
    private fun getAttachedTouchListener(view: View): View.OnTouchListener? = runCatching {
        val info = view.reflekt()
            .firstFieldOrNull { name = "mListenerInfo"; superclass() }
            ?.get() ?: return null
        info.reflekt()
            .firstFieldOrNull { name = "mOnTouchListener" }
            ?.get() as? View.OnTouchListener
    }.getOrNull()

    // ── gesture ──────────────────────────────────────────────────────────────
    //
    // The conversation row is clickable, so it consumes ACTION_DOWN in its own onTouchEvent and no
    // child touch-target is created — meaning the row's onInterceptTouchEvent is never called for
    // subsequent MOVE events. On top of that, the home screen's horizontal ViewPager intercepts the
    // horizontal drag before the row would ever see it. So an OnTouchListener (which runs ahead of
    // both onTouchEvent and the click) is the only reliable place to catch the swipe: we detect the
    // horizontal drag at scaledTouchSlop (smaller than the pager's paging slop) and immediately call
    // requestDisallowInterceptTouchEvent(true) so the pager can't steal the gesture. This mirrors
    // how WeChat's own MMSlideDelView drives its slide entirely from onTouchEvent.
    private fun handleSwipe(v: View, s: SwipeState, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                s.startX = event.rawX
                s.startY = event.rawY
                s.isDragging = false
                s.triggered = false
                // Return false so the row still receives the click / long-press on this stream.
                false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - s.startX
                val dy = event.rawY - s.startY
                if (!s.isDragging && dx < 0 && abs(dx) > s.touchSlop && abs(dx) > abs(dy)) {
                    s.isDragging = true
                    // Win the gesture from the ViewPager / ListView before they cross their slop.
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    // Cancel the pending click / pressed state on the row.
                    v.isPressed = false
                    // The row scheduled a long-press callback on ACTION_DOWN (its onTouchEvent saw
                    // the DOWN because we returned false then). Once we start consuming MOVE events,
                    // they no longer reach onTouchEvent, so its own movement-cancel never runs and the
                    // long-press menu would still pop mid-swipe. Remove that queued callback here.
                    v.cancelLongPress()
                }
                if (s.isDragging) {
                    v.translationX = dx.coerceIn(-s.triggerThreshold, 0f)
                    // Haptic tick tracks the live fireable state: buzz when crossing INTO the fire
                    // zone, and re-arm when sliding back out so a re-cross buzzes again.
                    val past = dx <= -s.triggerThreshold
                    if (past && !s.triggered) {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    s.triggered = past
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - s.startX
                // Decide by the FINAL position, not whether the threshold was ever crossed: sliding
                // past the threshold and then back is an intentional cancel, so it must NOT fire.
                val fire = s.isDragging && dx <= -s.triggerThreshold
                if (s.isDragging) {
                    v.animate()
                        .translationX(0f)
                        .setDuration(250)
                        .setInterpolator(springInterpolator)
                        .start()
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    s.isDragging = false
                    if (fire) onSwipeLeft(v, s)
                    // Consume so the row's click doesn't fire after a swipe.
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (s.isDragging) {
                    v.animate().translationX(0f).setDuration(150).start()
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    s.isDragging = false
                }
                false
            }

            else -> false
        }
    }

    // ── config page (删除而非隐藏 + 跳过确认) ──────────────────────────────────

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            var deleteInsteadOfHideInput by remember { mutableStateOf(deleteInsteadOfHide) }
            var skipConfirmInput by remember { mutableStateOf(skipConfirm) }

            AlertDialogContent(
                title = { Text("左划删除对话") },
                text = {
                    DefaultColumn {
                        ListItem(
                            headlineContent = { Text("删除而非隐藏") },
                            supportingContent = {
                                Text(
                                    if (deleteInsteadOfHideInput)
                                        "左划调用微信原生「删除该聊天」, 彻底删除对话"
                                    else
                                        "左划仅将对话从列表隐藏, 保留聊天记录, 重新收到消息时会再次出现 (无需确认)"
                                )
                            },
                            trailingContent = {
                                Switch(checked = deleteInsteadOfHideInput, onCheckedChange = null)
                            },
                            modifier = Modifier.clickable {
                                deleteInsteadOfHideInput = !deleteInsteadOfHideInput
                                deleteInsteadOfHide = deleteInsteadOfHideInput
                            }
                        )

                        if (deleteInsteadOfHideInput) {
                            ListItem(
                                headlineContent = { Text("跳过删除确认对话框") },
                                supportingContent = {
                                    Text(
                                        if (skipConfirmInput)
                                            "左划到底松手直接删除, 不再弹出确认对话框 (删除不可恢复, 请谨慎)"
                                        else
                                            "删除前仍弹出微信原生确认对话框"
                                    )
                                },
                                trailingContent = {
                                    Switch(checked = skipConfirmInput, onCheckedChange = null)
                                },
                                modifier = Modifier.clickable {
                                    skipConfirmInput = !skipConfirmInput
                                    skipConfirm = skipConfirmInput
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    // ── delete / hide on swipe ─────────────────────────────────────────────────

    private fun onSwipeLeft(view: View, state: SwipeState) {
        val talker = state.talker
        val conversation = state.conversation
        if (talker.isNullOrBlank() || conversation == null) return

        if (!deleteInsteadOfHide) {
            // 不显示该聊天: same as WeChat's native "不显示该聊天" (ConversationStorage.delChatContact).
            // It notifies list observers synchronously, so it must run on the main thread.
            runOnUiThread {
                WeConversationApi.hideConversation(talker)
                showToast("已隐藏")
            }
            return
        }

        // 删除该聊天: WeChat's own delete helpers.
        if (skipConfirm) {
            // No dialog: permanent delete via the shared API (s1.d / doDeleteConv). Notifies list
            // observers synchronously, so it must run on the main thread.
            runOnUiThread {
                WeConversationApi.deleteConversation(talker, conversation)
                showToast("已删除")
            }
            return
        }

        // Confirm-then-delete: WeChat's own dialog helper.
        val activity = view.context.findActivity() ?: getTopMostActivity() ?: return
        try {
            // s1.c(talker, context, m3, showTip=true, onDone=null, onCancel=null, true, false):
            // shows WeChat's native confirm dialog before wiping.
            methodDeleteConversation.method.invoke(
                null,
                talker,
                activity,
                conversation,
                true,
                null,
                null,
                true,
                false
            )
        } catch (ex: Exception) {
            WeLogger.e(TAG, "failed to delete conversation $talker", ex)
        }
    }
}
