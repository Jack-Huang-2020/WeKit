package dev.ujhhgtg.wekit.features.api.net

import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderEntity
import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderType
import dev.ujhhgtg.wekit.agent.model.ModelProviderManager
import dev.ujhhgtg.wekit.constants.Preferences
import dev.ujhhgtg.wekit.preferences.WePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent, in-memory-mirrored account state for WeKit Pro.
 *
 * **Persistence**: all fields are backed by [WePrefs] (MMKV). Values are loaded eagerly into a
 * [StateFlow] so composables can react to account changes without polling.
 *
 * **Thread safety**: writes are serialised via `@Synchronized`; the backing [MutableStateFlow]
 * is updated atomically after every write.
 *
 * **Local status is cosmetic only.** The server always verifies the [userToken] independently.
 * Local [tier] / [periodEnd] are only used for displaying the badge in the profile card.
 */
object WekitProAccount {

    // ---------------------------------------------------------------------------
    //  State snapshot
    // ---------------------------------------------------------------------------

    data class State(
        val userToken: String,
        val tier: WekitProTier,
        /** Period-end epoch millis; 0 = no active subscription. */
        val periodEnd: Long,
    ) {
        val isLoggedIn: Boolean get() = userToken.isNotEmpty()
        /** True when logged in and either periodEnd hasn't passed or is unknown (0). */
        val isActive: Boolean
            get() = isLoggedIn && (periodEnd == 0L || periodEnd > System.currentTimeMillis())
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Snapshot of the current account state (non-reactive helper). */
    val current: State get() = _state.value

    // ---------------------------------------------------------------------------
    //  Accessors
    // ---------------------------------------------------------------------------

    val userToken: String get() = current.userToken
    val tier: WekitProTier get() = current.tier
    val periodEnd: Long get() = current.periodEnd
    val isLoggedIn: Boolean get() = current.isLoggedIn
    val isActive: Boolean get() = current.isActive

    // ---------------------------------------------------------------------------
    //  Mutations
    // ---------------------------------------------------------------------------

    /**
     * Persists and broadcasts a successful redeem / account refresh result.
     * [token] is the `userToken` returned by the server.
     */
    @Synchronized
    fun save(token: String, tier: WekitProTier, periodEnd: Long) {
        WePrefs.putString(Preferences.WEKIT_PRO_TOKEN, token)
        WePrefs.putString(Preferences.WEKIT_PRO_TIER, tier.serverKey)
        WePrefs.putLong(Preferences.WEKIT_PRO_PERIOD_END, periodEnd)
        _state.value = State(token, tier, periodEnd)
    }

    /**
     * Resets the local account to the unauthenticated 普通 state.
     * Call when the server returns 401 (TOKEN_EXPIRED / TOKEN_REVOKED) or 429 (RATE_LIMITED).
     */
    @Synchronized
    fun clear() {
        WePrefs.remove(Preferences.WEKIT_PRO_TOKEN)
        WePrefs.remove(Preferences.WEKIT_PRO_TIER)
        WePrefs.remove(Preferences.WEKIT_PRO_PERIOD_END)
        _state.value = State("", WekitProTier.NONE, 0L)
    }

    /**
     * If [periodEnd] is in the past, silently downgrades the local state to [WekitProTier.NONE]
     * without touching the token. Used on startup when the network refresh fails and we cannot
     * confirm renewal — the subscription has definitively expired by time alone.
     */
    @Synchronized
    fun clearIfExpiredLocally() {
        val s = current
        if (s.periodEnd > 0L && s.periodEnd <= System.currentTimeMillis()) {
            clear()
        }
    }

    // ---------------------------------------------------------------------------
    //  ModelProviderManager integration
    // ---------------------------------------------------------------------------

    /**
     * Builds a synthetic [ModelProviderEntity] for the built-in WeKit Router. The entity is never
     * written to the Room database; [ModelProviderManager] and [dev.ujhhgtg.wekit.agent.data.WeAgentRepository]
     * intercept the known [ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID] and return this
     * instead, so the [apiKey] always reflects the live [userToken].
     */
    fun buildSyntheticProvider(): ModelProviderEntity = ModelProviderEntity(
        id = ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID,
        type = ModelProviderType.WEKIT_ROUTER,
        name = "WeKit Router",
        baseUrl = ModelProviderManager.WEKIT_ROUTER_BASE_URL,
        apiKey = userToken,
    )

    // ---------------------------------------------------------------------------
    //  Persistence helpers
    // ---------------------------------------------------------------------------

    private fun load(): State {
        val token = WePrefs.getStringOrDef(Preferences.WEKIT_PRO_TOKEN, "")
        val tier = WekitProTier.fromServerKey(WePrefs.getStringOrDef(Preferences.WEKIT_PRO_TIER, ""))
        val periodEnd = WePrefs.getLongOrDef(Preferences.WEKIT_PRO_PERIOD_END, 0L)
        return State(token, tier, periodEnd)
    }
}
