package dev.ujhhgtg.wekit.features.api.net

import dev.ujhhgtg.wekit.agent.data.WeAgentRepository
import dev.ujhhgtg.wekit.agent.data.entity.ModelEntity
import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderEntity
import dev.ujhhgtg.wekit.agent.data.entity.ModelProviderType
import dev.ujhhgtg.wekit.agent.model.ModelProviderManager
import dev.ujhhgtg.wekit.features.core.ApiFeature
import dev.ujhhgtg.wekit.features.core.Feature
import dev.ujhhgtg.wekit.utils.WeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Startup hook for WeKit Pro account management.
 *
 * On [onEnable] (fires once per WeChat main-process start):
 * 1. Seeds the built-in [ModelProviderType.WEKIT_ROUTER] provider entity and its three hardcoded
 *    model entries to the WeAgent Room database (idempotent upsert — safe to run every launch).
 * 2. Refreshes the stored [WekitProAccount] against the server:
 *    - Up to [MAX_REFRESH_RETRIES] attempts with exponential back-off.
 *    - Terminal errors (401 / TOKEN_EXPIRED / TOKEN_REVOKED / 429 / RATE_LIMITED) clear the
 *      account immediately on the first failure — no retries for those.
 *    - Transient network failures: after all retries, calls [WekitProAccount.clearIfExpiredLocally]
 *      (path A — keep last-known tier unless the local `periodEnd` has already passed).
 *
 * This feature has no Xposed hooks; it is KSP-registered via [@Feature] and runs in
 * [ApiFeature.startup] → [enable] → [onEnable], which is the main-process startup path.
 */
@Feature(name = "WeKit Pro 账号", categories = ["API"], description = "WeKit Pro 账号管理与启动刷新")
object WekitProFeature : ApiFeature() {

    private const val TAG = "WekitProFeature"

    private const val MAX_REFRESH_RETRIES = 3
    /** Base delay between retries in millis — doubles each attempt (100 ms, 200 ms, 400 ms). */
    private const val RETRY_BASE_DELAY_MS = 100L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ---------------------------------------------------------------------------
    //  Built-in model entries (hardcoded; tier gating is server-side only)
    // ---------------------------------------------------------------------------

    /**
     * The three WeKit Router models available to subscribers. The server enforces tier gating and
     * returns `403 MODEL_TIER_NOT_ALLOWED` (OpenAI-compatible format) for requests that exceed the
     * account's current tier — no client-side enforcement needed.
     *
     * `contextWindow` is intentionally null (server-dependent; usage % is hidden for these models).
     * `supportsVision` is false by default; flip to true when the router adds vision support.
     */
    internal val BUILTIN_MODELS = listOf(
        ModelEntity(
            id = "wk-basic-builtin",
            providerId = ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID,
            modelIdRemote = "wk-basic",
            displayName = "WeKit AI 基础",
            reasoningEffort = null,
            customJsonOverride = null,
            contextWindow = null,
            supportsVision = false,
        ),
        ModelEntity(
            id = "wk-standard-builtin",
            providerId = ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID,
            modelIdRemote = "wk-standard",
            displayName = "WeKit AI 标准",
            reasoningEffort = null,
            customJsonOverride = null,
            contextWindow = null,
            supportsVision = false,
        ),
        ModelEntity(
            id = "wk-flagship-builtin",
            providerId = ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID,
            modelIdRemote = "wk-flagship",
            displayName = "WeKit AI 旗舰",
            reasoningEffort = null,
            customJsonOverride = null,
            contextWindow = null,
            supportsVision = false,
        ),
    )

    // ---------------------------------------------------------------------------
    //  ApiFeature
    // ---------------------------------------------------------------------------

    override fun onEnable() {
        scope.launch {
            seedBuiltinProviderAndModels()
            if (WekitProAccount.isLoggedIn) {
                performStartupRefresh()
            }
        }
    }

    // ---------------------------------------------------------------------------
    //  Seeding
    // ---------------------------------------------------------------------------

    /**
     * Idempotently writes the built-in WeKit Router provider and its model rows to the DB.
     * Uses upsert semantics — safe to call on every launch.
     *
     * The provider's `apiKey` column is left empty; [WeAgentRepository.getDecryptedModelProvider]
     * intercepts the known [ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID] and injects the live
     * [WekitProAccount.userToken] instead, so the DB row never holds a real credential.
     */
    private suspend fun seedBuiltinProviderAndModels() {
        runCatching {
            // Upsert the provider stub (apiKey intentionally blank — injected at runtime).
            WeAgentRepository.upsertModelProvider(
                ModelProviderEntity(
                    id = ModelProviderManager.WEKIT_ROUTER_BUILTIN_ID,
                    type = ModelProviderType.WEKIT_ROUTER,
                    name = "WeKit Router",
                    baseUrl = ModelProviderManager.WEKIT_ROUTER_BASE_URL,
                    apiKey = "",
                )
            )
            // Upsert each model (preserves any user-modified fields on conflict because
            // Room's CONFLICT_REPLACE replaces the whole row — models are ours, so that's fine).
            BUILTIN_MODELS.forEach { WeAgentRepository.upsertModel(it) }
            WeLogger.d(TAG, "built-in WeKit Router provider + ${BUILTIN_MODELS.size} models seeded")
        }.onFailure { WeLogger.e(TAG, "failed to seed built-in provider", it) }
    }

    // ---------------------------------------------------------------------------
    //  Startup refresh
    // ---------------------------------------------------------------------------

    private suspend fun performStartupRefresh() {
        var lastNetworkError: Throwable? = null

        repeat(MAX_REFRESH_RETRIES) { attempt ->
            val token = WekitProAccount.userToken
            if (token.isEmpty()) return // logged out during a retry — abort

            when (val result = WekitProClient.refreshAccount(token)) {
                is WekitProClient.RefreshResult.Success -> {
                    WekitProAccount.save(token, result.tier, result.periodEnd)
                    WeLogger.d(TAG, "account refreshed: tier=${result.tier}, status=${result.status}")
                    return // success — stop retrying
                }
                is WekitProClient.RefreshResult.Error -> {
                    WeLogger.w(TAG, "refresh attempt ${attempt + 1} failed: code=${result.code}, terminal=${result.terminal}")
                    if (result.terminal) {
                        // 401 / 429 / explicit TOKEN_* codes — clear immediately, do not retry.
                        WekitProAccount.clear()
                        WeLogger.i(TAG, "account cleared due to terminal error: ${result.code}")
                        return
                    }
                    // Network / transient error — record and schedule a retry.
                    lastNetworkError = null // code-only failure, not a thrown exception
                    if (attempt < MAX_REFRESH_RETRIES - 1) {
                        delay(RETRY_BASE_DELAY_MS * (1 shl attempt)) // exponential back-off
                    }
                }
            }
        }

        // All retries exhausted without a terminal error — apply path A:
        // keep the cached tier unless the local periodEnd has definitively passed.
        WeLogger.i(TAG, "refresh failed after $MAX_REFRESH_RETRIES attempts; applying path-A fallback")
        WekitProAccount.clearIfExpiredLocally()
    }
}
