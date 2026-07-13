package dev.ujhhgtg.wekit.features.api.net

import dev.ujhhgtg.wekit.agent.model.LlmJson
import dev.ujhhgtg.wekit.agent.model.ModelProviderManager
import dev.ujhhgtg.wekit.utils.WeLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * HTTP client for the WeKit Pro management API.
 *
 * Only covers the two endpoints the module needs:
 *  - [redeem] — `POST /v1/admin/redeem` (no auth for new accounts; Bearer for renewals/upgrades)
 *  - [refreshAccount] — `GET /v1/admin/account` (requires Bearer [userToken])
 *
 * All responses are parsed against the SPEC's unified error format
 * `{ "error": { "code": "...", "message": "..." } }`. Terminal auth errors
 * (`TOKEN_EXPIRED`, `TOKEN_REVOKED`, `RATE_LIMITED`) are surfaced as [Error.Terminal] so
 * callers can immediately clear the local account state.
 *
 * The wxId is intentionally *not* sent with [redeem] — the SPEC records it for reconciliation
 * only and we cannot verify it on the client side anyway.
 */
object WekitProClient {

    private const val TAG = "WekitProClient"

    private val baseUrl = ModelProviderManager.WEKIT_PRO_MGMT_ROOT.trimEnd('/')

    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    // ---------------------------------------------------------------------------
    //  Sealed result types
    // ---------------------------------------------------------------------------

    sealed interface RedeemResult {
        data class Success(
            val userToken: String,
            val tier: WekitProTier,
            /** periodEnd epoch millis as returned by the server. */
            val periodEnd: Long,
            /** "created" | "renewed" | "upgraded" */
            val action: String,
        ) : RedeemResult

        data class Error(val code: String, val message: String, val terminal: Boolean) : RedeemResult
    }

    sealed interface RefreshResult {
        data class Success(
            val tier: WekitProTier,
            val periodEnd: Long,
            /** "active" | "expired" | "revoked" */
            val status: String,
        ) : RefreshResult

        data class Error(val code: String, val message: String, val terminal: Boolean) : RefreshResult
    }

    // ---------------------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------------------

    /**
     * Redeems an activation code.
     *
     * - If [existingToken] is non-empty, it is sent as `Authorization: Bearer` so the server
     *   treats the request as a renewal or upgrade of the existing account.
     * - If [existingToken] is empty, no auth header is sent and the server creates a new account.
     */
    suspend fun redeem(code: String, existingToken: String = ""): RedeemResult {
        return runCatching {
            val resp = http.post("$baseUrl/v1/admin/redeem") {
                contentType(ContentType.Application.Json)
                if (existingToken.isNotEmpty()) bearerAuth(existingToken)
                setBody("""{"code":"${code.trim()}"}""")
            }
            val body = resp.bodyAsText()
            WeLogger.d(TAG, "redeem HTTP ${resp.status.value}: $body")

            if (resp.status.isSuccess()) {
                parseRedeemSuccess(body)
            } else {
                parseError(body, resp.status.value).let { (errCode, errMsg) ->
                    RedeemResult.Error(errCode, errMsg, isTerminalCode(errCode, resp.status.value))
                }
            }
        }.getOrElse { e ->
            WeLogger.e(TAG, "redeem network error", e)
            RedeemResult.Error("NETWORK_ERROR", e.message ?: "network error", terminal = false)
        }
    }

    /**
     * Refreshes the cached account state from the server. Requires a stored [userToken].
     * Returns [RefreshResult.Error] with [RefreshResult.Error.terminal] = true for
     * `TOKEN_EXPIRED`, `TOKEN_REVOKED`, and `RATE_LIMITED` — caller should [WekitProAccount.clear].
     */
    suspend fun refreshAccount(userToken: String): RefreshResult {
        return runCatching {
            val resp = http.get("$baseUrl/v1/admin/account") {
                bearerAuth(userToken)
            }
            val body = resp.bodyAsText()
            WeLogger.d(TAG, "refreshAccount HTTP ${resp.status.value}: $body")

            if (resp.status.isSuccess()) {
                parseRefreshSuccess(body)
            } else {
                parseError(body, resp.status.value).let { (errCode, errMsg) ->
                    RefreshResult.Error(errCode, errMsg, isTerminalCode(errCode, resp.status.value))
                }
            }
        }.getOrElse { e ->
            WeLogger.e(TAG, "refreshAccount network error", e)
            RefreshResult.Error("NETWORK_ERROR", e.message ?: "network error", terminal = false)
        }
    }

    // ---------------------------------------------------------------------------
    //  Parsing helpers
    // ---------------------------------------------------------------------------

    private fun parseRedeemSuccess(body: String): RedeemResult {
        val root = runCatching { LlmJson.json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return RedeemResult.Error("PARSE_ERROR", "invalid response body", false)

        val token = root["userToken"]?.jsonPrimitive?.content
            ?: return RedeemResult.Error("PARSE_ERROR", "missing userToken", false)
        val tierKey = root["tier"]?.jsonPrimitive?.content
        val periodEnd = root["periodEnd"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val action = root["action"]?.jsonPrimitive?.content ?: "created"

        return RedeemResult.Success(
            userToken = token,
            tier = WekitProTier.fromServerKey(tierKey),
            periodEnd = periodEnd,
            action = action,
        )
    }

    private fun parseRefreshSuccess(body: String): RefreshResult {
        val root = runCatching { LlmJson.json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return RefreshResult.Error("PARSE_ERROR", "invalid response body", false)

        val tierKey = root["tier"]?.jsonPrimitive?.content
        val periodEnd = root["periodEnd"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val status = root["status"]?.jsonPrimitive?.content ?: "active"

        return RefreshResult.Success(
            tier = WekitProTier.fromServerKey(tierKey),
            periodEnd = periodEnd,
            status = status,
        )
    }

    /** Extracts (code, message) from the unified error body. */
    private fun parseError(body: String, httpStatus: Int): Pair<String, String> {
        val root = runCatching { LlmJson.json.parseToJsonElement(body).jsonObject }.getOrNull()
        val errObj = root?.get("error")?.jsonObject
        val code = errObj?.get("code")?.jsonPrimitive?.content
            ?: "HTTP_$httpStatus"
        val msg = errObj?.get("message")?.jsonPrimitive?.content
            ?: "HTTP error $httpStatus"
        return code to msg
    }

    /**
     * Returns true for error codes that should immediately invalidate the local account state.
     * Per the spec and user decision §1: TOKEN_EXPIRED, TOKEN_REVOKED, RATE_LIMITED (and the
     * generic 401 fallback) are all terminal — they indicate the token is no longer usable.
     */
    private fun isTerminalCode(code: String, httpStatus: Int): Boolean = when {
        code in setOf("TOKEN_EXPIRED", "TOKEN_REVOKED", "RATE_LIMITED") -> true
        httpStatus == 401 || httpStatus == 429 -> true // catch-all for non-standard responses
        else -> false
    }
}
